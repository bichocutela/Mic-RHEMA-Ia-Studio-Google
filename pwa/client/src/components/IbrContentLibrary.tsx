import { useEffect, useMemo, useState } from "react";
import { BookOpen, ChevronLeft, Download, ExternalLink, FileText } from "lucide-react";
import { listenToCollection } from "@/lib/firebase";
import { resolveDisplayImageUrl, resolvePdfEmbedUrl, resolvePortableAssetUrl, safeFilename } from "@/lib/parity-utils";
import { RichDocumentReader } from "./RichDocumentReader";

type VipBook = {
  id: string;
  title?: string;
  author?: string;
  coverUrl?: string;
  bookUrl?: string;
  type?: string;
  approved?: boolean;
  isApproved?: boolean;
};

type BookKind = "pdf" | "docx" | "epub";

function bookKind(item: VipBook): BookKind {
  const fingerprint = `${item.type || ""} ${item.bookUrl || ""}`.toLowerCase();
  if (fingerprint.includes("epub")) return "epub";
  if (fingerprint.includes("word") || fingerprint.includes("docx") || fingerprint.includes("wordprocessingml")) return "docx";
  return "pdf";
}

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

export function IbrContentLibrary() {
  const [books, setBooks] = useState<VipBook[]>([]);
  const [selected, setSelected] = useState<VipBook | null>(null);
  useEffect(() => listenToCollection<VipBook>("vip_books", setBooks, () => setBooks([])), []);
  const visible = useMemo(() => books.filter((item) => item.approved !== false && item.isApproved !== false && String(item.bookUrl || "").trim()), [books]);
  if (!visible.length) return null;

  if (selected) {
    const kind = bookKind(selected);
    const sourceUrl = resolvePortableAssetUrl(selected.bookUrl || "");
    const title = selected.title || "Conteúdo IBR";
    if (kind === "docx" || kind === "epub") return <RichDocumentReader sourceUrl={sourceUrl} type={kind} title={title} onClose={() => setSelected(null)}/>;
    const pdfEmbed = resolvePdfEmbedUrl(selected.bookUrl || "");
    const filename = `${safeFilename(title)}.pdf`;
    return <section className="parity-page">
      <button className="back-link" onClick={() => setSelected(null)}><ChevronLeft size={18}/> Voltar ao Conteúdo IBR</button>
      <div className="parity-title"><div><p>CONTEÚDO IBR</p><h1>{title}</h1><span>{selected.author || "Instituto Bíblico Rhema"}</span></div><FileText size={30}/></div>
      {pdfEmbed ? <iframe className="parity-document" style={{ width: "100%", minHeight: "72dvh", border: 0, borderRadius: 16, background: "#fff" }} src={pdfEmbed} title={title}/> : <p className="parity-status">Não foi possível preparar o leitor deste PDF.</p>}
      <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 12 }}>
        {pdfEmbed && <a className="back-link" href={pdfEmbed} target="_blank" rel="noreferrer"><ExternalLink size={17}/> Tela cheia</a>}
        <button className="back-link" onClick={() => download(sourceUrl, filename)}><Download size={17}/> Baixar PDF</button>
      </div>
    </section>;
  }

  return <section style={{ marginTop: 20 }}>
    <div className="parity-title" style={{ marginBottom: 12 }}><div><p>BIBLIOTECA DO INSTITUTO</p><h2 style={{ margin: 0 }}>Conteúdo IBR</h2><span>PDF, EPUB e Word para leitura dentro do MIC Rhema.</span></div><BookOpen size={28}/></div>
    <div className="android-list-cards">{visible.map((book) => {
      const kind = bookKind(book);
      const cover = resolveDisplayImageUrl(book.coverUrl);
      return <button key={book.id} onClick={() => setSelected(book)}>
        {cover ? <img src={cover} alt="" loading="lazy" style={{ width: 46, height: 58, objectFit: "cover", borderRadius: 8 }}/> : <span><BookOpen size={20}/></span>}
        <div><strong>{book.title || "Material IBR"}</strong><small>{book.author || "Instituto Bíblico Rhema"} · {kind === "docx" ? "Word" : kind.toUpperCase()}</small></div>
        <BookOpen size={19}/>
      </button>;
    })}</div>
  </section>;
}
