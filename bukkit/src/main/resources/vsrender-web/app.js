'use strict';
const state={page:'items',catalog:{items:[],packs:[],errors:[]},materials:[],templates:[],docs:[],doc:'introduccion',search:'',pack:'all',kind:'all',modal:false,mode:'',entry:null};
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
function renderItems(){
 const items=state.catalog.items,filtered=items.filter(x=>(state.pack==='all'||x.pack===state.pack)&&(state.kind==='all'||x.type===state.kind)&&(x.id+' '+x.name+' '+x.material).toLowerCase().includes(state.search.toLowerCase()));
 const select='<select id="pack-filter"><option value="all">Todos los packs</option>'+packOptions(state.pack)+'</select>';
 const filter='<select id="kind-filter">'+[['all','Todos'],['items','Ítems'],['blocks','Bloques'],['furniture','Muebles']].map(x=>'<option value="'+x[0]+'" '+(state.kind===x[0]?'selected':'')+'>'+x[1]+'</option>').join('')+'</select>';
 const rows=filtered.map(x=>'<tr><td><img class="thumb" src="/api/thumb?ref='+ref(x.ref)+'" alt=""><span class="entry">'+h(x.name)+'<small>'+h(x.id)+'</small></span></td><td>'+h(x.type)+'</td><td>'+h(x.pack)+'</td><td><span class="mono">'+h(x.material)+'</span></td><td><button class="btn small" data-open="'+h(x.ref)+'">Editar</button> <button class="btn small" data-export="'+h(x.ref)+'">↓ ZIP</button></td></tr>').join('');
 return heading('Creaciones','Ítems, bloques y muebles en los YAML del servidor.',button('import','↥ Importar')+' '+button('new-item','＋ Crear ítem','primary'))
 +'<div class="stats">'+count('Creaciones',items.length)+count('Ítems',items.filter(x=>x.type==='items').length)+count('Bloques',items.filter(x=>x.type==='blocks').length)+count('Muebles',items.filter(x=>x.type==='furniture').length)+'</div>'
 +'<div class="panel"><div class="filter"><input id="search" placeholder="Buscar por nombre o ID…" value="'+h(state.search)+'">'+select+filter+'</div>'
 +(rows?'<div style="overflow-x:auto"><table><thead><tr><th>Creación</th><th>Tipo</th><th>Pack</th><th>Material</th><th></th></tr></thead><tbody>'+rows+'</tbody></table></div>':'<div class="empty">Sin elementos. Creá tu primer ítem o seleccioná otro pack.</div>')+'</div>'
 +'<div class="note">Los YAML se actualizan inmediatamente. Para aplicarlos en Minecraft pulsá «Aplicar al juego»; para texturas y modelos también puede ser necesario regenerar el resource pack.</div>'
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
async function refresh(show=false){try{state.catalog=await api('/api/catalog');$('#count').textContent=state.catalog.items.length;if(!state.modal)render();if(show)toast('Catálogo actualizado')}catch(e){toast(e.message);$('#page').innerHTML='<p class="error">'+h(e.message)+'</p>'}}
function modal(title,body,wide=false){
 state.modal=true;
 $('#modal').innerHTML='<div class="overlay"><div class="dialog '+(wide?'wide':'')+'" role="dialog" aria-modal="true"><h2>'+h(title)+'</h2><p>Guardá los datos en el pack seleccionado.</p>'+body+'<div class="foot"><button class="btn" data-action="close">Cancelar</button><button class="btn primary" data-action="save">Guardar cambios</button></div></div></div>';
}
function field(label,id,value='',extra=''){return '<div class="field"><label for="'+id+'">'+h(label)+'</label><input id="'+id+'" value="'+h(value)+'" '+extra+'></div>'}
function close(){state.modal=false;state.entry=null;$('#modal').innerHTML=''}
async function newItem(type='ingrediente'){
 if(!state.catalog.packs.length){toast('Primero creá un pack');return}
 state.mode='create';state.template=type;
 const base={ingrediente:'paper',alimento:'apple',bebida:'honey_bottle',plato:'beetroot_soup'}[type]||'paper';
 const options=state.materials.map(x=>'<option value="'+h(x.id)+'"></option>').join('');
 const extras=type==='ingrediente'?'':field('Nutrición','nutrition','4','type="number" min="0" max="20"')+field('Saturación','saturation','1.2','type="number" step="0.1" min="0" max="20"');
 modal('Crear '+type,'<div class="fields"><div class="field"><label>Pack</label><select id="pack">'+packOptions('')+'</select></div>'+field('Nombre','name','', 'placeholder="Ej. Tomate"')+field('ID (opcional)','item-id','', 'placeholder="Automático"')+field('Material vanilla','material',base,'list="materials"')+'<datalist id="materials">'+options+'</datalist>'+extras+field('Apilamiento','stack',type==='plato'?'1':'64','type="number" min="1" max="99"')+'</div>');
}
async function editItem(refid){
 const entry=await api('/api/item?ref='+ref(refid));
 state.entry=entry;state.mode='edit';
 modal('Editar '+entry.id,'<div class="fields">'+field('Nombre','edit-name',entry.name)+field('Material','edit-material',entry.material)+'</div><div class="field full"><label>YAML avanzado</label><textarea id="edit-yaml">'+h(entry.yaml)+'</textarea></div><div class="note">Se conservarán las otras definiciones del archivo. Se crea un respaldo automático antes de guardar.</div><div class="foot"><button class="btn small" data-action="export-current">Exportar ZIP</button><button class="btn small" data-action="duplicate">Duplicar</button><button class="btn danger small" data-action="delete">Eliminar</button></div>',true);
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
  const body={pack,namespace:p?.namespace,name:get('name'),id:get('item-id'),material:get('material'),template:state.template,max_stack_size:get('stack'),nutrition:get('nutrition'),saturation:get('saturation')};
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
 const el=ev.target.closest('[data-page],[data-action],[data-open],[data-export],[data-template],[data-doc]');
 if(!el)return;
 attempt(async()=>{
 if(el.dataset.page){state.page=el.dataset.page;render();return}
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
});
document.addEventListener('change',ev=>{
 if(ev.target.id==='pack-filter'){state.pack=ev.target.value;render()}
 if(ev.target.id==='kind-filter'){state.kind=ev.target.value;render()}
});
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