'use strict';
const state={page:'items',catalog:{items:[],packs:[],errors:[]},recipes:[],guis:[],materials:[],templates:[],docs:[],doc:'introduccion',search:'',view:'cards',pack:'all',kind:'all',modal:false,mode:'',entry:null};
const $=s=>document.querySelector(s);
const h=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const ref=s=>encodeURIComponent(s);
const get=id=>$('#'+id)?.value?.trim()||'';
const names={items:'Creaciones',packs:'Packs',templates:'Plantillas',recipes:'Crafting Studio',guis:'GUI Studio',docs:'Documentación'};
function toast(s){const el=$('#toast');el.textContent=s;el.classList.add('show');clearTimeout(toast.id);toast.id=setTimeout(()=>el.classList.remove('show'),4200)}
async function api(url,method='GET',body=null,headers={}){
 const res=await fetch(url,{method,cache:'no-store',headers:Object.assign({'X-VSRender-Editor':'1'},body!=null&&!(body instanceof Uint8Array)?{'Content-Type':'application/json'}:{},headers),body:body==null?undefined:body instanceof Uint8Array?body:JSON.stringify(body)});
 const data=res.headers.get('content-type')?.includes('json')?await res.json():await res.text();
 if(!res.ok)throw Error(data.error||String(data));
 return data;
}
function heading(title,desc,buttons=''){
 return '<div class="head"><div><small class="minor">ADMINISTRADOR DE CONTENIDO</small><h1>'+h(title)+'</h1><p>'+h(desc)+'</p></div><div class="actions">'+buttons+'</div></div>';
}
function button(action,label,cls=''){return '<button class="btn '+cls+'" data-action="'+action+'">'+h(label)+'</button>'}
function count(name,n){return '<div class="stat"><small>'+name+'</small><b>'+n+'</b></div>'}
function packOptions(selected){return state.catalog.packs.map(p=>'<option value="'+h(p.name)+'" '+(p.name===selected?'selected':'')+'>'+h(p.name)+'</option>').join('')}
function slot(item,large=false) {
 const material=(item.material||'paper').replace('minecraft:','');
 const letters=material.split('_').slice(0,2).map(x=>x[0]?.toUpperCase()).join('');
 return '<span class="item-slot '+(large?'large':'')+'" title="Material: '+h(material)+'"><img loading="lazy" src="/api/thumb?ref='+ref(item.ref)+'" alt=""><span class="item-fallback"><b>'+h(letters||'?')+'</b><small>'+h(material.replace(/_/g,' '))+'</small></span></span>';
}
function renderItems(){
 const items=state.catalog.items,filtered=items.filter(x=>(state.pack==='all'||x.pack===state.pack)
  &&(state.kind==='all'||x.type===state.kind)
  &&(x.id+' '+x.name+' '+x.material).toLowerCase().includes(state.search.toLowerCase()));
 const options='<select id="pack-filter"><option value="all">Todos los packs</option>'+packOptions(state.pack)+'</select>';
 const kinds='<select id="kind-filter">'+[['all','Todos los tipos'],['items','Ítems'],['blocks','Bloques'],['furniture','Muebles']].map(x=>'<option value="'+x[0]+'" '+(state.kind===x[0]?'selected':'')+'>'+x[1]+'</option>').join('')+'</select>';
 const buttons='<div class="view-buttons"><button data-view="cards" class="btn small '+(state.view==='cards'?'active':'')+'">▦ Tarjetas</button><button data-view="list" class="btn small '+(state.view==='list'?'active':'')+'">☷ Lista</button></div>';
 const listing=state.view==='cards'
   ?'<div class="item-gallery">'+filtered.map(x=>'<article class="item-card">'+slot(x,true)+'<div class="item-card-info"><span class="pill">'+h(x.type)+'</span><b>'+h(x.name)+'</b><small>'+h(x.id)+'</small><small>Material: '+h(x.material)+'</small></div><div class="actions"><button class="btn small" data-open="'+h(x.ref)+'">Editar</button><button class="btn small" data-export="'+h(x.ref)+'">↓ Exportar</button></div></article>').join('')+'</div>'
   :'<div style="overflow-x:auto"><table><thead><tr><th>Creación</th><th>Tipo</th><th>Pack</th><th>Material</th><th></th></tr></thead><tbody>'+filtered.map(x=>'<tr><td><div class="item-line">'+slot(x)+'<span class="entry">'+h(x.name)+'<small>'+h(x.id)+'</small></span></div></td><td>'+h(x.type)+'</td><td>'+h(x.pack)+'</td><td class="mono">'+h(x.material)+'</td><td><button class="btn small" data-open="'+h(x.ref)+'">Editar</button> <button class="btn small" data-export="'+h(x.ref)+'">↓ ZIP</button></td></tr>').join('')+'</tbody></table></div>';
 return heading('Creaciones','Vista previa de las texturas reales de tus packs de VSRender.',button('import','↥ Importar')+' '+button('new-item','＋ Crear ítem','primary'))
 +'<div class="stats">'+count('Creaciones',items.length)+count('Ítems',items.filter(x=>x.type==='items').length)+count('Bloques',items.filter(x=>x.type==='blocks').length)+count('Muebles',items.filter(x=>x.type==='furniture').length)+'</div>'
 +'<div class="panel"><div class="filter"><input id="search" placeholder="Buscar por nombre o ID…" value="'+h(state.search)+'">'+options+kinds+buttons+'</div>'
 +(filtered.length?listing:'<div class="empty">Sin creaciones. Seleccioná otro pack o creá tu primer ítem.</div>')+'</div>'
 +'<div class="note">Los ítems con PNG del resource pack muestran su textura real. Los materiales vanilla sin PNG local muestran una ficha identificada; la vista web no simula el renderizado 3D del cliente de Minecraft.</div>'
 +(state.catalog.errors.length?'<p class="error">Archivos con errores: '+state.catalog.errors.length+'</p>':'');
}
const RECIPE_TYPES=[
 ['shaped','Mesa de trabajo · Con forma'],
 ['shapeless','Mesa de trabajo · Sin forma'],
 ['smelting','Horno'],
 ['smoking','Ahumador'],
 ['blasting','Alto horno'],
 ['campfire_cooking','Fogata'],
 ['stonecutting','Cortapiedras']
];
const cookingRecipe=type=>['smelting','smoking','blasting','campfire_cooking'].includes(type);
const singleRecipe=type=>cookingRecipe(type)||type==='stonecutting';
function renderRecipes(){
 const recipes=state.recipes.filter(x=>state.pack==='all'||x.pack===state.pack);
 const opts='<select id="recipe-pack-filter"><option value="all">Todos los packs</option>'+packOptions(state.pack)+'</select>';
 const cards=recipes.map(x=>'<article class="tile recipe-card"><span class="pill">'+h(RECIPE_TYPES.find(t=>t[0]===x.type)?.[1]||x.type)+'</span><h3>'+h(x.id)+'</h3><p>Resultado: <code>'+h(x.result)+'</code></p><small>Pack: '+h(x.pack)+'</small><div class="actions"><button class="btn small" data-recipe="'+h(x.ref)+'">Editar</button><button class="btn small" data-recipe-export="'+h(x.ref)+'">↓ YAML</button></div></article>').join('');
 return heading('Crafting Studio','Recetas reales de VSRender, sin motores adicionales.',button('new-recipe','＋ Nueva receta','primary'))
  +'<div class="panel"><div class="filter">'+opts+'</div>'
  +(recipes.length?'<div class="grid">'+cards+'</div>':'<div class="empty">Todavía no hay recetas compatibles. Podés crear una desde esta sección.</div>')
  +'</div><div class="note">Soporta mesa de trabajo, horno, ahumador, alto horno, fogata y cortapiedras. Guardá y aplicá la recarga de VSRender para probarlas en Minecraft.</div>';
}
function recipeGridFromDefinition(definition){
 const grid=Array(9).fill('');
 const type=String(definition?.type||'').replace('minecraft:','');
 if(type==='shaped'){
  const pat=definition.pattern||[];
  for(let i=0;i<3;i++)for(let j=0;j<3;j++){
   const symbol=String(pat[i]||'').charAt(j);
   if(symbol&&symbol!==' ')grid[i*3+j]=definition.ingredients?.[symbol]||'';
  }
 }else if(type==='shapeless'){
  (Array.isArray(definition.ingredients)?definition.ingredients:[]).slice(0,9).forEach((x,i)=>grid[i]=typeof x==='string'?x:'');
 }else{
  grid[0]=typeof definition.ingredients==='string'?definition.ingredients:'';
 }
 return grid;
}
function recipeIngredientOptions(){
 const ids=new Set(state.catalog.items.map(x=>x.id));
 state.materials.forEach(m=>ids.add('minecraft:'+m.id));
 return '<datalist id="recipe-items">'+[...ids].sort().map(x=>'<option value="'+h(x)+'"></option>').join('')+'</datalist>';
}
function recipeGridUI(type,slots){
 const total=singleRecipe(type)?1:9;
 return '<div class="recipe-grid '+(total===1?'recipe-single':'')+'">'+slots.slice(0,total).map((value,i)=>
  '<label class="recipe-slot"><small>Casilla '+(i+1)+'</small><input list="recipe-items" data-recipe-slot="'+i+'" placeholder="namespace:item" value="'+h(value||'')+'" autocomplete="off"></label>').join('')+'</div>';
}
function refreshRecipeGrid(){
 const type=get('recipe-type');
 const previous=Array(9).fill('');
 document.querySelectorAll('[data-recipe-slot]').forEach(el=>previous[Number(el.dataset.recipeSlot)]=el.value.trim());
 const grid=$('#recipe-layout');if(grid)grid.innerHTML=recipeGridUI(type,previous);
 const cooking=$('#recipe-cooking');if(cooking)cooking.hidden=!cookingRecipe(type);
}
async function openRecipe(refid=null){
 if(!state.catalog.packs.length){toast('Primero creá un pack');return}
 const entry=refid?await api('/api/recipe?ref='+ref(refid)):null;
 state.mode=entry?'recipe-edit':'recipe-create';
 state.entry=entry;
 const def=entry?.definition||{},type=String(def.type||'shaped').replace('minecraft:','');
 const result=typeof def.result==='object'?def.result||{}:{id:def.result||'',count:1};
 const packs='<div class="field"><label>Pack</label><select id="recipe-pack" '+(entry?'disabled':'')+'>'+packOptions(entry?.pack||'')+'</select></div>';
 const fields=packs+field('ID de la receta','recipe-id',entry?.id?.split(':')[1]||'','placeholder="ensalada_simple" '+(entry?'disabled':''))
  +selectField('Tipo de receta','recipe-type',RECIPE_TYPES,type)
  +field('Ítem resultante','recipe-result',result.id||'','list="recipe-items" placeholder="cocina:ensalada"')
  +field('Cantidad del resultado','recipe-count',String(result.count||1),'type="number" min="1" max="64"')
  +'<div id="recipe-cooking" class="fields" '+(!cookingRecipe(type)?'hidden':'')+'>'
  +field('Tiempo de cocción (ticks)','recipe-time',String(def.time||200),'type="number" min="1" max="72000"')
  +field('Experiencia','recipe-exp',String(def.exp??0),'type="number" min="0" max="100" step="0.1"')+'</div>';
 const footer=entry?'<div class="foot"><button data-action="recipe-export" class="btn small">Exportar YAML</button><button data-action="recipe-delete" class="btn small danger">Eliminar receta</button></div>':'';
 modal(entry?'Editar receta · '+entry.id:'Nueva receta',
  '<h3 class="form-section">Configuración</h3><div class="fields">'+fields+'</div>'
  +'<h3 class="form-section">Ingredientes</h3><p class="minor">Escribí un ID existente por casilla; las casillas vacías no consumen ingredientes.</p>'
  +recipeIngredientOptions()+'<div id="recipe-layout">'+recipeGridUI(type,recipeGridFromDefinition(def))+'</div>'
  +'<div class="note">Las recetas guardadas son YAML nativo de VSRender. Se crea un respaldo automático antes de modificar una receta.</div>'+footer,true);
}
function recipePayload(){
 const grid=Array(9).fill('');
 document.querySelectorAll('[data-recipe-slot]').forEach(el=>grid[Number(el.dataset.recipeSlot)]=el.value.trim());
 return {pack:get('recipe-pack'),id:get('recipe-id'),type:get('recipe-type'),
   result:get('recipe-result'),count:get('recipe-count'),grid,
   time:get('recipe-time'),experience:get('recipe-exp')};
}
function renderGuis(){
 const rows=state.guis.filter(x=>state.pack==='all'||state.pack===x.pack);
 const filter='<select id="gui-pack-filter"><option value="all">Todos los packs</option>'+packOptions(state.pack)+'</select>';
 const cards=rows.map(x=>'<article class="tile recipe-card"><span class="pill">'+x.rows+' filas · '+x.buttons+' botones</span><h3>'+h(x.title)+'</h3><p><code>'+h(x.pack)+'/'+h(x.id)+'</code></p><div class="actions"><button data-gui="'+h(x.ref)+'" class="btn small">Editar</button></div></article>').join('');
 return heading('GUI Studio','Inventarios visuales compatibles con /vsr gui open.',button('new-gui','＋ Nueva GUI','primary'))
 +'<div class="panel"><div class="filter">'+filter+'</div>'
 +(rows.length?'<div class="grid">'+cards+'</div>':'<div class="empty">Todavía no hay GUIs editables en este pack.</div>')
 +'</div><div class="note">Diseñá de 1 a 6 filas y agregá botones de ítems reales. Acciones disponibles: ninguna o cerrar. Fondo y estados dinámicos quedan para una versión posterior.</div>';
}
function guiCaptureButton(){
 if(!state.mode?.startsWith('gui-'))return;
 const item=get('gui-item'),action=get('gui-action')||'none';
 if(item)state.guiButtons[String(state.guiSelected)]={item,action};
 else delete state.guiButtons[String(state.guiSelected)];
}
function guiPaint(){
 const rows=Number(get('gui-rows'))||3;
 for(const k of Object.keys(state.guiButtons))if(Number(k)>=rows*9)delete state.guiButtons[k];
 const grid=$('#gui-grid');if(!grid)return;
 grid.innerHTML=Array.from({length:rows*9},(_,slot)=>{
  const button=state.guiButtons[String(slot)];
  const title=button?.item?.split(':')[1]||'';
  return '<button type="button" class="gui-slot '+(slot===state.guiSelected?'selected':'')+'" data-gui-slot="'+slot+'" title="Casilla '+slot+'">'
   +'<small>'+slot+'</small><b>'+h(title.slice(0,10)||'＋')+'</b></button>';
 }).join('');
}
function guiInspector(){
 const slot=state.guiSelected;
 const current=state.guiButtons[String(slot)]||{};
 const panel=$('#gui-inspector');if(!panel)return;
 panel.innerHTML='<h3>Casilla '+slot+'</h3><div class="fields">'
  +field('Ítem','gui-item',current.item||'','list="recipe-items" placeholder="minecraft:emerald"')
  +selectField('Acción','gui-action',[['none','Sin acción'],['close','Cerrar inventario']],current.action||'none')
  +'</div><p class="minor">Para dejar la casilla vacía, borrá el ID del ítem.</p>';
}
async function openGui(refid=null){
 if(!state.catalog.packs.length){toast('Primero creá un pack');return}
 const entry=refid?await api('/api/gui?ref='+ref(refid)):null;
 state.mode=entry?'gui-edit':'gui-create';state.entry=entry;
 state.guiButtons={...(entry?.buttons||{})};state.guiSelected=0;
 const pack='<div class="field"><label>Pack</label><select id="gui-pack" '+(entry?'disabled':'')+'>'+packOptions(entry?.pack||'')+'</select></div>';
 const rows=[1,2,3,4,5,6].map(i=>[String(i),i+' filas']);
 const fields=pack+field('ID de GUI','gui-id',entry?.id||'','placeholder="mesa_preparacion" '+(entry?'disabled':''))
  +field('Título del inventario','gui-title',entry?.title||'Mesa de preparación')
  +selectField('Filas','gui-rows',rows,String(entry?.rows||3));
 const footer=entry?'<div class="foot"><button class="btn danger small" data-action="gui-delete">Eliminar GUI</button></div>':'';
 modal(entry?'GUI Studio · '+entry.id:'Crear GUI','<div class="fields">'+fields+'</div>'
  +recipeIngredientOptions()+'<div class="gui-editor"><div id="gui-grid" class="gui-grid"></div><div id="gui-inspector" class="gui-inspector"></div></div>'
  +'<div class="note">Seleccioná una casilla del inventario para asignarle un ítem. Al modificar una GUI, se guarda primero un respaldo.</div>'+footer,true);
 guiPaint();guiInspector();
}
function guiPayload(){
 guiCaptureButton();
 return {pack:get('gui-pack'),id:get('gui-id'),title:get('gui-title'),
   rows:get('gui-rows'),buttons:state.guiButtons};
}
function renderPacks(){return heading('Packs','Paquetes disponibles en resources/.',button('new-pack','＋ Crear pack','primary')+' <button class="btn" data-action="import-pack">↑ Importar pack ZIP</button>')+'<div class="grid">'+state.catalog.packs.map(p=>'<div class="tile"><h3>'+h(p.name)+'</h3><p>Namespace: '+h(p.namespace)+'</p><p>'+h(p.description)+'</p><small>'+state.catalog.items.filter(x=>x.pack===p.name).length+' ítems</small></div>').join('')+'</div>'}
function renderTemplates(){return heading('Plantillas','Generá contenido utilizando los comportamientos reales de VSRender.')+'<div class="grid">'+state.templates.map(x=>'<div class="tile"><small class="minor">'+h(TEMPLATE_GROUPS[x.id]||'PLANTILLA')+'</small><h3>'+h(x.name)+'</h3><p>'+h(x.details)+'</p><button class="btn primary" data-template="'+h(x.id)+'">Usar plantilla →</button></div>').join('')+'</div><div class="note">Platos variables: esta plantilla solo crea un ítem base. La estación que acumula ingredientes y calcula valores por plato está pendiente. Cultivos completos se preparan con la plantilla YAML de cocina.</div>'}
function parseDoc(raw){return raw.split('\n').map(line=>{
 let x=h(line);if(x.startsWith('# '))return '<h1>'+x.slice(2)+'</h1>';
 if(x.startsWith('## '))return '<h2>'+x.slice(3)+'</h2>';
 if(x.startsWith('- '))return '<p>• '+x.slice(2)+'</p>';
 return x.trim()?'<p>'+x+'</p>':'';
}).join('')}
async function renderDocs(){
 const page=await api('/api/docs?slug='+ref(state.doc));
 $('#page').innerHTML=heading('Documentación','Guías integradas en VSRender.')+'<div class="docs"><div class="doc-nav">'+state.docs.map(d=>'<button data-doc="'+h(d.slug)+'" class="'+(d.slug===state.doc?'selected':'')+'">'+h(d.title)+'</button>').join('')+'</div><div class="panel doc-body">'+parseDoc(page.markdown)+'</div></div>';
}
function render(){
 $('#crumb').textContent=names[state.page];
 document.querySelectorAll('[data-page]').forEach(el=>el.classList.toggle('selected',el.dataset.page===state.page));
 if(state.page==='docs'){renderDocs().catch(e=>toast(e.message));return}
 $('#page').innerHTML=state.page==='packs'?renderPacks():state.page==='templates'?renderTemplates():state.page==='recipes'?renderRecipes():state.page==='guis'?renderGuis():renderItems();
}
async function refresh(show=false){try{
 const updated=await api('/api/catalog');
  // Recipe and GUI inventories are disk-backed; fetch only when their page needs them.
  const recipes=state.page==='recipes'?await api('/api/recipes'):state.recipes;
  const guis=state.page==='guis'?await api('/api/guis'):state.guis;
  const oldRecipes=JSON.stringify(state.recipes.map(x=>x.ref+'@'+x.revision));
  const oldGuis=JSON.stringify(state.guis.map(x=>x.ref+'@'+x.revision));
  state.recipes=recipes;
  state.guis=guis;
 const oldDigest=state.catalog.items?.map(x=>x.ref+'@'+x.revision).join('|')||'';
 const newDigest=updated.items?.map(x=>x.ref+'@'+x.revision).join('|')||'';
 const packsChanged=JSON.stringify(updated.packs)!==JSON.stringify(state.catalog.packs);
 state.catalog=updated;
 $('#count').textContent=updated.items.length;
 if(!state.modal && (show || !$('#page').querySelector('.panel') || oldDigest!==newDigest || packsChanged || (state.page==='recipes'&&oldRecipes!==JSON.stringify(recipes.map(x=>x.ref+'@'+x.revision))) || (state.page==='guis'&&oldGuis!==JSON.stringify(guis.map(x=>x.ref+'@'+x.revision)))))render();
 if(show)toast('Catálogo actualizado');
 }catch(e){toast(e.message);$('#page').innerHTML='<p class="error">'+h(e.message)+'</p>'}}
