import {test} from 'node:test';
import assert from 'node:assert/strict';
import {ensureDiscipuladoTab} from '../pwa/client/src/lib/app-tabs.ts';
test('configuração antiga recebe Discipulado após IBR sem alterar origem',()=>{
 const source=[{id:'4',systemRoute:'ibr',order:4},{id:'5',systemRoute:'content',order:5}];
 const result=ensureDiscipuladoTab(source);
 assert.equal(result.find(t=>t.systemRoute==='discipulado').order,5);
 assert.equal(result.find(t=>t.id==='5').order,6);
 assert.equal(source[1].order,5);
 assert.deepEqual(ensureDiscipuladoTab(result),result);
});
test('configuração explícita oculta ou renomeada é preservada',()=>{
 const tabs=[{id:'custom',systemRoute:'discipulado',isVisible:false,title:'Estudos',order:9}];
 assert.deepEqual(ensureDiscipuladoTab(tabs),tabs);
});
test('todas as abas padrão do Android possuem rota no PWA',async()=>{
 const {readFileSync}=await import('node:fs');
 const tabs=JSON.parse(readFileSync(new URL('../pwa/client/src/data/android-tabs.json',import.meta.url),'utf8'));
 const shell=readFileSync(new URL('../pwa/client/src/components/PwaShell.tsx',import.meta.url),'utf8');
 const map=shell.slice(shell.indexOf('const tabRouteMap:'),shell.indexOf('const tabIdRouteMap:'));
 assert.equal(tabs.length,15);
 for(const tab of tabs)assert.match(map,new RegExp(`\\b${tab.systemRoute}:`),`${tab.title} sem rota`);
});
