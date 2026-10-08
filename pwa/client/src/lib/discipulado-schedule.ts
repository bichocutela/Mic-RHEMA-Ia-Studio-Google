export function scheduleParts(epoch: number) {
  if (!epoch) return { date: "", time: "12:00" };
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Fortaleza", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).formatToParts(epoch);
  const part = (name: string) => parts.find(p => p.type === name)?.value || "";
  return { date: `${part("year")}-${part("month")}-${part("day")}`, time: `${part("hour")}:${part("minute")}` };
}

export function publicationEpoch(date: string, time: string, now = Date.now()) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time)) throw new Error("Escolha a data e o horário da publicação.");
  const epoch = Date.parse(`${date}T${time}:00-03:00`);
  if (!Number.isFinite(epoch)) throw new Error("Data ou horário inválidos.");
  const normalized = scheduleParts(epoch);
  if (normalized.date !== date || normalized.time !== time) throw new Error("Data ou horário inválidos.");
  if (epoch <= now) throw new Error("Escolha uma data e horário futuros.");
  return epoch;
}

export function publicationLabel(epoch: number) {
  return new Intl.DateTimeFormat("pt-BR", {
    timeZone: "America/Fortaleza", dateStyle: "short", timeStyle: "short",
  }).format(epoch);
}
