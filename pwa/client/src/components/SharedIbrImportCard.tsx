import { useEffect, useMemo, useState } from "react";
import { BookOpen, FilePlus2, Share2, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { createAdminDocumentId, replaceAdminDocument, saveAdminDocument } from "@/lib/admin-firestore";
import { uploadAdminMedia } from "@/lib/admin-storage";
import {
  clearPendingSharedDocument, detectPendingSharedKind, loadPendingSharedDocument, sharedDocumentFile,
  type PendingSharedDocument, type SharedDocumentKind,
} from "@/lib/shared-import";

type AnyDoc = { id: string; [key: string]: any };
type KnownKind = Exclude<SharedDocumentKind, "unknown">;

function labelForKind(kind: SharedDocumentKind | "") {
  return kind === "pdf" ? "PDF" : kind === "epub" ? "EPUB" : kind === "docx" ? "Word (.docx)" : "Tipo não identificado";
}

export function SharedIbrImportCard({ courses }: { courses: AnyDoc[] }) {
  const [pending, setPending] = useState<PendingSharedDocument | null>(null);
  const [kind, setKind] = useState<KnownKind | "">("");
  const [courseId, setCourseId] = useState("");
  const [chapterId, setChapterId] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    void loadPendingSharedDocument().then(async (value) => {
      if (!active) return;
      setPending(value);
      if (value) {
        const detected = await detectPendingSharedKind(value).catch(() => "unknown" as const);
        if (active && detected !== "unknown") setKind(detected);
      }
      if (value && courses.length) setCourseId((current) => current || courses[0].id);
    }).catch(() => undefined);
    return () => { active = false; };
  }, [courses.length]);

  const selectedCourse = useMemo(() => courses.find((course) => course.id === courseId) || null, [courses, courseId]);
  const chapters = Array.isArray(selectedCourse?.chapters) ? selectedCourse!.chapters : [];
  useEffect(() => {
    if (!selectedCourse) { setChapterId(""); return; }
    if (!chapters.some((chapter: AnyDoc) => chapter.id === chapterId)) setChapterId(chapters[0]?.id || "");
  }, [selectedCourse?.id, chapters.length]);

  if (!pending) return null;

  const clear = async () => {
    await clearPendingSharedDocument();
    setPending(null);
    const params = new URLSearchParams(window.location.search);
    params.delete("shared");
    window.history.replaceState({}, "", `${window.location.pathname}${params.size ? `?${params.toString()}` : ""}`);
  };

  const upload = async () => {
    if (!kind) throw new Error("Escolha se o arquivo é PDF, EPUB ou Word (.docx).");
    const file = await sharedDocumentFile(pending, kind);
    return uploadAdminMedia(file);
  };

  const attachToLesson = async () => {
    if (!selectedCourse || !chapterId) return toast.error("Escolha o curso e a aula que receberão o arquivo.");
    if (!kind) return toast.error("Confirme o tipo do arquivo compartilhado.");
    if (kind === "epub") return toast.info("EPUB é publicado no Conteúdo IBR, igual ao Android.");
    setBusy(true);
    try {
      const result = await upload();
      const field = kind === "pdf" ? "studyPdfUrl" : "studyDocxUrl";
      const nextChapters = chapters.map((chapter: AnyDoc) => chapter.id === chapterId ? { ...chapter, [field]: result.url } : chapter);
      const { id, ...courseData } = selectedCourse;
      await replaceAdminDocument("ibr_courses", id, { ...courseData, chapters: nextChapters });
      await clear();
      toast.success(`${labelForKind(kind)} anexado à aula e sincronizado.`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível anexar o arquivo.");
    } finally { setBusy(false); }
  };

  const publishToIbr = async () => {
    if (!kind) return toast.error("Confirme o tipo do arquivo compartilhado.");
    setBusy(true);
    try {
      const result = await upload();
      const title = pending.name.replace(/\.(pdf|docx|epub)$/i, "").replace(/[_-]+/g, " ").trim() || "Material IBR";
      await saveAdminDocument("vip_books", createAdminDocumentId("vip_books"), {
        title,
        author: "Compartilhado pela PWA",
        coverUrl: "",
        contentText: "",
        bookUrl: result.url,
        type: kind === "docx" ? "word" : kind,
        isApproved: true,
        approved: true,
      });
      await clear();
      toast.success(`${labelForKind(kind)} adicionado ao Conteúdo IBR.`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível publicar o arquivo.");
    } finally { setBusy(false); }
  };

  return <article className="admin-global-card" style={{ display: "grid", gap: 12, marginBottom: 18 }}>
    <div style={{ display: "flex", gap: 12, alignItems: "center" }}><Share2 size={24}/><div style={{ flex: 1 }}><strong>Arquivo recebido por compartilhamento</strong><small style={{ display: "block", marginTop: 3 }}>{pending.name} · {labelForKind(kind || pending.kind)}{pending.size > 0 ? ` · ${(pending.size / 1024 / 1024).toFixed(1)} MB` : " · link compartilhado"}</small></div></div>
    <p style={{ margin: 0, opacity: .78 }}>Recebido pelo menu Compartilhar do aparelho. PDF, EPUB e Word são identificados e podem ser publicados no mesmo Conteúdo IBR usado pelo Android.</p>
    {!kind && <label className="admin-field"><span>Confirme o tipo do arquivo</span><select value={kind} onChange={(event) => setKind(event.target.value as KnownKind | "")}><option value="">Selecionar…</option><option value="pdf">PDF</option><option value="epub">EPUB</option><option value="docx">Word (.docx)</option></select></label>}
    {kind && kind !== "epub" && courses.length > 0 && <div className="admin-form-grid">
      <label className="admin-field"><span>Curso (opcional)</span><select value={courseId} onChange={(event) => setCourseId(event.target.value)}>{courses.map((course) => <option key={course.id} value={course.id}>{course.title || course.id}</option>)}</select></label>
      <label className="admin-field"><span>Aula</span><select value={chapterId} onChange={(event) => setChapterId(event.target.value)} disabled={!chapters.length}>{chapters.length ? chapters.map((chapter: AnyDoc) => <option key={chapter.id} value={chapter.id}>{chapter.title || chapter.id}</option>) : <option value="">Sem aulas</option>}</select></label>
    </div>}
    <div style={{ display: "flex", gap: 9, flexWrap: "wrap" }}>
      <button className="admin-save" disabled={busy || !kind} onClick={() => void publishToIbr()}><BookOpen size={18}/>{busy ? "Enviando…" : "Adicionar ao Conteúdo IBR"}</button>
      {kind && kind !== "epub" && <button className="admin-secondary" disabled={busy || !chapterId} onClick={() => void attachToLesson()}><FilePlus2 size={18}/>Anexar à aula</button>}
      <button className="admin-secondary" disabled={busy} onClick={() => void clear()}><Trash2 size={18}/>Descartar</button>
    </div>
  </article>;
}
