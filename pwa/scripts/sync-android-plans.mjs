import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const sourcePath = resolve(scriptDir, '../../app/src/main/java/com/aistudio/micrhema/PlansData.kt');
const outputPath = resolve(scriptDir, '../client/src/data/android-plans.json');

function readString(source, start) {
  const opening = source.indexOf('"', start);
  if (opening < 0) throw new Error('String Kotlin não encontrada.');
  let value = '';
  for (let index = opening + 1; index < source.length; index += 1) {
    const char = source[index];
    if (char === '\\') {
      const next = source[index + 1];
      const escapes = { n: '\n', r: '\r', t: '\t', '"': '"', '\\': '\\' };
      value += escapes[next] ?? next;
      index += 1;
    } else if (char === '"') {
      return { value, next: index + 1 };
    } else {
      value += char;
    }
  }
  throw new Error('String Kotlin sem fechamento.');
}

function callBody(source, start) {
  const opening = source.indexOf('(', start);
  if (opening < 0) throw new Error('Abertura de chamada Kotlin não encontrada.');
  let depth = 0;
  for (let index = opening; index < source.length; index += 1) {
    if (source[index] === '"') {
      index = readString(source, index).next - 1;
      continue;
    }
    if (source[index] === '(') depth += 1;
    if (source[index] === ')') {
      depth -= 1;
      if (depth === 0) return { body: source.slice(opening + 1, index), next: index + 1 };
    }
  }
  throw new Error('Chamada Kotlin sem fechamento.');
}

function extractThemes(categoryBody) {
  const themes = [];
  let cursor = 0;
  while (true) {
    const marker = categoryBody.indexOf('PlanTheme(', cursor);
    if (marker < 0) break;
    const { body, next } = callBody(categoryBody, marker);
    const title = readString(body, 0);
    const content = readString(body, title.next);
    const versesCall = callBody(body, body.indexOf('listOf(', content.next));
    const versesBody = versesCall.body;
    const verses = [];
    let verseCursor = 0;
    while (verseCursor < versesBody.length) {
      const quote = versesBody.indexOf('"', verseCursor);
      if (quote < 0) break;
      const verse = readString(versesBody, quote);
      verses.push(verse.value);
      verseCursor = verse.next;
    }
    const image = readString(body, versesCall.next);
    themes.push({ title: title.value, content: content.value, verses, imageUrl: image.value });
    cursor = next;
  }
  return themes;
}

const source = await readFile(sourcePath, 'utf8');
const categories = [];
let cursor = source.indexOf('object PlansData');
if (cursor < 0) throw new Error('Catálogo PlansData não encontrado no Android.');
while (true) {
  const marker = source.indexOf('PlanCategory(', cursor);
  if (marker < 0) break;
  const { body, next } = callBody(source, marker);
  const name = readString(body, 0).value;
  const color = body.match(/Color\(0xFF([0-9A-Fa-f]{6})\)/)?.[1] ?? '997D19';
  const themes = extractThemes(body);
  if (themes.length) categories.push({ name, color: `#${color}`, themes });
  cursor = next;
}

if (!categories.length) throw new Error('Nenhuma categoria de Planos foi encontrada no Android.');
await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, `${JSON.stringify(categories, null, 2)}\n`);
console.log(`Catálogo Android sincronizado: ${categories.length} categorias, ${categories.reduce((total, category) => total + category.themes.length, 0)} temas.`);
