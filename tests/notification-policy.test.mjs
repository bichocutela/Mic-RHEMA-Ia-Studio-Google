import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import vm from 'node:vm';
import { webcrypto } from 'node:crypto';

const policy = readFileSync(new URL('../supabase/functions/notify-fcm/notification-policy.ts', import.meta.url), 'utf8').replace(/export /g, '');
const scheduled = readFileSync(new URL('../supabase/functions/pwa-scheduled-notifications/index.ts', import.meta.url), 'utf8').replace(/^import .*;\n/gm, '');
function fixture(hour = 15) {
  const records = new Map(); const sends = []; let handler;
  class Clock extends Date {
    constructor(...args) { super(...(args.length ? args : [`2026-10-11T${String(hour + 3).padStart(2, '0')}:00:00Z`])); }
    static now() { return new Clock().getTime(); }
  }
  const response = (value, status = 200) => new Response(JSON.stringify(value), { status });
  const fetch = async (input, init = {}) => {
    const url = new URL(input); const body = init.body ? JSON.parse(init.body) : null;
    if (url.hostname === 'fcm.googleapis.com') { sends.push(body); return response({ name: 'accepted' }); }
    if (url.pathname.endsWith('/notify-fcm')) { sends.push(body); return response({ ok: true }); }
    if (url.pathname.endsWith('/notification_outbox')) return response({ documents: [...records.values()].filter(d => d.name.includes('/notification_outbox/')) });
    const name = url.pathname.slice(4);
    if (init.method === 'PATCH') {
      if (url.searchParams.get('currentDocument.exists') === 'false' && records.has(name)) return response({}, 409);
      const d = { name, updateTime: 'v1', fields: { ...records.get(name)?.fields, ...body.fields } }; records.set(name, d); return response(d);
    }
    if (init.method === 'DELETE') { records.delete(name); return response({}); }
    if (!init.method) return response({ name, fields: { isPublished: { booleanValue: true } } });
    throw new Error(`Unexpected request: ${url}`);
  };
  const c = vm.createContext({ Date: Clock, Intl, URL, URLSearchParams, Request, Response, TextEncoder, Uint8Array, crypto: webcrypto, fetch, console, notificationPresentation: () => ({ icon: 'icon', badge: 'badge', link: 'https://example' }), Deno: { env: { get: () => 'https://example.supabase.co' }, serve: fn => { handler = fn; } } });
  vm.runInContext(stripTypeScriptTypes(policy + '\n' + scheduled) + '\nglobalThis.api = { notificationSlot, fortalezaClock, claimDelivery, drainOutbox, send };', c);
  return { api: c.api, records, sends, handler: () => handler };
}
test('church clock, midnight and all content slots are explicit', () => {
  const f = fixture(0);
  assert.equal(f.api.fortalezaClock().hour, 0);
  for (const [data, expected] of [[{ collection: 'devocionais' }, 8], [{ category: 'service' }, 10], [{ category: 'news' }, 12], [{ collection: 'discipulado_pdfs' }, 14], [{ category: 'media' }, 15], [{ category: 'app_update' }, 18], [{ category: 'courses' }, 19]]) assert.equal(f.api.notificationSlot(data), expected);
});
test('concurrent schedulers claim the same delivery only once', async () => {
  const f = fixture(); const results = await Promise.all([f.api.claimDelivery('mic-rhema', 'token', 'same-event'), f.api.claimDelivery('mic-rhema', 'token', 'same-event')]);
  assert.equal(results.filter(Boolean).length, 1);
});
test('two pending media documents generate one digest at 15h', async () => {
  const f = fixture();
  for (const id of ['a', 'b']) {
    const name = `projects/mic-rhema/databases/(default)/documents/notification_outbox/${id}`;
    f.records.set(name, { name, fields: { state: { stringValue: 'pending' }, slot: { integerValue: '15' }, payload: { stringValue: JSON.stringify({ topic: 'all_users', title: id, body: 'new', data: { collection: 'conteudos_videos', documentId: id, category: 'sermons' } }) } } });
  }
  await Promise.all([f.api.drainOutbox('mic-rhema', 'token', new Date('2026-10-11T18:00:00Z')), f.api.drainOutbox('mic-rhema', 'token', new Date('2026-10-11T18:00:00Z'))]);
  assert.equal(f.sends.length, 1); assert.equal(f.sends[0].title, '2 novidades no MIC Rhema'); assert.equal(f.sends[0].data.documentId, undefined);
});
test('content queue does not deliver at midnight', async () => {
  const f = fixture(0);
  await f.api.drainOutbox('mic-rhema', 'token', new Date('2026-10-11T03:00:00Z'));
  assert.equal(f.sends.length, 0);
});
test('daily push is data-only with one-hour expiry and can validate without sending', async () => {
  const f = fixture(8);
  await f.api.send('mic-rhema', 'token', 'all_users', { title: 'Devocional', body: 'Hoje', category: 'devotional', tag: 'today' }, true);
  assert.equal(f.sends[0].validate_only, true); assert.equal(f.sends[0].message.notification, undefined);
  assert.equal(f.sends[0].message.android.ttl, '3600s'); assert.equal(f.sends[0].message.webpush.headers.TTL, '3600');
});
test('scheduler rejects anonymous calls', async () => {
  const f = fixture(); const result = await f.handler()(new Request('https://example/scheduler', { method: 'POST', body: '{}' }));
  assert.equal(result.status, 401); assert.equal(f.sends.length, 0);
});
