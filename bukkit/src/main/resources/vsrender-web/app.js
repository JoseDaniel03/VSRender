'use strict';
const state={page:'items',catalog:{items:[],packs:[],errors:[]},materials:[],templates:[],docs:[],doc:'introduccion',search:'',view:'cards',pack:'all',kind:'all',modal:false,mode:'',entry:null};
const $=s=>document.querySelector(s);
const h=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const ref=s=>encodeURIComponent(s);
const get=id=>$('#'+id)?.value?.trim()||'';
const names={items:'Creaciones',packs:'Packs',templates:'Plantillas',docs:'Documentación'};
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
function renderPacks(){return heading('Packs','Paquetes disponibles en resources/.',button('new-pack','＋ Crear pack','primary'))+'<div class="grid">'+state.catalog.packs.map(p=>'<div class="tile"><h3>'+h(p.name)+'</h3><p>Namespace: '+h(p.namespace)+'</p><p>'+h(p.description)+'</p><small>'+state.catalog.items.filter(x=>x.pack===p.name).length+' ítems</small></div>').join('')+'</div>'}
function renderTemplates(){return heading('Plantillas','Reutilizá el motor de ítems actual, sin código adicional.')+'<div class="grid">'+state.templates.map(x=>'<div class="tile"><small class="minor">PLANTILLA</small><h3>'+h(x.name)+'</h3><p>'+h(x.details)+'</p><button class="btn primary" data-template="'+h(x.id)+'">Usar plantilla →</button></div>').join('')+'</div><div class="note">Platos variables: esta plantilla solo crea un ítem base. La estación que acumula ingredientes y calcula valores por plato está pendiente. Cultivos completos se preparan con la plantilla YAML de cocina.</div>'}
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
 $('#page').innerHTML=state.page==='packs'?renderPacks():state.page==='templates'?renderTemplates():renderItems();
}
async function refresh(show=false){try{
 const updated=await api('/api/catalog');
 const oldDigest=state.catalog.items?.map(x=>x.ref+'@'+x.revision).join('|')||'';
 const newDigest=updated.items?.map(x=>x.ref+'@'+x.revision).join('|')||'';
 const packsChanged=JSON.stringify(updated.packs)!==JSON.stringify(state.catalog.packs);
 state.catalog=updated;
 $('#count').textContent=updated.items.length;
 if(!state.modal && (show || !$('#page').querySelector('.panel') || oldDigest!==newDigest || packsChanged))render();
 if(show)toast('Catálogo actualizado');
 }catch(e){toast(e.message);$('#page').innerHTML='<p class="error">'+h(e.message)+'</p>'}}
