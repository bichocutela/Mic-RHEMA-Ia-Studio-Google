import { useState } from "react";
import { BookOpen, Download, FileText } from "lucide-react";
import { resolvePdfEmbedUrl, resolvePortableAssetUrl, safeFilename } from "@/lib/parity-utils";
import { RichDocumentReader } from "./RichDocumentReader";

type LessonMaterials = {
  title?: string;
  studyPdfUrl?: string;
  studyDocxUrl?: string;
  studyEpubUrl?: string;
};

type ReaderState = { url: string; type: "docx" | "epub"; title: string } | null;

function download(url: string, filename: string) {
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
}

export function IbrStudyMaterials({ lesson }: { lesson: LessonMaterials }) {
  const [reader, setReader] = useState<ReaderState>(null);
  const pdf = String(lesson.studyPdfUrl || "").trim();
  const docx = String(lesson.studyDocxUrl || "").trim();
  const epub = String(lesson.studyEpubUrl || "").trim();
  if (!pdf && !docx && !epub) return null;

  const title = lesson.title || "Material IBR";
  const pdfUrl = resolvePortableAssetUrl(pdf);
  const pdfEmbed = resolvePdfEmbedUrl(pdf);
  const docxUrl = resolvePortableAssetUrl(docx);
  const epubUrl = resolvePortableAssetUrl(epub);
  const fileBase = safeFilename(title || "material-ibr");

  return <>
    <section style={{ marginTop: 22, display: "grid", gap: 10 }}>
      <p style={{ fontWeight: 800, margin: 0 }}>Conteúdos para estudo</p>
      {pdf && <>
        <article className="android-module-card" style={{ display: "flex", alignItems: "center", gap: 12, padding: 16, flexWrap: "wrap" }}>
          <FileText size={28}/><div style={{ flex: "1 1 220px" }}><strong style={{ display: "block" }}>Material complementar em PDF</strong><small>Leia dentro da PWA ou baixe para estudar depois.</small></div>
          <button className="back-link" onClick={() => download(pdfUrl, `${fileBase}.pdf`)}><Download size={18}/> Baixar</button>
        </article>
        {pdfEmbed && <iframe className="parity-document" style={{ width: "100%", minHeight: "62dvh", border: 0, borderRadius: 16, background: "#fff" }} src={pdfEmbed} title="Material complementar IBR"/>}
      </>}
      {docx && <article className="android-module-card" style={{ display: "flex", alignItems: "center", gap: 12, padding: 16, flexWrap: "wrap" }}>
        <FileText size={28}/><div style={{ flex: "1 1 220px" }}><strong style={{ display: "block" }}>Material complementar em Word (.docx)</strong><small>Abra no leitor interno do MIC Rhema ou baixe o arquivo original.</small></div>
        <button className="back-link" onClick={() => setReader({ url: docxUrl, type: "docx", title })}><BookOpen size={18}/> Ler aqui</button>
        <button className="back-link" onClick={() => download(docxUrl, `${fileBase}.docx`)}><Download size={18}/> Baixar</button>
      </article>}
      {epub && <article className="android-module-card" style={{ display: "flex", alignItems: "center", gap: 12, padding: 16, flexWrap: "wrap" }}>
        <BookOpen size={28}/><div style={{ flex: "1 1 220px" }}><strong style={{ display: "block" }}>Livro complementar EPUB</strong><small>Leia os capítulos dentro da PWA ou baixe o EPUB.</small></div>
        <button className="back-link" onClick={() => setReader({ url: epubUrl, type: "epub", title })}><BookOpen size={18}/> Ler aqui</button>
        <button className="back-link" onClick={() => download(epubUrl, `${fileBase}.epub`)}><Download size={18}/> Baixar</button>
      </article>}
    </section>
    {reader && <RichDocumentReader sourceUrl={reader.url} type={reader.type} title={reader.title} onClose={() => setReader(null)}/>} 
  </>;
}
