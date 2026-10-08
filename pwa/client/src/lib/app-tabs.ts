type Tab = {id:string;systemRoute?:string|null;order?:number};
/** Mesma migração de ensureDiscipuladoTab em Data.kt. Preserva configurações explícitas. */
export function ensureDiscipuladoTab<T extends Tab>(tabs:T[]):Array<T | (Tab & {title:string;iconName:string;isPrivate:boolean;isVisible:boolean;showInBottomBar:boolean;type:string})>{
  if(tabs.some(tab=>tab.id==="discipulado_tab"||tab.systemRoute==="discipulado"))return tabs.slice();
  const order=Number(tabs.find(tab=>tab.systemRoute==="ibr")?.order??4)+1;
  return [...tabs.map(tab=>Number(tab.order||0)>=order?{...tab,order:Number(tab.order||0)+1}:tab),
    {id:"discipulado_tab",title:"Discipulado",iconName:"MenuBook",isPrivate:false,isVisible:true,showInBottomBar:false,order,type:"SYSTEM",systemRoute:"discipulado"}];
}