function modal(title,body,wide=false){
 state.modal=true;
 $('#modal').innerHTML='<div class="overlay"><div class="dialog '+(wide?'wide':'')+'" role="dialog" aria-modal="true"><h2>'+h(title)+'</h2><p>Guardá los datos en el pack seleccionado.</p>'+body+'<div class="foot"><button class="btn" data-action="close">Cancelar</button><button class="btn primary" data-action="save">Guardar cambios</button></div></div></div>';
}
function field(label,id,value='',extra=''){return '<div class="field"><label for="'+id+'">'+h(label)+'</label><input id="'+id+'" value="'+h(value)+'" '+extra+'></div>'}
function close(){state.modal=false;state.entry=null;$('#modal').innerHTML=''}
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
 const material=get('material')||({ingrediente:'paper',alimento:'bread',bebida:'honey_bottle',plato:'beetroot_soup'}[state.template]);
 const image=$('#new-image');
 const tex=get('texture'),pack=get('pack');
 if(image){
  const letters=material.split('_').slice(0,2).map(x=>x.charAt(0).toUpperCase()).join('');
  const preview=tex&&pack?'<img alt="Vista previa del PNG" src="/api/texture?pack='+ref(pack)+'&id='+ref(tex)+'">':'';
  image.classList.toggle('missing',!preview);
  image.innerHTML=preview+'<span class="item-fallback"><b>'+h(letters)+'</b><small>'+h(material.replace(/_/g,' '))+'</small></span>';
 }
 const text=$('#preview-copy');
 if(text)text.innerHTML='<b>'+h(name)+'</b><small>Base: '+h(material)+'</small><small>'+h(state.template==='ingrediente'?'Sin consumo':(get('nutrition')||'4')+' nutrición · '+(get('saturation')||'1.2')+' saturación · '+(get('effect')||'Sin efecto'))+'</small>';
}
async function newItem(type='ingrediente'){
 if(!state.catalog.packs.length){toast('Primero creá un pack');return}
 state.mode='create';state.template=type;
 const base={ingrediente:'paper',alimento:'bread',bebida:'honey_bottle',plato:'beetroot_soup'}[type]||'paper';
 const options=state.materials.map(x=>'<option value="'+h(x.id)+'"></option>').join('');
 const isFood=type!=='ingrediente';
 const basic='<div class="field"><label>Pack de destino</label><select id="pack">'+packOptions('')+'</select></div>'
 +field('Nombre','name','', 'placeholder="Ej. Sopa de verduras"')
 +field('ID (opcional)','item-id','', 'placeholder="Se genera automáticamente"')
 +field('Material de Minecraft','material',base,'list="materials"')
 +'<datalist id="materials">'+options+'</datalist>'
 +field('Textura personalizada (opcional)','texture','', 'list="textures" placeholder="cocina:item/sopa_verduras"')
 +'<datalist id="textures"></datalist>'
 +'<div class="field"><label for="texture-file">Subir PNG al pack (máx. 500 KB)</label><input id="texture-file" type="file" accept="image/png"><small>Se guarda en el resource pack real y se selecciona automáticamente.</small></div>'
 +field('Descripción / lore','description','', 'placeholder="Descripción visible en Minecraft"')
 +field('Apilamiento máximo','stack',type==='plato'?'1':'16','type="number" min="1" max="99"');
 const consumption=isFood?
   '<h3 class="form-section">Consumo</h3><div class="fields">'
   +field('Nutrición (0–20)','nutrition',type==='bebida'?'0':'4','type="number" min="0" max="20"')
   +field('Saturación (0–20)','saturation',type==='bebida'?'0':'1.2','type="number" step="0.1" min="0" max="20"')
   +field('Tiempo para consumir (segundos)','consume-seconds','1.6','type="number" step="0.1" min="0.05" max="60"')
   +selectField('Animación al consumir','animation',[['eat','Comer'],['drink','Beber']],type==='bebida'?'drink':'eat')
   +selectField('Recipiente que queda','remainder',[['none','Ninguno'],['bowl','Cuenco'],['glass_bottle','Botella de cristal'],['bucket','Balde']],type==='plato'?'bowl':type==='bebida'?'glass_bottle':'none')
   +'<div class="field"><label class="check"><input type="checkbox" id="always-eat"> Se puede consumir con el hambre llena</label></div>'
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
 modal('Nueva plantilla · '+type,
  '<div class="create-preview"><span id="new-image" class="item-slot large missing"><b>??</b></span><span id="preview-copy"></span></div>'
  +'<h3 class="form-section">Datos principales</h3><div class="fields">'+basic+'</div>'
  +consumption
  +'<div class="note">El material es la base de Minecraft; la textura define su apariencia personalizada. Esta plantilla no crea recetas, cultivos ni estaciones de cocina automáticamente.</div>',true);
 previewNew();await loadTextures();
}
async function loadTextures(){
 const pack=get('pack');
 if(!pack)return;
 try {
  const list=await api('/api/textures?pack='+ref(pack));
  const datalist=$('#textures');
  if(datalist)datalist.innerHTML=list.map(x=>'<option value="'+h(x)+'"></option>').join('');
 }catch(e){toast('No se pudieron listar las texturas: '+e.message)}
}
async function uploadImage(file){
 if(!file || !state.modal || state.mode!=='create')return;
 if(file.size>500000)throw Error('PNG demasiado grande: máximo 500 KB');
 const bytes=new Uint8Array(await file.arrayBuffer());
 if(bytes[0]!==137 || bytes[1]!==80 || bytes[2]!==78 || bytes[3]!==71)throw Error('Seleccioná un PNG válido');
 const suggested=(get('item-id')||get('name')||file.name.replace(/\.png$/i,''))
 .normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_+|_+$/g,'').slice(0,58);
 if(!suggested)throw Error('Primero escribí el nombre de la creación');
 const resp=await fetch('/api/texture?pack='+ref(get('pack'))+'&name='+ref(suggested),{
  method:'POST',headers:{'X-VSRender-Editor':'1','Content-Type':'image/png'},body:bytes
 });
 const data=await resp.json();
 if(!resp.ok)throw Error(data.error||'No se pudo subir la textura');
 $('#texture').value=data.texture;
 await loadTextures();
 previewNew();
 toast('PNG guardado: '+data.texture);
}
async function editItem(refid){
 const entry=await api('/api/item?ref='+ref(refid));
 state.entry=entry;state.mode='edit';
 modal('Editar '+entry.id,'<div class="create-preview">'+slot(entry,true)+'<div id="preview-copy"><b>'+h(entry.name)+'</b><small>'+h(entry.material)+'</small><small>'+h(entry.file)+'</small></div></div><div class="fields">'+field('Nombre','edit-name',entry.name)+field('Material','edit-material',entry.material)+'</div><div class="field full"><label>YAML avanzado</label><textarea id="edit-yaml">'+h(entry.yaml)+'</textarea></div><div class="note">Se conservarán las otras definiciones del archivo. Se crea un respaldo automático antes de guardar.</div><div class="foot"><button class="btn small" data-action="export-current">Exportar ZIP</button><button class="btn small" data-action="duplicate">Duplicar</button><button class="btn danger small" data-action="delete">Eliminar</button></div>',true);
}
function askPack(){state.mode='pack';modal('Nuevo pack','<div class="fields">'+field('Nombre','name','', 'placeholder="cocina"')+field('Namespace','namespace','', 'placeholder="cocina"')+'</div>')}
function askImport(){state.mode='import';modal('Importar ítem','<div class="fields"><div class="field"><label>Pack destino</label><select id="pack">'+packOptions('')+'</select></div><div class="field"><label>Archivo YAML/ZIP</label><input type="file" id="file" accept=".yml,.yaml,.zip"></div></div><div class="note">No sobrescribe ítems existentes. No se instalan scripts de terceros.</div>')}
function askDuplicate(){state.mode='duplicate';state.backupEntry=state.entry;modal('Duplicar '+state.entry.id,'<div class="fields"><div class="field"><label>Pack</label><select id="pack">'+packOptions(state.entry.pack)+'</select></div>'+field('Namespace','namespace',state.entry.id.split(':')[0])+field('ID','item-id',state.entry.id.split(':')[1]+'_copia')+'</div>')}
async function save(){
 if(state.mode==='pack'){
  const p=await api('/api/packs','POST',{name:get('name'),namespace:get('namespace')});close();await refresh();toast('Pack creado: '+p.name);return;
 }
 if(state.mode==='create'){
  const pack=get('pack');const p=state.catalog.packs.find(p=>p.name===pack);
  const body={pack,namespace:p?.namespace,name:get('name'),id:get('item-id'),material:get('material'),template:state.template,
   max_stack_size:get('stack'),description:get('description'),texture:get('texture'),
   nutrition:get('nutrition'),saturation:get('saturation'),consume_seconds:get('consume-seconds'),
   animation:get('animation'),remainder:get('remainder'),always_eat:$('#always-eat')?.checked||false,
   effect:get('effect'),effect_level:get('effect-level'),effect_seconds:get('effect-seconds'),effect_chance:get('effect-chance'),
   effect2:get('effect2'),effect2_level:get('effect2-level'),effect2_seconds:get('effect2-seconds'),effect2_chance:get('effect2-chance')};
  const result=await api(state.template==='ingrediente'?'/api/create':'/api/template','POST',body);
  close();await refresh();toast('Creado: '+result.id);await editItem(result.ref);return;
 }
 if(state.mode==='edit'){
  const e=state.entry,raw=$('#edit-yaml').value;
  const changed=raw.trimEnd()!==e.yaml.trimEnd();
  const body=changed?{revision:e.revision,yaml:raw}:{revision:e.revision,name:get('edit-name'),material:get('edit-material')};
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
 const el=ev.target.closest('[data-page],[data-action],[data-open],[data-export],[data-template],[data-doc],[data-view]');
 if(!el)return;
 attempt(async()=>{
 if(el.dataset.page){state.page=el.dataset.page;render();return}
 if(el.dataset.view){state.view=el.dataset.view;render();return}
 if(el.dataset.doc){state.doc=el.dataset.doc;await renderDocs();return}
 if(el.dataset.open){await editItem(el.dataset.open);return}
 if(el.dataset.export){location.href='/api/export?ref='+ref(el.dataset.export);return}
 if(el.dataset.template){await newItem(el.dataset.template);return}
 switch(el.dataset.action){
 case 'new-item':await newItem();break;
 case 'new-pack':askPack();break;
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
 if(ev.target.id==='pack-filter'){state.pack=ev.target.value;render()}
 if(ev.target.id==='kind-filter'){state.kind=ev.target.value;render()}
 if(ev.target.id==='pack' && state.mode==='create'){attempt(loadTextures);previewNew()}
 if(ev.target.id==='texture-file'){attempt(async()=>uploadImage(ev.target.files?.[0]))}
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