import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { stripTypeScriptTypes } from 'node:module';
import { webcrypto } from 'node:crypto';

const source = readFileSync(new URL('../index.ts', import.meta.url), 'utf8').replace(/^import .*;\n/gm, '');
const now = Date.parse('2026-10-08T15:00:00Z');
const prefix = 'projects/mic-rhema/databases/(default)/documents/';
const value = v => typeof v === 'boolean' ? {booleanValue:v} : typeof v === 'number' ? {integerValue:String(v)} : {stringValue:v};
const doc = (collection, id, data) => ({name:prefix+collection+'/'+id, fields:Object.fromEntries(Object.entries(data).map(([k,v])=>[k,value(v)])), updateTime:'v1'});
function fixture({future=false, failNotification=false}={}) {
  const docs = new Map(); let version = 1, deliveries=0, commits=0, handler;
  const scheduled = doc('discipulado_schedules','study',{title:'Fundamentos da Fé',description:'Descrição completa',fileUrl:'https://drive.google.com/file/d/source/view',isPublished:false,scheduledPublishAt:now+(future?60000:0),releaseNotificationPending:false});
  docs.set(scheduled.name, scheduled);
  const reply = (body, status=200)=>new Response(JSON.stringify(body),{status});
  const fetch = async (input, init={}) => {
    const url = new URL(input);const body=init.body?JSON.parse(init.body):null;
    if(url.pathname.endsWith(':runQuery')) {
      const q=body.structuredQuery;const name=q.from[0].collectionId;
      if(name==='discipulado_schedules') {
        assert.equal(q.where.compositeFilter.filters[1].fieldFilter.op,'LESS_THAN_OR_EQUAL');
        assert.equal(Number(q.where.compositeFilter.filters[1].fieldFilter.value.integerValue),now);
      }
      return reply([...docs.values()].filter(d=>d.name.includes('/'+name+'/')).filter(d=>name==='discipulado_schedules'?Number(d.fields.scheduledPublishAt.integerValue)>0&&Number(d.fields.scheduledPublishAt.integerValue)<=now:d.fields.releaseNotificationPending?.booleanValue===true).map(document=>({document:structuredClone(document)})));
    }
    if(url.pathname.endsWith(':commit')) {
      const [update, deletion]=body.writes;
      if(docs.get(deletion.delete)?.updateTime!==deletion.currentDocument.updateTime) return reply({},409);
      const old=docs.get(update.update.name);
      if(update.currentDocument.exists===false&&old) return reply({},409);
      docs.set(update.update.name,{...update.update,updateTime:'v'+(++version)});
      docs.delete(deletion.delete); commits++;return reply({});
    }
    if(url.pathname.endsWith('/notify-fcm')) {
      const live=docs.get(prefix+'discipulado_pdfs/study');
      assert.equal(live?.fields.isPublished.booleanValue,true,'notification must follow publication');
      assert.equal(docs.has(scheduled.name),false,'queue must be consumed first');
      assert.equal(body.title,'Fundamentos da Fé'); assert.match(body.body,/novo estudo.*Discipulado/);
      assert.equal(body.data.destination,'discipulado');
      if(failNotification) {failNotification=false;return reply({error:'temporarily unavailable'},502);}
      deliveries++;return reply({ok:true});
    }
    const name=url.pathname.slice('/v1/'.length);const existing=docs.get(name);
    if(init.method==='PATCH') {
      if(url.searchParams.get('currentDocument.updateTime')!==existing?.updateTime)return reply({},409);
      const updated={...existing,fields:{...existing.fields,...body.fields},updateTime:'v'+(++version)};
      docs.set(name,updated);return reply(updated);
    }
    return existing?reply(structuredClone(existing)):reply({},404);
  };
  class Clock extends Date {static now(){return now;}constructor(...args){super(...(args.length?args:[now]));}}
  const context=vm.createContext({fetch,Date:Clock,URL,URLSearchParams,Response,Request,TextEncoder,Uint8Array,crypto:webcrypto,AbortSignal,console:{error(){}},studies:[],Deno:{env:{get:()=> 'https://example.supabase.co'},serve:fn=>{handler=fn;}}});
  vm.runInContext(stripTypeScriptTypes(source)+'\nglobalThis.api={release};',context);
  return {run:()=>context.api.release('test-token'),handler:()=>handler,docs,counts:()=>({deliveries,commits})};
}

test('does not publish or notify before the selected time',async()=>{const f=fixture({future:true});const r=await f.run();assert.equal(r.published.length,0);assert.deepEqual(f.counts(),{deliveries:0,commits:0});});
test('publishes full metadata, then notifies once and consumes the schedule',async()=>{const f=fixture();await f.run();await f.run();const d=f.docs.get(prefix+'discipulado_pdfs/study');assert.equal(d.fields.description.stringValue,'Descrição completa');assert.equal(d.fields.releaseNotificationPending.booleanValue,false);assert.deepEqual(f.counts(),{deliveries:1,commits:1});});
test('retries a failed notification without republishing the study',async()=>{const f=fixture({failNotification:true});const first=await f.run();assert.equal(first.failed.length,1);assert.equal(f.docs.get(prefix+'discipulado_pdfs/study').fields.releaseNotificationPending.booleanValue,true);await f.run();assert.deepEqual(f.counts(),{deliveries:1,commits:1});});
test('concurrent schedulers use atomic preconditions to avoid duplicates',async()=>{const f=fixture();await Promise.all([f.run(),f.run()]);assert.deepEqual(f.counts(),{deliveries:1,commits:1});});
test('rejects an unauthenticated publication request',async()=>{const f=fixture();const r=await f.handler()(new Request('https://example/discipulado-release',{method:'POST',body:JSON.stringify({action:'release'})}));assert.equal(r.status,401);assert.deepEqual(f.counts(),{deliveries:0,commits:0});});
