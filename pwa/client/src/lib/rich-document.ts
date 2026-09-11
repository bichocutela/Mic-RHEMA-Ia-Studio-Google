export type RichDocumentType = "docx" | "epub";

export type ParsedRichDocument = {
  title?: string;
  html: string;
  objectUrls: string[];
};

const MAX_ARCHIVE_BYTES = 50 * 1024 * 1024;
const MAX_EXPANDED_BYTES = 100 * 1024 * 1024;
const MAX_ENTRIES = 3000;
const textDecoder = new TextDecoder("utf-8");

type ZipEntry = { name: string; bytes: Uint8Array };

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character] || character);
}

function normalizeArchivePath(base: string, relative: string) {
  const clean = relative.split(/[?#]/)[0].replace(/\\/g, "/");
  if (!clean) return "";
  const parts = clean.startsWith("/") ? [] : base.split("/").filter(Boolean);
  for (const part of clean.split("/")) {
    if (!part || part === ".") continue;
    if (part === "..") parts.pop();
    else parts.push(part);
  }
  return parts.join("/");
}

function extensionMime(path: string) {
  const extension = path.split(".").pop()?.toLowerCase() || "";
  return ({ png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", gif: "image/gif", webp: "image/webp", svg: "image/svg+xml" } as Record<string, string>)[extension] || "application/octet-stream";
}

function bufferFromBytes(bytes: Uint8Array) {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
}

async function inflateRaw(bytes: Uint8Array) {
  if (!("DecompressionStream" in globalThis)) throw new Error("Este navegador não possui o recurso necessário para abrir este documento internamente.");
  const stream = new Blob([bufferFromBytes(bytes)]).stream().pipeThrough(new DecompressionStream("deflate-raw" as any));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

function findEndOfCentralDirectory(view: DataView) {
  const minimum = Math.max(0, view.byteLength - 65_557);
  for (let offset = view.byteLength - 22; offset >= minimum; offset--) {
    if (view.getUint32(offset, true) === 0x06054b50) return offset;
  }
  return -1;
}

async function unzip(arrayBuffer: ArrayBuffer) {
  if (arrayBuffer.byteLength <= 0 || arrayBuffer.byteLength > MAX_ARCHIVE_BYTES) throw new Error("O documento está vazio ou ultrapassa 50 MB.");
  const view = new DataView(arrayBuffer);
  const eocd = findEndOfCentralDirectory(view);
  if (eocd < 0) throw new Error("O arquivo não parece ser um DOCX/EPUB válido.");
  const totalEntries = view.getUint16(eocd + 10, true);
  const centralOffset = view.getUint32(eocd + 16, true);
  if (totalEntries > MAX_ENTRIES) throw new Error("O documento possui arquivos internos demais para leitura segura.");

  const result = new Map<string, ZipEntry>();
  let cursor = centralOffset;
  let expanded = 0;
  for (let index = 0; index < totalEntries; index++) {
    if (cursor + 46 > view.byteLength || view.getUint32(cursor, true) !== 0x02014b50) throw new Error("Estrutura ZIP inválida.");
    const flags = view.getUint16(cursor + 8, true);
    const method = view.getUint16(cursor + 10, true);
    const compressedSize = view.getUint32(cursor + 20, true);
    const uncompressedSize = view.getUint32(cursor + 24, true);
    const nameLength = view.getUint16(cursor + 28, true);
    const extraLength = view.getUint16(cursor + 30, true);
    const commentLength = view.getUint16(cursor + 32, true);
    const localOffset = view.getUint32(cursor + 42, true);
    const nameStart = cursor + 46;
    const name = textDecoder.decode(new Uint8Array(arrayBuffer, nameStart, nameLength)).replace(/\\/g, "/");
    cursor = nameStart + nameLength + extraLength + commentLength;
    if (!name || name.endsWith("/")) continue;
    if ((flags & 0x1) !== 0) throw new Error("Documentos ZIP criptografados não são suportados.");
    expanded += uncompressedSize;
    if (expanded > MAX_EXPANDED_BYTES) throw new Error("O documento expandido é grande demais para leitura segura.");
    if (localOffset + 30 > view.byteLength || view.getUint32(localOffset, true) !== 0x04034b50) throw new Error("Entrada ZIP inválida.");
    const localNameLength = view.getUint16(localOffset + 26, true);
    const localExtraLength = view.getUint16(localOffset + 28, true);
    const dataStart = localOffset + 30 + localNameLength + localExtraLength;
    if (dataStart + compressedSize > view.byteLength) throw new Error("Documento ZIP truncado.");
    const compressed = new Uint8Array(arrayBuffer, dataStart, compressedSize);
    let bytes: Uint8Array;
    if (method === 0) bytes = new Uint8Array(bufferFromBytes(compressed));
    else if (method === 8) bytes = await inflateRaw(compressed);
    else throw new Error(`Compressão ZIP não suportada (${method}).`);
    result.set(name, { name, bytes });
  }
  return result;
}

function xmlFrom(entry: ZipEntry | undefined, label: string) {
  if (!entry) throw new Error(`${label} não encontrado no documento.`);
  const xml = new DOMParser().parseFromString(textDecoder.decode(entry.bytes), "application/xml");
  if (xml.querySelector("parsererror")) throw new Error(`${label} está corrompido.`);
  return xml;
}

function descendantsByLocalName(element: Element, localName: string) {
  return Array.from(element.getElementsByTagName("*")).filter((node) => node.localName === localName);
}

function directChild(element: Element, localName: string) {
  return Array.from(element.children).find((node) => node.localName === localName) || null;
}

function attributeByLocalName(element: Element | null, localName: string) {
  if (!element) return "";
  return Array.from(element.attributes).find((attribute) => attribute.localName === localName)?.value || "";
}

function makeObjectUrl(entries: Map<string, ZipEntry>, path: string, objectUrls: string[]) {
  const entry = entries.get(path);
  if (!entry) return "";
  const url = URL.createObjectURL(new Blob([bufferFromBytes(entry.bytes)], { type: extensionMime(path) }));
  objectUrls.push(url);
  return url;
}

function parseDocx(entries: Map<string, ZipEntry>): ParsedRichDocument {
  const documentXml = xmlFrom(entries.get("word/document.xml"), "Conteúdo principal do Word");
  const relationships = new Map<string, string>();
  const relEntry = entries.get("word/_rels/document.xml.rels");
  if (relEntry) {
    const relXml = xmlFrom(relEntry, "Relações do Word");
    for (const relation of Array.from(relXml.getElementsByTagName("Relationship"))) {
      const id = relation.getAttribute("Id") || "";
      const target = relation.getAttribute("Target") || "";
      if (id && target && !/^https?:/i.test(target)) relationships.set(id, normalizeArchivePath("word", target));
    }
  }

  const objectUrls: string[] = [];
  const imageCache = new Map<string, string>();
  const imageFor = (relationshipId: string) => {
    const target = relationships.get(relationshipId) || "";
    if (!target) return "";
    if (!imageCache.has(target)) imageCache.set(target, makeObjectUrl(entries, target, objectUrls));
    return imageCache.get(target) || "";
  };

  const renderRun = (run: Element) => {
    const props = directChild(run, "rPr");
    let html = "";
    for (const child of Array.from(run.children)) {
      if (child.localName === "t") html += escapeHtml(child.textContent || "");
      else if (child.localName === "tab") html += "&emsp;";
      else if (child.localName === "br" || child.localName === "cr") html += "<br/>";
      else if (child.localName === "drawing" || child.localName === "pict") {
        for (const blip of descendantsByLocalName(child, "blip")) {
          const relationshipId = attributeByLocalName(blip, "embed");
          const url = imageFor(relationshipId);
          if (url) html += `<img src="${url}" alt="Imagem do documento" loading="lazy"/>`;
        }
      }
    }
    if (!html) return "";
    if (props && descendantsByLocalName(props, "b").length) html = `<strong>${html}</strong>`;
    if (props && descendantsByLocalName(props, "i").length) html = `<em>${html}</em>`;
    if (props && descendantsByLocalName(props, "u").length) html = `<u>${html}</u>`;
    return html;
  };

  const renderParagraph = (paragraph: Element) => {
    const pPr = directChild(paragraph, "pPr");
    const styleValue = attributeByLocalName(pPr ? directChild(pPr, "pStyle") : null, "val").toLowerCase();
    const align = attributeByLocalName(pPr ? directChild(pPr, "jc") : null, "val").toLowerCase();
    const numbered = Boolean(pPr && descendantsByLocalName(pPr, "numPr").length);
    let inner = "";
    for (const child of Array.from(paragraph.children)) {
      if (child.localName === "r") inner += renderRun(child);
      else if (child.localName === "hyperlink") for (const run of Array.from(child.children).filter((node) => node.localName === "r")) inner += renderRun(run);
    }
    if (numbered && inner) inner = `• ${inner}`;
    const style = align && ["left", "right", "center", "both"].includes(align) ? ` style="text-align:${align === "both" ? "justify" : align}"` : "";
    const tag = styleValue.includes("heading1") || styleValue.includes("titulo1") ? "h1" : styleValue.includes("heading2") || styleValue.includes("titulo2") ? "h2" : styleValue.includes("heading3") || styleValue.includes("titulo3") ? "h3" : "p";
    return `<${tag}${style}>${inner || "&nbsp;"}</${tag}>`;
  };

  const renderTable = (table: Element) => {
    const rows = Array.from(table.children).filter((node) => node.localName === "tr").map((row) => {
      const cells = Array.from(row.children).filter((node) => node.localName === "tc").map((cell) => {
        const content = Array.from(cell.children).filter((node) => node.localName === "p").map(renderParagraph).join("");
        return `<td>${content}</td>`;
      }).join("");
      return `<tr>${cells}</tr>`;
    }).join("");
    return `<div class="rich-table-wrap"><table><tbody>${rows}</tbody></table></div>`;
  };

  const body = descendantsByLocalName(documentXml.documentElement, "body")[0];
  if (!body) throw new Error("O Word não possui conteúdo legível.");
  const html = Array.from(body.children).map((child) => child.localName === "p" ? renderParagraph(child) : child.localName === "tbl" ? renderTable(child) : "").join("");
  return { html: html || "<p>Documento sem texto visível.</p>", objectUrls };
}

function sanitizeEpubBody(document: Document, chapterPath: string, entries: Map<string, ZipEntry>, objectUrls: string[]) {
  const body = document.body;
  if (!body) return "";
  body.querySelectorAll("script,iframe,object,embed,form,link,meta,base").forEach((node) => node.remove());
  body.querySelectorAll("*").forEach((element) => {
    for (const attribute of Array.from(element.attributes)) {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim();
      if (name.startsWith("on") || name === "style") element.removeAttribute(attribute.name);
      else if ((name === "href" || name === "src") && /^javascript:/i.test(value)) element.removeAttribute(attribute.name);
    }
    if (element.tagName.toLowerCase() === "img") {
      const source = element.getAttribute("src") || "";
      if (source && !/^(data:|blob:|https?:)/i.test(source)) {
        const resolved = normalizeArchivePath(chapterPath.split("/").slice(0, -1).join("/"), source);
        const url = makeObjectUrl(entries, resolved, objectUrls);
        if (url) element.setAttribute("src", url); else element.removeAttribute("src");
      }
    }
    if (element.tagName.toLowerCase() === "a") {
      const href = element.getAttribute("href") || "";
      if (/^https?:/i.test(href)) { element.setAttribute("target", "_blank"); element.setAttribute("rel", "noopener noreferrer"); }
      else if (href && !href.startsWith("#")) element.removeAttribute("href");
    }
  });
  return body.innerHTML;
}

function parseEpub(entries: Map<string, ZipEntry>): ParsedRichDocument {
  const container = xmlFrom(entries.get("META-INF/container.xml"), "Índice do EPUB");
  const rootfile = descendantsByLocalName(container.documentElement, "rootfile")[0];
  const opfPath = rootfile?.getAttribute("full-path") || "";
  if (!opfPath) throw new Error("O EPUB não informa seu arquivo principal.");
  const opf = xmlFrom(entries.get(opfPath), "Pacote principal do EPUB");
  const base = opfPath.split("/").slice(0, -1).join("/");
  const manifest = new Map<string, { href: string; mediaType: string }>();
  for (const item of descendantsByLocalName(opf.documentElement, "item")) {
    const id = item.getAttribute("id") || "";
    const href = item.getAttribute("href") || "";
    if (id && href) manifest.set(id, { href, mediaType: item.getAttribute("media-type") || "" });
  }
  const spineIds = descendantsByLocalName(opf.documentElement, "itemref").map((item) => item.getAttribute("idref") || "").filter(Boolean);
  const objectUrls: string[] = [];
  const sections: string[] = [];
  for (const id of spineIds) {
    const item = manifest.get(id);
    if (!item) continue;
    const chapterPath = normalizeArchivePath(base, item.href);
    const entry = entries.get(chapterPath);
    if (!entry) continue;
    const source = textDecoder.decode(entry.bytes);
    const chapter = new DOMParser().parseFromString(source, "text/html");
    const chapterHtml = sanitizeEpubBody(chapter, chapterPath, entries, objectUrls);
    if (chapterHtml.trim()) sections.push(`<section class="rich-epub-chapter">${chapterHtml}</section>`);
  }
  const titleNode = descendantsByLocalName(opf.documentElement, "title")[0];
  const title = titleNode?.textContent?.trim() || undefined;
  if (!sections.length) throw new Error("Nenhum capítulo legível foi encontrado no EPUB.");
  return { title, html: sections.join("<hr class=\"rich-chapter-separator\"/>"), objectUrls };
}

export async function loadRichDocument(sourceUrl: string, type: RichDocumentType): Promise<ParsedRichDocument> {
  const response = await fetch(sourceUrl, { cache: "no-store", credentials: "omit" });
  if (!response.ok) throw new Error(`Não foi possível baixar o documento (HTTP ${response.status}).`);
  const buffer = await response.arrayBuffer();
  const entries = await unzip(buffer);
  return type === "docx" ? parseDocx(entries) : parseEpub(entries);
}
