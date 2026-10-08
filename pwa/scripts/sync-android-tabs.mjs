import fs from 'node:fs';
const source=fs.readFileSync(new URL('../../app/src/main/java/com/aistudio/micrhema/Data.kt',import.meta.url),'utf8');
const block=source.slice(source.indexOf('val defaultTabs = listOf('),source.indexOf('appTabsState.addAll(defaultTabs)'));
const routes={Home:'home',Services:'services',Devotionals:'devocionais',Ibr:'ibr',Discipulado:'discipulado',Content:'content',Prayer:'prayer',Team:'equipe',Members:'members',About:'about',Settings:'settings',Donations:'donations',Admin:'admin'};
const tabs=Array.from(block.matchAll(/AppTab\("([^"]+)", "([^"]+)", "([^"]+)", (true|false), (true|false), (true|false), (\d+), TabContentType\.SYSTEM, (?:Screen\.(\w+)\.route|"([^"]+)")\)/g),m=>({id:m[1],title:m[2],iconName:m[3],isPrivate:m[4]==='true',isVisible:m[5]==='true',showInBottomBar:m[6]==='true',order:Number(m[7]),type:'SYSTEM',systemRoute:m[9]||routes[m[8]]}));
if(!tabs.length||tabs.some(tab=>!tab.systemRoute))throw new Error('Não foi possível sincronizar as abas do Android.');
fs.writeFileSync(new URL('../client/src/data/android-tabs.json',import.meta.url),JSON.stringify(tabs,null,2)+'\n');
console.log(`Sincronizadas ${tabs.length} abas do Android.`);
