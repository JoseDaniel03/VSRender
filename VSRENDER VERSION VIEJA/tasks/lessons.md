# Lessons

- Un callback de `EntityScheduler.execute()` no puede ser el único owner de finalización de una operación asíncrona: durante disable Canvas/Folia puede suprimir tanto la ejecución como el callback de retirement.
- Todo future expuesto por VsRender debe tener un owner de lifecycle independiente del scheduler y una ruta explícita de cierre.
- No exponer controles de lifecycle (`shutdown`) en un API de gameplay público sólo para resolver ownership interno; preferir un contrato interno/estándar como `AutoCloseable` y cerrar desde el lifecycle propietario.
- Si cerrar un service puede desregistrarlo, iterar una snapshot de las registraciones para no depender de la mutabilidad de la colección del ServicesManager.
- Si el toolchain real no está disponible, separar claramente “fuente validada por harness” de “artefacto ejecutable recompilado”; nunca reciclar un JAR anterior como si incluyera el fix.
- Antes de empaquetar una migración sobre un fork, comparar contra el último checkpoint realmente entregado, no contra un árbol intermedio; un árbol de trabajo posterior puede haber perdido fixes aunque parezca más nuevo.
- Cuando un adapter implemente varios contratos de provider con firmas coincidentes (default + abstract), declarar el método de identidad explícitamente y compilar el adapter completo, no sólo cada contrato aislado.

- Las capabilities runtime opcionales deben colgar de `RenderInstance` con defaults vacíos; así una migración de renderer no obliga a romper todas las implementaciones existentes.
- Cuando un provider externo todavía posee el backend físico, concentrar sus llamadas (`animate`, `stop`, tracker creation) en un adapter único antes de reemplazar el backend; primero se mueve ownership/API, después implementación.
- No meter profile/skin/tint provider-specific en un `Map<String,Object>` para acelerar una factory común: definir contratos nativos explícitos antes de migrar furniture.

- Antes de afirmar que una fusión tiene ambos codebases localmente, verificar físicamente si el árbol contiene la fuente o sólo una dependencia API/compileOnly; mover contracts no equivale a haber absorbido el backend.
- `BaseEntity.passengerPosition()` de BetterModel representa el `EntityAttachment.PASSENGER` del owner, no un booleano implícito de “entidad montada”; verificar la semántica upstream antes de convertir una propiedad geométrica en un guard de fallback.

## 2026-10-05 — Playback ownership y eventos transitorios
- Un `stop()` que publica rest pose de forma diferida debe ser cancelable por un `play()` posterior; de lo contrario el rest frame puede pisar el primer tick de la nueva animación.
- No modelar `damage/jump/death` como una sola categoría: `damage/jump` full-rig `override=false` son interrupts reproducibles, mientras `death` agrega lifecycle terminal (`forRemoval` + close) y requiere un contrato separado.
- Al reemplazar una priority stack externa por un runtime simple, migrar sólo los casos cuya semántica de preemption/restart esté demostrada por tests; el resto conserva fallback completo.

## 2026-10-06 — Terminal lifecycle y scheduler retirement
- No usar el callback `retired()` como atajo cuando el propio tick observa `owner.isAlive()==false` o `owner.isValid()==false`: en ese caso el repeating task puede seguir registrado. La ruta observada por el host debe ejecutar `close()` y cancelar su handle; `retired()` queda reservado al callback real del `EntityScheduler`.
- Un lifecycle terminal que debe completar aun sin viewers necesita un avance culled explícito y opt-in; no debilitar el culling normal sólo para resolver death.
- `override=true` de BetterModel no significa “sólo bones con track”: una vez que un bone hace match, el override se propaga a sus descendientes con iteradores vacíos. La máscara nativa correcta es subtree desde cada track root.
- La prioridad BetterModel es prioridad descendente y, a igualdad, inserción más reciente primero. Preservar esa regla en cualquier layer stack nativo.

## 2026-10-07 — Repositorio de trabajo de VsRender
- Cuando el usuario pida subir VsRender para usar el runner, el destino es `DaniDevz/VirgolandStudio`, no `DaniDevz/VSEngine---CanvasMC`. Verificar el repo explícito antes de preparar ramas/workflows.

## 2026-10-07 — Importación exacta y gates reales
- Un árbol reconstruido byte-a-byte antes del commit no garantiza que el árbol versionado sea igual: un `.gitignore` anidado puede excluir fuentes válidas (por ejemplo rutas llamadas `server/`). Para imports vendorizados, usar `git add -f` cuando corresponda y verificar el árbol **trackeado** después del commit/checkout.
- Si un test real descubre que una pose ya resuelta se publica de forma diferida, no volver a resolverla sólo por haber acumulado delta de scheduler: una segunda resolución puede limpiar dirty bits antes de publicar. Las publicaciones forzadas son boundaries de ownership y deben preservar el pose actual.
- Un backend físico de hitbox no puede usar una entidad `Interaction` con `noPhysics` como sustituto de un cuerpo pushable. La física owner-aware vive en un backend versionado NMS; click/attack/projectile permanecen en la capa packet/exact-AABB para evitar doble impacto.
- Durante una serie de commits mecánicos en una rama con runner único, pausar el trigger por push y reactivarlo una sola vez al final evita una cola de CI obsoleta; el workflow final debe usar `concurrency` con cancelación de runs superseded.

## 2026-10-07 — Delivery con cuota de Actions
- No hacer que una cuota externa de `actions/upload-artifact` invalide un build y smoke ya verificados si existe un canal de entrega independiente.
- El artifact autoritativo debe poder empaquetarse localmente en el runner y publicarse como GitHub Release asset; Actions artifact puede ser un espejo best-effort.
- Diferenciar explícitamente fallo de runtime/build de fallo de almacenamiento externo para evitar falsos negativos y, a la vez, no afirmar que existe un Actions artifact cuando la cuota lo rechazó.