function modal(title,body,wide=false){
 state.modal=true;
 $('#modal').innerHTML='<div class="overlay"><div class="dialog '+(wide?'wide':'')+'" role="dialog" aria-modal="true"><h2>'+h(title)+'</h2><p>Guardá los datos en el pack seleccionado.</p>'+body+'<div class="foot"><button class="btn" data-action="close">Cancelar</button><button class="btn primary" data-action="save">Guardar cambios</button></div></div></div>';
}
function field(label,id,value='',extra=''){return '<div class="field"><label for="'+id+'">'+h(label)+'</label><input id="'+id+'" value="'+h(value)+'" '+extra+'></div>'}
function close(){state.modal=false;state.entry=null;$('#modal').innerHTML=''}
const DISHES={
 sopa:{label:'Sopa',material:'beetroot_soup',remainder:'bowl'},
 ensalada:{label:'Ensalada',material:'bowl',remainder:'bowl'},
 guiso:{label:'Guiso',material:'beetroot_soup',remainder:'bowl'},
 estofado:{label:'Estofado',material:'rabbit_stew',remainder:'bowl'},
 tarta:{label:'Tarta',material:'pumpkin_pie',remainder:'none'}
};
function chooseDish(value,initial=false){
 const selected=DISHES[value];if(!selected)return;
 const prev=DISHES[state.dishType]||DISHES.sopa;
 for(const [id,key] of [['name','label'],['item-id','id']]){
  const input=$('#'+id);if(!input)continue;
  const before=key==='id'?state.dishType:prev.label;
  const after=key==='id'?value:selected.label;
  if(initial||!input.value.trim()||input.value===before)input.value=after;
 }
 const base=$('#material');
 if(base&&(initial||!base.value||base.value===prev.material))base.value=selected.material;
 const remain=$('#remainder');
 if(remain&&(initial||remain.value===prev.remainder))remain.value=selected.remainder;
 state.dishType=value;
 previewNew();
}
const BLOCK_TEMPLATES=new Set(['bloque','mineral','bloque_luz','bloque_orientable']);
const NON_FOOD_TEMPLATES=new Set(['ingrediente','objeto','herramienta','taladro','equipable','bloque','mineral','bloque_luz','bloque_orientable','mueble','asiento','expositor']);
const TEMPLATE_MATERIALS={ingrediente:'paper',objeto:'paper',alimento:'apple',bebida:'honey_bottle',plato:'beetroot_soup',cultivo:'carrot',herramienta:'iron_pickaxe',taladro:'iron_pickaxe',equipable:'leather_helmet',bloque:'stone',mineral:'stone',bloque_luz:'glowstone',bloque_orientable:'stone',mueble:'paper',asiento:'paper',expositor:'paper'};
const TEMPLATE_STACKS={plato:1,herramienta:1,taladro:1,equipable:1,mueble:1,asiento:1,expositor:1,bloque:64,mineral:64,bloque_luz:64,bloque_orientable:64,ingrediente:64,objeto:64};
const TEMPLATE_GROUPS={ingrediente:'Ítems',objeto:'Ítems',alimento:'Cocina',bebida:'Cocina',plato:'Cocina',cultivo:'Cultivos',herramienta:'Equipamiento',taladro:'Equipamiento',equipable:'Equipamiento',bloque:'Bloques',mineral:'Bloques',bloque_luz:'Bloques',bloque_orientable:'Bloques',mueble:'Decoración',asiento:'Decoración',expositor:'Decoración'};
const EFFECT_CHOICES=[
 ['','Sin efecto de estado'],['speed','Velocidad'],['slowness','Lentitud'],['haste','Prisa'],
 ['strength','Fuerza'],['regeneration','Regeneración'],['resistance','Resistencia'],['fire_resistance','Resistencia al fuego'],
 ['night_vision','Visión nocturna'],['jump_boost','Salto mejorado'],['water_breathing','Respiración acuática'],
 ['absorption','Absorción'],['saturation','Saturación'],['poison','Veneno'],['hunger','Hambre'],
 ['weakness','Debilidad'],['wither','Wither'],['slow_falling','Caída lenta'],['invisibility','Invisibilidad']];
