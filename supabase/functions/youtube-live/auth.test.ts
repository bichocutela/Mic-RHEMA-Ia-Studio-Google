let handler: (request: Request) => Promise<Response>;
const serve = Deno.serve;
Object.defineProperty(Deno, "serve", { configurable: true, value: (value: typeof handler) => { handler = value; return {}; } });
await import("./index.ts");
Object.defineProperty(Deno, "serve", { configurable: true, value: serve });
globalThis.fetch = (() => { throw new Error("Unauthenticated refresh attempted upstream access"); }) as typeof fetch;
for (const method of ["GET", "POST"]) Deno.test(`YouTube forced refresh requires authentication: ${method}`, async () => {
  const response = await handler(new Request("https://example.test?force=1", { method }));
  if (response.status !== 401) throw new Error(`Expected 401, got ${response.status}`);
  await response.text();
});
