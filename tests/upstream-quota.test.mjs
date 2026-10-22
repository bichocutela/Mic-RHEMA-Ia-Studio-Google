import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {stripTypeScriptTypes} from 'node:module';
import vm from 'node:vm';
for (const name of ['discipulado-release','youtube-live','pwa-scheduled-notifications']) {
 test(`${name}: quota stops probes and writes until cooldown, then recovers`, async () => {
  const source=readFileSync(`supabase/functions/${name}/index.ts`,'utf8');
  const helper=source.split('// BEGIN UPSTREAM QUOTA GUARD')[1].split('// END UPSTREAM QUOTA GUARD')[0];
  let now=1000,calls=0;
  const context=vm.createContext({URL,Request,Response,Error,Date:{now:()=>now},fetch:async()=>{calls++;return new Response('{}',{status:calls===1?429:200});}});
  vm.runInContext(stripTypeScriptTypes(helper),context);
  const fetcher=vm.runInContext('firestoreQuotaFetch',context);
  const classify=vm.runInContext('upstreamQuotaResponse',context);
  assert.equal((await fetcher('https://firestore.googleapis.com/v1/documents')).status,429);
  await assert.rejects(fetcher('https://firestore.googleapis.com/v1/documents',{method:'PATCH',body:'{}'}),/RESOURCE_EXHAUSTED/);
  assert.equal(calls,1,'no automatic replay or writes during quota exhaustion');
  const temporary=classify(new Error('Leitura falhou: 429'),{});
  assert.equal(temporary.status,503);assert.equal(temporary.headers.get('retry-after'),'900');
  assert.equal(classify(new Error('Bad credential'),{}),null,'unrelated faults are not disguised as quota');
  await fetcher('https://oauth2.googleapis.com/token');assert.equal(calls,2,'quota does not block identity endpoints');
  now+=900001;assert.equal((await fetcher('https://firestore.googleapis.com/v1/documents')).status,200);
  assert.equal(calls,3);
 });
}
