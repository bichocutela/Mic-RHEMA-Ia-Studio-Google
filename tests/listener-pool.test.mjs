import {test} from 'node:test';
import assert from 'node:assert/strict';
import {ListenerPool} from '../pwa/client/src/lib/listener-pool.ts';

test('navigation and simultaneous screens reuse one listener and release it after grace', t => {
  t.mock.timers.enable({apis:['setTimeout']});
  const pool = new ListenerPool(); let starts = 0, stops = 0, emit;
  const start = data => {starts++;emit=data;return () => stops++;};
  const first = [], second = [];
  const closeA = pool.subscribe('public:guest:news',start,v=>first.push(v));
  emit(['news']);
  const closeB = pool.subscribe('public:guest:news',start,v=>second.push(v));
  assert.equal(starts,1); assert.deepEqual(second,[['news']]);
  closeA(); closeB(); t.mock.timers.tick(20_000);
  const closeC = pool.subscribe('public:guest:news',start,()=>{});
  assert.equal(starts,1); assert.equal(stops,0);
  closeC(); closeC(); t.mock.timers.tick(30_000); assert.equal(stops,1);
  pool.subscribe('public:guest:news',start,()=>{});
  assert.equal(starts,2);
});

test('member and admin subscriptions never share results', () => {
  const pool = new ListenerPool(); let starts = 0;
  const start = data => { starts++;data(starts);return () => {}; };
  const values=[];
  pool.subscribe('member:user1:news',start,v=>values.push(v));
  pool.subscribe('admin:user1:news',start,v=>values.push(v));
  pool.subscribe('member:user2:news',start,v=>values.push(v));
  assert.deepEqual(values,[1,2,3]);
});

test('failed listener discards its data and a later subscription starts fresh', t => {
  t.mock.timers.enable({apis:['setTimeout']});
  const pool = new ListenerPool(); let fail, starts=0;
  const start = (data,error) => {starts++;fail=error;data(starts);return () => {};};
  const values=[], errors=[];
  const close = pool.subscribe('public:guest:news',start,v=>values.push(v),e=>errors.push(e.message));
  fail(Error('unavailable'));
  pool.subscribe('public:guest:news',start,v=>values.push(v));
  close();t.mock.timers.tick(30_000);
  pool.subscribe('public:guest:news',start,v=>values.push(v));
  assert.equal(starts,2);assert.deepEqual(values,[1,2,2]);assert.deepEqual(errors,['unavailable']);
});
