import { useEffect, useMemo, useState } from "react";
import { BookOpen, FilePlus2, Share2, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { createAdminDocumentId, replaceAdminDocument, saveAdminDocument } from "@/lib/admin-firestore";
import { uploadAdminMedia } from "@/lib/admin-storage";
import { clearPendingSharedDocument, loadPendingSharedDocument, sharedDocumentFile, type PendingSharedDocument } from "@/lib/shared-import";

type AnyDoc = { id: string; [key: string]: any };

function labelForKind(kind: PendingSharedDocument["kind"]) {
  return kind === "pdf" ? "PDF" : kind === "epub" ? "EPUB" : "Word (.docx)";
}

export function SharedIbrImportCard({ courses }: { courses: AnyDoc[] }) {
  const [pending, setPending] = useState<PendingSharedDocument | null>(null);
  const [courseId, setCourseId] = useState("");
  const [chapterId, setChapterId] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void loadPendingSharedDocument().then((value) => {
      setPending(value);
      if (value && courses.length) setCourseId((current) => current || courses[0].id);
    }).catch(() => undefined);
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

  const upload = async () => uploadAdminMedia(sharedDocumentFile(pending));

  const attachToLesson = async () => {
    if (!selectedCourse || !chapterId) return toast.error("Escolha o curso e a aula que receberão o arquivo.");
    setBusy(true);
    try {
      const result = await upload();
      const field = pending.kind === "pdf" ? "studyPdfUrl" : pending.kind === "epub" ? "studyEpubUrl" : "studyDocxUrl";
      const nextChapters = chapters.map((chapter: AnyDoc) => chapter.id === chapterId ? { ...chapter, [field]: result.url } : chapter);
      const { id, ...courseData } = selectedCourse;
      await replaceAdminDocument("ibr_courses", id, { ...courseData, chapters: nextChapters });
      await clear();
      toast.success(`${labelForKind(pending.kind)} anexado à aula e sincronizado.`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível anexar o arquivo.");
    } finally { setBusy(false); }
  };

  const addToLibrary = async () => {
    setBusy(true);
    try {
      const result = await upload();
      const title = pending.name.replace(/\.(pdf|docx|epub)$/i, "").replace(/[_-]+/g, " ").trim() || "Material IBR";
      await saveAdminDocument("conteudos_books", createAdminDocumentId("conteudos_books"), {
        title,
        author: "Instituto Bíblico Rhema",
        description: pending.sharedText || "Material recebido por compartilhamento.",
        bookUrl: result.url,
        mediaUrl: result.url,
        type: pending.kind === "docx" ? "word" : pending.kind,
        contentType: pending.kind,
        isApproved: true,
        approved: true,
        publishedAt: Date.now(),
      });
      await clear();
      toast.success(`${labelForKind(pending.kind)} publicado na biblioteca.`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Não foi possível publicar o arquivo.");
    } finally { setBusy(false); }
  };

  return <article className="admin-global-card" style={{ display: "grid", gap: 12, marginBottom: 18 }}>
    <div style={{ display: "flex", gap: 12, alignItems: "center" }}><Share2 size={24}/><div style={{ flex: 1 }}><strong>Arquivo recebido por compartilhamento</strong><small style={{ display: "block", marginTop: 3 }}>{pending.name} · {labelForKind(pending.kind)} · {(pending.size / 1024 / 1024).toFixed(1)} MB</small></div></div>
    <p style={{ margin: 0, opacity: .78 }}>Recebido pelo menu Compartilhar do aparelho. Escolha anexar a uma aula do IBR ou publicar na biblioteca.</p>
    {courses.length > 0 && <div className="admin-form-grid">
      <label className="admin-field"><span>Curso</span><select value={courseId} onChange={(event) => setCourseId(event.target.value)}>{courses.map((course) => <option key={course.id} value={course.id}>{course.title || course.id}</option>)}</select></label>
      <label className="admin-field"><span>Aula</span><select value={chapterId} onChange={(event) => setChapterId(event.target.value)} disabled={!chapters.length}>{chapters.length ? chapters.map((chapter: AnyDoc) => <option key={chapter.id} value={chapter.id}>{chapter.title || chapter.id}</option>) : <option value="">Sem aulas</option>}</select></label>
    </div>}
    <div style={{ display: "flex", gap: 9, flexWrap: "wrap" }}>
      <button className="admin-save" disabled={busy || !chapterId} onClick={() => void attachToLesson()}><FilePlus2 size={18}/>{busy ? "Enviando…" : "Anexar à aula"}</button>
      <button className="admin-secondary" disabled={busy} onClick={() => void addToLibrary()}><BookOpen size={18}/>Publicar na biblioteca</button>
      <button className="admin-secondary" disabled={busy} onClick={() => void clear()}><Trash2 size={18}/>Descartar</button>
    </div>
  </article>;
}
