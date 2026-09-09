import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import vm from 'node:vm';
import test from 'node:test';

const original = {
  id: 'badge_leitor_palavra', name: 'Distintivo Leitor da Palavra',
  description: 'Original', cost: 750, image_url: '', active: true,
  stock: 12, limit_per_member: 1, available_from: '2026-01-01',
};
const custom = { id: 'custom', kind: 'distintivo', name: 'Outro' };
const source = readFileSync(new URL('../supabase/functions/xp-shop-admin-simple/index.ts', import.meta.url), 'utf8')
  .replace(/^import .*;\n/gm, '');
const compiled = stripTypeScriptTypes(source);

function backend() {
  let handler;
  let row = { ...original };
  const writes = [];
  const client = {
    from(table) {
      let ids; let updated;
      const query = {
        select() { return this; },
        eq(key, value) { if (key === 'id') ids = [value]; return this; },
        in(key, value) { if (key === 'id') ids = value; return this; },
        order() { return this; },
        update(value) { updated = value; return this; },
        single() {
          assert.equal(table, 'xp_shop_items');
          assert.deepEqual(ids, [original.id]);
          writes.push(JSON.parse(JSON.stringify(updated)));
          row = { ...row, ...updated };
          return Promise.resolve({ data: row });
        },
        then(resolve, reject) {
          return Promise.resolve({ data: table === 'profile_cosmetics' ? [custom] : (ids?.includes(row.id) ? [row] : []) }).then(resolve, reject);
        },
      };
      return query;
    },
  };
  vm.runInNewContext(compiled, {
    createClient: () => client, Deno: { env: { get: () => 'test' }, serve: h => handler = h },
    Response, console,
  });
  return {
    writes,
    get row() { return row; },
    async call(input) {
      const response = await handler(new Request('https://test.invalid', { method: 'POST', body: JSON.stringify(input) }));
      return { status: response.status, body: await response.json() };
    },
  };
}

test('legacy catalogs unchanged; new client sees original IDs beside custom items', async () => {
  const b = backend();
  const legacy = await b.call({ action: 'cosmetics_catalog', kind: 'distintivo' });
  assert.deepEqual(legacy.body.items.map(i => i.id), ['custom']);
  const modern = await b.call({ action: 'cosmetics_catalog', kind: 'distintivo', includeBuiltins: true });
  assert.deepEqual(modern.body.items.map(i => i.id), [original.id, 'custom']);
  assert.equal(modern.body.items[0].xp_cost, 750);
  assert.equal(modern.body.items[0].kind, 'distintivo');
  assert.equal(b.writes.length, 0);
});

test('editing the original preserves stock, scheduling, ID and existing purchases', async () => {
  const b = backend();
  const result = await b.call({ action: 'admin_upsert_cosmetic', id: original.id, kind: 'distintivo',
    name: 'Leitor', description: 'Novo nome', xpCost: 800, imageRef: '', active: false });
  assert.equal(result.status, 200);
  assert.equal(result.body.item.id, original.id);
  assert.equal(result.body.item.active, false);
  assert.equal(b.row.stock, 12);
  assert.equal(b.row.limit_per_member, 1);
  assert.equal(b.row.available_from, original.available_from);
  assert.deepEqual(Object.keys(b.writes[0]).sort(), ['active', 'cost', 'description', 'image_url', 'name', 'updated_at']);
});

test('rejects wrong kind, invalid price and unsupported image before any write', async () => {
  for (const patch of [{ kind: 'moldura' }, { xpCost: 0 }, { xpCost: 1.5 }, { imageRef: 'https://invalid/image.png' }]) {
    const b = backend();
    const result = await b.call({ action: 'admin_upsert_cosmetic', id: original.id, kind: 'distintivo',
      name: 'Leitor', xpCost: 750, imageRef: '', ...patch });
    assert.equal(result.status, 400);
    assert.equal(b.writes.length, 0);
  }
});

test('supports uploaded replacement and restoring the native star', async () => {
  const b = backend();
  for (const imageRef of ['micrhema-xp://badge/example', '']) {
    const result = await b.call({ action: 'admin_upsert_cosmetic', id: original.id, kind: 'distintivo',
      name: 'Leitor', xpCost: 750, imageRef });
    assert.equal(result.status, 200);
    assert.equal(result.body.item.image_ref, imageRef);
  }
});
