import { useEffect, useMemo, useRef, useState } from "react";
import { ChevronLeft, Download, ExternalLink, FileText, Minus, Plus } from "lucide-react";
import { loadRichDocument, type RichDocumentType } from "@/lib/rich-document";
import "./RichDocumentReader.css";

function downloadFile(url: string, filename: string) {
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
}

export function RichDocumentReader({ sourceUrl, type, title, onClose }: { sourceUrl: string; type: RichDocumentType; title: string; onClose?: () => void }) {
  const [html, setHtml] = useState("");
  const [documentTitle, setDocumentTitle] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [fontScale, setFontScale] = useState(1);
  const [progress, setProgress] = useState(0);
  const readerRef = useRef<HTMLDivElement | null>(null);
  const positionKey = useMemo(() => `mic-rhema-rich-reader:${type}:${sourceUrl}`, [sourceUrl, type]);

  useEffect(() => {
    let active = true;
    let objectUrls: string[] = [];
    setLoading(true);
    setError("");
    setHtml("");
    void loadRichDocument(sourceUrl, type).then((result) => {
      objectUrls = result.objectUrls;
      if (!active) return;
      setHtml(result.html);
      setDocumentTitle(result.title || "");
      setLoading(false);
      window.setTimeout(() => {
        const reader = readerRef.current;
        if (!reader) return;
        const saved = Number(localStorage.getItem(positionKey) || 0);
        if (Number.isFinite(saved) && saved > 0) reader.scrollTop = saved;
      }, 0);
    }).catch((reason) => {
      if (!active) return;
      setError(reason instanceof Error ? reason.message : "Não foi possível abrir este documento.");
      setLoading(false);
    });
    return () => {
      active = false;
      objectUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [sourceUrl, type, positionKey]);

  const extension = type === "docx" ? "docx" : "epub";
  const filename = `${(title || documentTitle || "material-ibr").replace(/[^a-z0-9áàâãéêíóôõúç_-]+/gi, "-").replace(/^-+|-+$/g, "") || "material-ibr"}.${extension}`;
  const body = <div className="rich-document-reader">
    <header className="rich-reader-toolbar">
      <div className="rich-reader-title">
        {onClose && <button type="button" onClick={onClose} aria-label="Voltar"><ChevronLeft size={21}/></button>}
        <FileText size={22}/>
        <span><strong>{documentTitle || title}</strong><small>{type === "docx" ? "Documento Word" : "Livro EPUB"}</small></span>
      </div>
      <div className="rich-reader-actions">
        <button type="button" onClick={() => setFontScale((value) => Math.max(.78, Number((value - .1).toFixed(2))))} aria-label="Diminuir texto"><Minus size={18}/></button>
        <button type="button" onClick={() => setFontScale((value) => Math.min(1.55, Number((value + .1).toFixed(2))))} aria-label="Aumentar texto"><Plus size={18}/></button>
        <button type="button" onClick={() => downloadFile(sourceUrl, filename)} aria-label="Baixar"><Download size={18}/></button>
        <a href={sourceUrl} target="_blank" rel="noopener noreferrer" aria-label="Abrir externamente"><ExternalLink size={18}/></a>
      </div>
    </header>
    <div className="rich-reader-progress"><i style={{ width: `${progress}%` }}/></div>
    <div
      ref={readerRef}
      className="rich-reader-scroll"
      style={{ fontSize: `${fontScale}rem` }}
      onScroll={(event) => {
        const element = event.currentTarget;
        localStorage.setItem(positionKey, String(Math.max(0, Math.round(element.scrollTop))));
        const range = Math.max(1, element.scrollHeight - element.clientHeight);
        setProgress(Math.max(0, Math.min(100, Math.round(element.scrollTop / range * 100))));
      }}
    >
      {loading ? <div className="rich-reader-state"><span className="rich-reader-spinner"/><strong>Preparando documento…</strong><small>O arquivo é processado somente para leitura.</small></div> : error ? <div className="rich-reader-state is-error"><strong>Não foi possível abrir dentro da PWA.</strong><p>{error}</p><a href={sourceUrl} target="_blank" rel="noopener noreferrer"><ExternalLink size={17}/> Abrir em outro aplicativo</a></div> : <article className="rich-reader-content" dangerouslySetInnerHTML={{ __html: html }}/>} 
    </div>
  </div>;

  return onClose ? <div className="rich-reader-overlay" role="dialog" aria-modal="true" aria-label={title}>{body}</div> : body;
}
