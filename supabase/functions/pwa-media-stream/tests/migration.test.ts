let handler: (request: Request) => Promise<Response>;
const originalServe = Deno.serve;
Deno.env.set("SUPABASE_URL", "https://sinzscjscvgzkvkimcbd.supabase.co");
Object.defineProperty(Deno, "serve", { configurable: true, value: (value: typeof handler) => { handler = value; return {}; } });
await import("../index.ts");
Object.defineProperty(Deno, "serve", { configurable: true, value: originalServe });
const fetched: string[] = [];
globalThis.fetch = ((input: RequestInfo | URL) => {
  fetched.push(String(input));
  return Promise.resolve(new Response(new Uint8Array([1, 2, 3]), { headers: { "content-type": "video/mp4", "content-length": "3" } }));
}) as typeof fetch;

for (const source of [
  "https://cwphbkdtorfpgmnlafqb.supabase.co/storage/v1/object/public/media-assets/rhema-admin/video.mp4",
  "https://sinzscjscvgzkvkimcbd.supabase.co/storage/v1/object/public/media-assets/rhema-admin/video.mp4",
  "media-assets/rhema-admin/video.mp4",
]) Deno.test(`media keeps its object path in destination: ${source}`, async () => {
  const response = await handler(new Request(`https://example.test?url=${encodeURIComponent(source)}`));
  if (response.status !== 200) throw new Error(await response.text());
  await response.arrayBuffer();
  if (fetched.at(-1) !== "https://sinzscjscvgzkvkimcbd.supabase.co/storage/v1/object/public/media-assets/rhema-admin/video.mp4") throw new Error("Wrong project or path");
});

Deno.test("migration does not permit unrelated upstream hosts", async () => {
  const count = fetched.length;
  const response = await handler(new Request("https://example.test?url=https%3A%2F%2Funtrusted.example%2Fmedia.mp4"));
  if (response.ok || fetched.length !== count) throw new Error("Unexpected external fetch");
  await response.text();
});