function selectField(label,id,entries,selected=''){
 return '<div class="field"><label for="'+id+'">'+h(label)+'</label><select id="'+id+'">'+entries.map(([v,n])=>'<option value="'+h(v)+'" '+(v===selected?'selected':'')+'>'+h(n)+'</option>').join('')+'</select></div>';
}
function previewNew(){
 const name=get('name')||'Nuevo ítem';
 const material=get('material')||TEMPLATE_MATERIALS[state.template]||'paper';
 const image=$('#new-image');
 const tex=get('texture'),pack=get('pack');
 if(image){
  const letters=material.split('_').slice(0,2).map(x=>x.charAt(0).toUpperCase()).join('');
  const preview=tex&&pack?'<img alt="Vista previa del PNG" src="/api/texture?pack='+ref(pack)+'&id='+ref(tex)+'">':'';
  image.classList.toggle('missing',!preview);
  image.innerHTML=preview+'<span class="item-fallback"><b>'+h(letters)+'</b><small>'+h(material.replace(/_/g,' '))+'</small></span>';
 }
 const text=$('#preview-copy');
 if(text)text.innerHTML='<b>'+h(name)+'</b><small>Base: '+h(material)+'</small><small>'+(NON_FOOD_TEMPLATES.has(state.template)?h(BLOCK_TEMPLATES.has(state.template)?'Bloque colocable con loot':['mueble','asiento','expositor'].includes(state.template)?'Mueble colocable':['herramienta','taladro'].includes(state.template)?'Herramienta con durabilidad':'Sin consumo'):(get('nutrition')||'4')+' nutrición · '+(get('saturation')||'1.2')+' saturación · '+(get('effect')||'Sin efecto'))+'</small>'
  +(state.template==='cultivo'?'<small>Incluye semillas y cultivo de '+h(get('stages')||'4')+' etapas</small>':'');
}
async function newItem(type='ingrediente'){
 if(!state.catalog.packs.length){toast('Primero creá un pack');return}
 state.mode='create';state.template=type;state.dishType='sopa';
 const base=TEMPLATE_MATERIALS[type]||'paper';
 const options=state.materials.map(x=>'<option value="'+h(x.id)+'"></option>').join('');
 const isFood=!NON_FOOD_TEMPLATES.has(type);
 const basic=(type==='plato'?selectField('Tipo de plato','dish-type',Object.entries(DISHES).map(([id,p])=>[id,p.label])):'')
 +'<div class="field"><label>Pack de destino</label><select id="pack">'+packOptions('')+'</select></div>'
 +field('Nombre','name','', 'placeholder="Ej. Lámpara, pico o sopa"')
 +field('ID (opcional)','item-id','', 'placeholder="Se genera automáticamente"')
 +field('Material de Minecraft','material',base,'list="materials"')
 +'<datalist id="materials">'+options+'</datalist>'
 +field('Textura personalizada'+(['mueble','asiento','expositor','bloque_orientable'].includes(type)?' (obligatoria)':' (opcional)'),'texture','', 'list="textures" placeholder="cocina:'+(BLOCK_TEMPLATES.has(type)?'block':'item')+'/mi_textura"')
 +'<datalist id="textures"></datalist>'
 +'<div class="field"><label for="texture-file">Subir PNG al pack (máx. 500 KB)</label><input id="texture-file" type="file" accept="image/png"><small>Se guarda en el resource pack real y se selecciona automáticamente.</small></div>'
 +field('Descripción / lore','description','', 'placeholder="Descripción visible en Minecraft"')
 +field('Apilamiento máximo','stack',String(TEMPLATE_STACKS[type]||16),'type="number" min="1" max="99" '+(['herramienta','taladro','equipable'].includes(type)?'readonly':''));
 const consumption=isFood?
   '<h3 class="form-section">Consumo</h3><div class="fields">'
   +field('Nutrición (0–20)','nutrition',type==='bebida'?'0':'4','type="number" min="0" max="20"')
   +field('Saturación (0–20)','saturation',type==='bebida'?'0':'1.2','type="number" step="0.1" min="0" max="20"')
   +field('Tiempo para consumir (segundos)','consume-seconds','1.6','type="number" step="0.1" min="0.05" max="60"')
   +selectField('Animación al consumir','animation',[['eat','Comer'],['drink','Beber']],type==='bebida'?'drink':'eat')
   +selectField('Recipiente que queda','remainder',[['none','Ninguno'],['bowl','Cuenco'],['glass_bottle','Botella de cristal'],['bucket','Balde']],type==='plato'?'bowl':type==='bebida'?'glass_bottle':'none')
   +'<div class="field"><label class="check"><input type="checkbox" id="always-eat" '+(type==='bebida'?'checked':'')+'> Se puede consumir con el hambre llena</label></div>'
   +'</div><h3 class="form-section">Efectos al consumir (opcional)</h3><div class="fields">'
   +selectField('Efecto de estado','effect',EFFECT_CHOICES)
   +field('Nivel del efecto (I–V)','effect-level','1','type="number" min="1" max="5"')
   +field('Duración (segundos)','effect-seconds','10','type="number" min="1" max="3600"')
   +field('Probabilidad (0–1)','effect-chance','1','type="number" step="0.1" min="0" max="1"')
   +selectField('Segundo efecto (opcional)','effect2',EFFECT_CHOICES)
   +field('Nivel del segundo efecto','effect2-level','1','type="number" min="1" max="5"')
   +field('Duración del segundo (segundos)','effect2-seconds','10','type="number" min="1" max="3600"')
   +field('Probabilidad del segundo (0–1)','effect2-chance','1','type="number" step="0.1" min="0" max="1"')
   +'</div>'
   :'';
 const specialized=['herramienta','taladro'].includes(type)
  ?'<h3 class="form-section">Herramienta o arma</h3><div class="fields">'
   +field('Durabilidad máxima','max-damage','256','type="number" min="1" max="100000"')
   +(type==='taladro'?selectField('Profundidad de minería','mining-depth',[['1','1 bloque'],['2','2 bloques'],['3','3 bloques']],'2'):'')
   +'</div><div class="note">'+(type==='taladro'?'El taladro usa range_mining_item nativo. Elegí iron_pickaxe, diamond_pickaxe u otro pico. La profundidad se limita a 3 bloques para evitar rangos excesivos.':'Elegí una base con durabilidad, como iron_pickaxe, iron_axe o iron_sword. Se mantienen sus atributos vanilla.')+'</div>'
  :type==='equipable'
  ?'<h3 class="form-section">Equipamiento</h3><div class="fields">'
   +selectField('Ranura de equipamiento','equip-slot',[['head','Cabeza'],['chest','Pecho'],['legs','Piernas'],['feet','Pies']],'head')
   +'</div><div class="note">Elegí una base de armadura adecuada. Las texturas 3D de armadura se preparan por separado.</div>'
  :BLOCK_TEMPLATES.has(type)
  ?'<h3 class="form-section">Bloque colocable</h3><div class="fields">'
   +field('Dureza','hardness',type==='mineral'?'3':'1.5','type="number" min="0" max="50" step="0.1"')
   +field('Resistencia a explosiones','resistance','3','type="number" min="0" max="1200" step="0.1"')
   +selectField('Sonido','block-sound',[['stone','Piedra'],['wood','Madera'],['metal','Metal'],['glass','Vidrio'],['sand','Arena'],['grass','Hierba']],'stone')
   +(type==='bloque_luz'?field('Nivel de luz (0–15)','luminance','15','type="number" min="0" max="15"'):'')
   +'</div><div class="note">'+(type==='bloque_orientable'?'Orientación de cuatro direcciones usando la textura como patrón de cerámica. Subí un PNG asimétrico para notar la rotación.':type==='bloque_luz'?'El bloque emite luz con el nivel seleccionado; no incorpora interruptor.':type==='mineral'?'Mineral minable con pico que devuelve el propio bloque; el editor de minerales complejos queda separado.':'Bloque sólido con textura, dureza y loot propio.')+'</div>'
  :['mueble','asiento','expositor'].includes(type)
  ?'<div class="note">'+(type==='asiento'?'Silla colocable: genera hitbox y asiento con seat nativo.':type==='expositor'?'Expositor de objetos: permite colocar y retirar ítems con display_item_furniture.':'Mueble de suelo con display de ítem y hitbox.')+' Subí un PNG propio; los modelos 3D complejos se importan aparte.</div>'
  :'';
 modal('Nueva plantilla · '+type,
  '<div class="create-preview"><span id="new-image" class="item-slot large missing"><b>??</b></span><span id="preview-copy"></span></div>'
  +'<h3 class="form-section">Datos principales</h3><div class="fields">'+basic+'</div>'
  +consumption
  +specialized
  +(type==='cultivo'?'<h3 class="form-section">Cultivo y semillas</h3><div class="fields">'
      +field('Etapas de crecimiento (2–8)','stages','4','type="number" min="2" max="8"')
      +field('Velocidad de crecimiento (0.01–1)','grow-speed','0.25','type="number" min="0.01" max="1" step="0.01"')
      +field('Textura de semillas (opcional)','seed-texture','', 'placeholder="cocina:item/tomate_semillas"')
      +'</div>':'')
  +(type==='cultivo'?'<div class="note">Genera alimento, semillas y bloque cultivable. Las etapas usan trigo vanilla como vista temporal. No se generan modelos personalizados ni recetas.</div>':'<div class="note">El material es la base de Minecraft; la textura define su apariencia personalizada. Esta plantilla no crea recetas ni estaciones de cocina automáticamente.</div>'),true);
 if(type==='plato')chooseDish('sopa',true);
 previewNew();await loadTextures();
}
async function loadTextures(){
 const pack=get('pack')||state.entry?.pack;
 if(!pack)return;
 try {
  const list=await api('/api/textures?pack='+ref(pack));
  const datalist=$('#textures');
  if(datalist)datalist.innerHTML=list.map(x=>'<option value="'+h(x)+'"></option>').join('');
 }catch(e){toast('No se pudieron listar las texturas: '+e.message)}
}
async function uploadImage(file){
 if(!file || !state.modal || !['create','edit'].includes(state.mode))return;
 if(file.size>500000)throw Error('PNG demasiado grande: máximo 500 KB');
 const bytes=new Uint8Array(await file.arrayBuffer());
 if(bytes[0]!==137 || bytes[1]!==80 || bytes[2]!==78 || bytes[3]!==71)throw Error('Seleccioná un PNG válido');
 const suggested=(file.name.replace(/\.png$/i,'')||get('item-id')||get('name')||state.entry?.id.split(':')[1])
 .normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_+|_+$/g,'').slice(0,58);
 if(!suggested)throw Error('Primero escribí el nombre de la creación');
 const activePack=get('pack')||state.entry?.pack;
 const folder=state.mode==='create'&&BLOCK_TEMPLATES.has(state.template)?'block':'item';
 const resp=await fetch('/api/texture?pack='+ref(activePack)+'&name='+ref(suggested)+'&folder='+ref(folder),{
  method:'POST',headers:{'X-VSRender-Editor':'1','Content-Type':'image/png'},body:bytes
 });
 const data=await resp.json();
 if(!resp.ok)throw Error(data.error||'No se pudo subir la textura');
 const input=state.mode==='edit'?'#edit-texture':'#texture';
 $(input).value=data.texture;
 await loadTextures();
 if(state.mode==='edit') {
   const slot=$('#modal .item-slot');
   if(slot){slot.classList.remove('missing');slot.querySelector('img').src='/api/texture?pack='+ref(activePack)+'&id='+ref(data.texture)}
 } else previewNew();
 toast('PNG guardado: '+data.texture);
}
async function editItem(refid){
 const entry=await api('/api/item?ref='+ref(refid));
 state.entry=entry;state.mode='edit';
 modal('Editar '+entry.id,'<div class="create-preview">'+slot(entry,true)+'<div id="preview-copy"><b>'+h(entry.name)+'</b><small>'+h(entry.material)+'</small><small>'+h(entry.file)+'</small></div></div><div class="fields">'+field('Nombre','edit-name',entry.name)+field('Material','edit-material',entry.material)+field('Textura del ítem','edit-texture',entry.texture||'','list="textures" placeholder="cocina:item/cebolla"')+'<div class="field"><label>Subir PNG nuevo</label><input type="file" id="texture-file-edit" accept="image/png"><small>Se añade al pack sin sobrescribir imágenes.</small></div><datalist id="textures"></datalist></div><div class="field full"><label>YAML avanzado</label><textarea id="edit-yaml">'+h(entry.yaml)+'</textarea></div><div class="note">Se conservarán las otras definiciones del archivo. Se crea un respaldo automático antes de guardar.</div><div class="foot"><button class="btn small" data-action="export-current">Exportar ZIP</button><button class="btn small" data-action="duplicate">Duplicar</button><button class="btn danger small" data-action="delete">Eliminar</button></div>',true);
 await loadTextures();
}
function askPack(){state.mode='pack';modal('Nuevo pack','<div class="fields">'+field('Nombre','name','', 'placeholder="cocina"')+field('Namespace','namespace','', 'placeholder="cocina"')+'</div>')}
function askPackImport(){state.mode='pack-import';modal('Importar pack completo','<div class="field"><label>Archivo ZIP del pack</label><input type="file" id="pack-file" accept=".zip"></div><div class="note">Subí un ZIP con una carpeta de pack (por ejemplo armas/pack.yml), o con pack.yml en la raíz. Incluye ítems, bloques, recetas, modelos y texturas. Máximo 8 MB comprimido; no sobrescribe packs existentes.</div>')}
function askImport(){state.mode='import';modal('Importar ítem','<div class="fields"><div class="field"><label>Pack destino</label><select id="pack">'+packOptions('')+'</select></div><div class="field"><label>Archivo YAML/ZIP</label><input type="file" id="file" accept=".yml,.yaml,.zip"></div></div><div class="note">No sobrescribe ítems existentes. No se instalan scripts de terceros.</div>')}
function askDuplicate(){state.mode='duplicate';state.backupEntry=state.entry;modal('Duplicar '+state.entry.id,'<div class="fields"><div class="field"><label>Pack</label><select id="pack">'+packOptions(state.entry.pack)+'</select></div>'+field('Namespace','namespace',state.entry.id.split(':')[0])+field('ID','item-id',state.entry.id.split(':')[1]+'_copia')+'</div>')}
async function save(){
 if(state.mode==='gui-create'||state.mode==='gui-edit'){
  const body=guiPayload();const e=state.entry;
  if(e)body.revision=e.revision;
  const result=await api(e?'/api/gui?ref='+ref(e.ref):'/api/gui',e?'PUT':'POST',body);
  close();await refresh(true);toast('GUI guardada: '+result.id+' · /vsr gui open '+result.pack+' '+result.id);return;
 }

 if(state.mode==='recipe-create'||state.mode==='recipe-edit'){
  const body=recipePayload();
  const e=state.entry;
  if(e)body.revision=e.revision;
  const result=await api(e?'/api/recipe?ref='+ref(e.ref):'/api/recipe',e?'PUT':'POST',body);
  close();await refresh(true);toast('Receta guardada: '+result.id+' · Aplicá al juego');return;
 }

 if(state.mode==='pack'){
  const p=await api('/api/packs','POST',{name:get('name'),namespace:get('namespace')});close();await refresh();toast('Pack creado: '+p.name);return;
 }
 if(state.mode==='pack-import'){
  const file=$('#pack-file')?.files?.[0];
  if(!file)throw Error('Seleccioná un ZIP de pack');
  if(file.size>8*1024*1024)throw Error('ZIP mayor a 8 MB');
  const r=await fetch('/api/import-pack',{method:'POST',headers:{'X-VSRender-Editor':'1','X-VSRender-File':file.name},body:file});
  const data=await r.json();if(!r.ok)throw Error(data.error||'No se pudo importar el pack');
  close();await refresh(true);toast('Pack instalado: '+data.name+' ('+data.files+' archivos). Aplicá al juego.');return;
 }
 if(state.mode==='create'){
  const pack=get('pack');const p=state.catalog.packs.find(p=>p.name===pack);
  const body={pack,namespace:p?.namespace,name:get('name'),id:get('item-id'),material:get('material'),template:state.template,
   max_stack_size:get('stack'),description:get('description'),texture:get('texture'),dish_type:get('dish-type'),
   nutrition:get('nutrition'),saturation:get('saturation'),consume_seconds:get('consume-seconds'),
   animation:get('animation'),remainder:get('remainder'),always_eat:$('#always-eat')?.checked||false,
   effect:get('effect'),effect_level:get('effect-level'),effect_seconds:get('effect-seconds'),effect_chance:get('effect-chance'),
   effect2:get('effect2'),effect2_level:get('effect2-level'),effect2_seconds:get('effect2-seconds'),effect2_chance:get('effect2-chance'),
    stages:get('stages'),grow_speed:get('grow-speed'),seed_texture:get('seed-texture'),
    max_damage:get('max-damage'),mining_depth:get('mining-depth'),equip_slot:get('equip-slot'),hardness:get('hardness'),resistance:get('resistance'),block_sound:get('block-sound'),luminance:get('luminance')};
  const result=await api(state.template==='ingrediente'?'/api/create':'/api/template','POST',body);
  close();await refresh();toast('Creado: '+result.id);await editItem(result.ref);return;
 }
 if(state.mode==='edit'){
  const e=state.entry,raw=$('#edit-yaml').value;
  const changed=raw.trimEnd()!==e.yaml.trimEnd();
  const body=changed?{revision:e.revision,yaml:raw}:{revision:e.revision,name:get('edit-name'),material:get('edit-material'),texture:get('edit-texture')};
  const result=await api('/api/item?ref='+ref(e.ref),'PUT',body);
  close();await refresh();toast('Guardado: '+result.id+'. Aplicá al juego.');return;
 }
 if(state.mode==='duplicate'){
  const e=state.backupEntry;
  const result=await api('/api/duplicate','POST',{ref:e.ref,pack:get('pack'),namespace:get('namespace'),id:get('item-id')});
  close();await refresh();toast('Duplicado: '+result.id);return;
 }
 if(state.mode==='import'){
  const file=$('#file')?.files[0];if(!file)throw Error('Seleccioná un archivo');
  if(file.size>1024*1024)throw Error('Máximo 1 MB para importar');
  const r=await fetch('/api/import?pack='+ref(get('pack')),{method:'POST',headers:{'X-VSRender-Editor':'1','X-VSRender-File':file.name},body:file});
  const data=await r.json();if(!r.ok)throw Error(data.error);
  close();await refresh();toast('Importado: '+data.id);
 }
}
async function attempt(fn){try{await fn()}catch(e){toast('Error: '+e.message)}}
document.addEventListener('click',ev=>{
 const el=ev.target.closest('[data-page],[data-action],[data-open],[data-export],[data-template],[data-doc],[data-view],[data-recipe],[data-recipe-export],[data-gui],[data-gui-slot]');
 if(!el)return;
 attempt(async()=>{
 if(el.dataset.page){state.page=el.dataset.page;render();if(['recipes','guis'].includes(state.page))await refresh();return}
 if(el.dataset.view){state.view=el.dataset.view;render();return}
 if(el.dataset.doc){state.doc=el.dataset.doc;await renderDocs();return}
 if(el.dataset.open){await editItem(el.dataset.open);return}
  if(el.dataset.recipe){await openRecipe(el.dataset.recipe);return}
  if(el.dataset.gui){await openGui(el.dataset.gui);return}
  if(el.dataset.guiSlot!==undefined){guiCaptureButton();state.guiSelected=Number(el.dataset.guiSlot);guiPaint();guiInspector();return}
  if(el.dataset.recipeExport){location.href='/api/recipe/export?ref='+ref(el.dataset.recipeExport);return}
 if(el.dataset.export){location.href='/api/export?ref='+ref(el.dataset.export);return}
 if(el.dataset.template){await newItem(el.dataset.template);return}
 switch(el.dataset.action){
 case 'new-item':await newItem();break;
  case 'new-recipe':await openRecipe();break;
  case 'new-gui':await openGui();break;
  case 'gui-delete':if(confirm('¿Eliminar '+state.entry.id+'? Se guardará un respaldo.')){
   await api('/api/gui?ref='+ref(state.entry.ref),'DELETE',{revision:state.entry.revision});
   close();await refresh(true);toast('GUI eliminada');}break;
  case 'recipe-export':location.href='/api/recipe/export?ref='+ref(state.entry.ref);break;
  case 'recipe-delete':if(confirm('¿Eliminar '+state.entry.id+'? Se guardará un respaldo.')){
   await api('/api/recipe?ref='+ref(state.entry.ref),'DELETE',{revision:state.entry.revision});
   close();await refresh(true);toast('Receta eliminada');}break;
 case 'new-pack':askPack();break;
 case 'import-pack':askPackImport();break;
 case 'import':askImport();break;
 case 'close':close();break;
 case 'save':await save();break;
 case 'export-current':location.href='/api/export?ref='+ref(state.entry.ref);break;
 case 'duplicate':askDuplicate();break;
 case 'delete':if(confirm('¿Eliminar '+state.entry.id+'? Se guardará un respaldo.')){await api('/api/item?ref='+ref(state.entry.ref),'DELETE',{revision:state.entry.revision});close();await refresh();toast('Eliminado')}break;
 }
 });
});
document.addEventListener('input',ev=>{
 if(ev.target.id==='search'){state.search=ev.target.value;const pos=ev.target.selectionStart;render();const el=$('#search');el?.focus();el?.setSelectionRange(pos,pos)}
 if(state.modal&&state.mode==='create')previewNew();
});
document.addEventListener('change',ev=>{
 if(state.modal&&state.mode?.startsWith('gui-')&&['gui-item','gui-action'].includes(ev.target.id)){guiCaptureButton();guiPaint();return}
 if(ev.target.id==='gui-rows'&&state.modal&&state.mode?.startsWith('gui-')){guiCaptureButton();guiPaint();guiInspector();return}
 if(ev.target.id==='gui-pack-filter'){state.pack=ev.target.value;render();return}
 if(ev.target.id==='recipe-type'&&state.modal&&state.mode.startsWith('recipe-')){refreshRecipeGrid();return}
 if(ev.target.id==='recipe-pack-filter'){state.pack=ev.target.value;render();return}
 if(ev.target.id==='dish-type'&&state.modal&&state.mode==='create'){chooseDish(ev.target.value);return}
 if(ev.target.id==='pack-filter'){state.pack=ev.target.value;render()}
 if(ev.target.id==='kind-filter'){state.kind=ev.target.value;render()}
 if(ev.target.id==='pack' && state.mode==='create'){attempt(loadTextures);previewNew()}
 if(ev.target.id==='texture-file' || ev.target.id==='texture-file-edit'){attempt(async()=>uploadImage(ev.target.files?.[0]))}
});
document.addEventListener('error',ev=>{if(ev.target?.tagName==='IMG')ev.target.closest('.item-slot')?.classList.add('missing')},true);
document.addEventListener('keydown',ev=>{if(ev.key==='Escape'&&state.modal)close()});
$('#refresh').addEventListener('click',()=>refresh(true));
$('#reload').addEventListener('click',()=>attempt(async()=>{await api('/api/reload','POST',{});toast('Recarga solicitada; revisá la consola del servidor.')}));
async function boot(){
 try{
  state.materials=(await api('/api/materials')).materials;
  state.templates=await api('/api/templates');
  state.docs=await api('/api/docs');
  await refresh();
  setInterval(()=>{if(!state.modal&&state.page!=='docs'&&!document.activeElement?.matches('input,textarea'))refresh()},6500);
 }catch(e){toast(e.message);$('#page').textContent=e.message}
}
boot();