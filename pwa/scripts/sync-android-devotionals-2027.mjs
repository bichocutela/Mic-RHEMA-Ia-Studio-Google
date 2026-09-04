import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const sourcePath = path.resolve(root, "../app/src/main/java/com/aistudio/micrhema/DevotionalCalendar2027.kt");
const outputPath = path.resolve(root, "client/src/data/android-devotionals-2027.json");
const source = fs.readFileSync(sourcePath, "utf8");

function unescapeKotlin(value) {
  return value.replace(/\\n/g, "\n").replace(/\\\"/g, '"').replace(/\\\\/g, "\\");
}

function quotedValues(fragment) {
  const values = [];
  const pattern = /"((?:\\.|[^"\\])*)"/g;
  let match;
  while ((match = pattern.exec(fragment))) values.push(unescapeKotlin(match[1]));
  return values;
}

const seeds = [...source.matchAll(/^\s*Seed\((.+)\),?\s*$/gm)]
  .map((match) => quotedValues(match[1]))
  .filter((parts) => parts.length === 4)
  .map(([title, verseReference, verse, reflection]) => ({ title, verseReference, verse, reflection }));
if (seeds.length < 20) throw new Error(`Falha ao extrair seeds do Android: ${seeds.length}`);

const focusBlock = source.match(/private val monthlyFocus = listOf\(([\s\S]*?)\n\s*\)/)?.[1] || "";
const monthlyFocus = quotedValues(focusBlock);
if (monthlyFocus.length !== 12) throw new Error(`Focos mensais inválidos: ${monthlyFocus.length}`);

const practiceBlock = source.match(/private val weekdayPractice = mapOf\(([\s\S]*?)\n\s*\)/)?.[1] || "";
const weekdayMap = {};
for (const match of practiceBlock.matchAll(/DayOfWeek\.(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)\s+to\s+"((?:\\.|[^"\\])*)"/g)) {
  weekdayMap[match[1]] = unescapeKotlin(match[2]);
}
const weekdayNames = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
if (Object.keys(weekdayMap).length !== 7) throw new Error("Práticas semanais incompletas.");

const start = new Date(2027, 0, 1);
const end = new Date(2027, 11, 31);
const items = [];
for (let date = new Date(start), index = 0; date <= end; date.setDate(date.getDate() + 1), index++) {
  const current = new Date(date);
  const month = current.getMonth() + 1;
  const seed = seeds[(index * 13 + month) % seeds.length];
  const yyyyMmDd = `${current.getFullYear()}-${String(month).padStart(2, "0")}-${String(current.getDate()).padStart(2, "0")}`;
  const practice = weekdayMap[weekdayNames[current.getDay()]] || "";
  items.push({
    id: `auto-2027-${yyyyMmDd}`,
    title: seed.title,
    date: yyyyMmDd,
    verse: seed.verse,
    verseReference: seed.verseReference,
    content: `${seed.reflection}\n\nNeste mês, mantenha atenção especial a ${monthlyFocus[month - 1]}. ${practice}\n\nOração: Senhor, firma meu coração na tua Palavra, dá-me sabedoria para este dia e ajuda-me a viver de modo que minhas escolhas revelem Cristo. Amém.`,
    type: "devocional_auto",
    isApproved: true,
    approved: true,
    timestamp: current.getTime(),
  });
}
if (items.length !== 365) throw new Error(`Calendário 2027 incompleto: ${items.length}`);
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, `${JSON.stringify(items, null, 2)}\n`);
console.log(`Sincronizados ${items.length} devocionais de 2027 a partir do Android.`);
