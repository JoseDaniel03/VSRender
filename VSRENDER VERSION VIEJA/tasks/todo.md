# VsRender — fusión BetterModel + CraftEngine

## Milestone actual — Native Model Foundation

### Especificación
El objetivo de este bloque es dejar de resolver modelos exclusivamente a través de la capa `CompatibilityManager -> ModelProvider -> ExternalModel` heredada de CraftEngine, sin romper furniture ni los providers existentes.

La migración debe cumplir estas invariantes:
1. VsRender tendrá contratos propios para modelo y provider dentro de Core.
2. La resolución de modelos tendrá un registry explícito, determinista y testeable.
3. Los providers legacy seguirán funcionando mediante bridges; no se reescribe BetterModel/CraftEngine de golpe.
4. La furniture existente mantendrá compatibilidad con providers legacy, pero resolverá y bindeará a través de `RenderModel`.
5. El renderer físico, rig/bones/attachments siguen externos; sólo identidad y lifecycle de animación pueden pasar a contratos nativos en este checkpoint.
6. No se toca Narrative ni VirgolandStudio.

### Plan
- [x] Cerrar lifecycle de futures de animación (`PendingFutureRegistry`) y validar carrera registro/shutdown.
- [x] Introducir `RenderModel`, `RenderModelProvider`, `RenderModelReference` y `RenderModelRegistry` en Core.
- [x] Convertir `ExternalModel` y `ModelProvider` en bridges compatibles hacia los contratos nativos.
- [x] Delegar la resolución de modelos de `BukkitCompatibilityManager` al registry nativo sin cambiar el comportamiento de furniture.
- [x] Adaptar BetterModel y ModelEngine providers a través del bridge, sin duplicar lógica.
- [x] Añadir pruebas de resolución: orden, provider explícito, reemplazo y ausencia.
- [x] Compilar/validar las clases Core nuevas con el toolchain disponible y revisar el diff por impacto mínimo.
- [x] Preparar checkpoint de fuentes y documentar qué gate de build completo sigue bloqueado.

## Siguiente milestone — Native Rig / Animation
- [x] Introducir lifecycle viewer-facing `RenderInstance` y adaptar trackers estáticos BetterModel de block/furniture.
- [x] Introducir `RenderPartSelector` provider-independent y mover la selección de tint bones al borde BetterModel.
- [x] Definir `RenderRig`, `RenderPart`, `RenderRigReference`, `RenderRigProvider` y registry provider-independent.
- [x] Exponer snapshots de rigs BetterModel (MODEL/PLAYER) con jerarquía y catálogo de animaciones sin tipos BetterModel en Core.
- [x] Representar locator/attachment point como `RenderPartKind.LOCATOR` y exponerlo desde `RenderRig` sin tipos BetterModel en Core.
- [x] Definir pose/binding runtime provider-independent del attachment point sin filtrar BetterModel a Core.
- [ ] Añadir executor de attachment que aplique continuamente el pose a un recurso hijo/móvil; el binding actual expone snapshot local y `apply`, no simula un attachment físico completo.
- [x] Definir identidad/modo/sesión de animación nativos sobre lifecycle ya existente.
- [ ] Hacer que BetterModel sea adapter de esos contratos, no runtime físico dueño. (Muy avanzado: model/rig metadata, instance factory, attachment binding, part selector y animation controller/lifecycle ya son VsRender; el tracker/render pipeline físico sigue BetterModel.)
- [~] Migrar player/entity animation al runtime nativo progresivamente. Player ya usa `RenderAnimationController`; entity runtime queda pendiente.

## Criterio de salida de la fusión
VsRender debe poder arrancar y renderizar bloque/furniture/item/entity/player animation sin BetterModel ni CraftEngine instalados como plugins externos. Los adapters de compatibilidad deben poder retirarse sin romper las APIs nativas.

## Estado de verificación previo
- [x] Harness local de `PendingFutureRegistry`: 5.000 carreras `register()`/`close()` sin fugas.
- [ ] Build Gradle completo: bloqueado porque Gradle 9.7.1 no está cacheado y el entorno no resuelve `services.gradle.org`.
- [ ] Smoke CanvasMC 26.2 real.

## Revisión
- Native Model Foundation implementada en fuentes.
- Furniture consume `RenderModel`; los contratos legacy quedaron como bridges.
- BetterModel player animation ya entrega un `RenderAnimationSession` nativo y usa `RenderAnimationKey`/`RenderAnimationMode`.
- Harness de registry/bridge: PASS.
- Harness de animation lifecycle: PASS.
- Harness de `RenderInstance`: PASS (`show/hide/refresh/close`).
- Harness de `RenderPartSelector`: PASS; selección ALL/NONE/NAMED y política de children quedan en Core.
- Harness de `RenderRig`/`RenderRigRegistry`: PASS; topología, cycles, provider legacy/explícito, catálogo de animaciones y locator attachment points.
- Harness del adapter BetterModel: PASS; `DummyTracker` queda detrás de `RenderInstance`, `ModelRenderer` se traduce a snapshot `RenderRig` y `BlueprintElement.Locator` queda contenido en el borde del adapter.
- Harness del `BetterModelProvider`: PASS; implementa explícitamente contratos nativos de model + rig sin conflicto de interfaces.
- Harness de `PendingAnimationResults` sobre `PendingFutureRegistry`: PASS, incluyendo carrera register/close y late completion.
- Diff contra el checkpoint anterior revisado: se detectó y corrigió una regresión de lifecycle antes de empaquetar; se preservan `AutoCloseable`, cierre propietario desde `BukkitCraftEngine` y manejo de excepción al agendar.
- Build Gradle/JAR y smoke CanvasMC siguen pendientes por toolchain externo, por lo que no se declara release verde.
- Boundary check: PASS; Core tiene 0 imports `kr.toxicity`, y `spawn/remove` BetterModel queda sólo en `BetterModelRenderInstance`.
- Diff final contra el checkpoint de shutdown: 0 archivos eliminados.

## Milestone en curso — Native Attachment + Animation Runtime

### Especificación
Este bloque mueve ownership runtime adicional desde BetterModel hacia contratos de VsRender, sin reimplementar todavía el renderer físico de BetterModel.

Invariantes:
1. Core no importa `kr.toxicity`, Bukkit ni tipos de plataforma para describir attachments o animaciones.
2. Una instancia concreta puede exponer opcionalmente parts/attachments y animation control; callers no conocen el tracker proveedor.
3. El transform de attachment es un snapshot inmutable y explícito; no se inventa escala si el provider no puede exponerla correctamente.
4. El controller de animación crea `RenderAnimationSession` nativas y el owner (`AnimationSessionBook`/caller) conserva el lifecycle.
5. BetterModel traduce `DummyTracker`/`RenderedBone` sólo dentro del adapter.
6. El fix de futures/shutdown y todos los gates del milestone anterior deben seguir verdes.

### Plan
- [x] Añadir transform/binding provider-independent de attachment/part en Core.
- [x] Extender `RenderInstance` con capabilities opcionales y compatibles hacia atrás.
- [x] Añadir `RenderAnimationController` instance-scoped sobre `RenderAnimationSession`.
- [x] Implementar attachment snapshots y animation controller en `BetterModelRenderInstance`.
- [x] Añadir harness/tests de lifecycle, lookup de locator, transforms y animation sessions.
- [x] Reejecutar todos los harnesses anteriores y boundary checks.
- [x] Actualizar revisión y empaquetar checkpoint de fuentes sin JAR preview falso.


### Extensión del milestone — Native Instance Factory
- [x] Añadir `RenderInstanceRequest`, `RenderInstanceProvider` y `RenderInstanceRegistry`.
- [x] Registrar factories de instancia desde `CompatibilityManager` cuando un provider soporta el contrato.
- [x] Hacer que `BetterModelProvider` materialice instancias estáticas desde un request nativo.
- [x] Migrar `BetterModelBlockEntityElement` al registry; ya no importa ni llama BetterModel/ModelRenderer/DummyTracker.
- [ ] Migrar furniture al factory nativo después de definir contracts propios de profile/skin + tint; no esconderlos en opciones opacas.

### Revisión de este bloque
- `RenderAttachmentBinding` expone snapshots `RenderPartTransform` explícitamente `INSTANCE_LOCAL`; no inventa escala no disponible.
- `RenderAnimationController` concentra las únicas llamadas físicas `tracker.animate/stopAnimation` del adapter BetterModel.
- `BetterModelPlayerAnimations` usa el controller nativo y conserva `AnimationSessionBook`, request guard y pending-result shutdown.
- `RenderInstance` mantiene compatibilidad binaria/fuente para implementaciones actuales mediante capabilities default vacías.
- `BetterModelBlockEntityElement` usa `RenderInstanceRequest -> RenderInstanceRegistry -> BetterModelProvider`; 0 imports BetterModel en ese elemento.
- Harness attachment + animation runtime: PASS.
- Harness player animation `INVALID_RESOURCE -> STARTED -> STOPPED -> shutdown -> UNAVAILABLE`: PASS.
- Harness RenderInstanceRegistry explícito/fallback: PASS.
- Harness BetterModelProvider + block element compile-shape: PASS.
- Regresiones previas pending/shutdown, AnimationSessionBook, model registry, part selector y rig registry: PASS.
- Boundary check: 0 imports `kr.toxicity` en Core; animate/stop físico sólo en `BetterModelAnimationController`; 0 archivos eliminados contra checkpoint anterior.
- Gradle/JAR completo: BLOQUEADO por `UnknownHostException: services.gradle.org` al intentar obtener Gradle 9.7.1.
- Smoke CanvasMC real: pendiente; no se declara release verde.

## Milestone en curso — Native Profile / Tint + Furniture Factory

### Especificación
El objetivo es sacar la creación de furniture del API directo de BetterModel sin esconder datos provider-specific en mapas opacos.

Invariantes:
1. Core define una representación inmutable de profile/skin que pueda transportar UUID/nombre/textura embebida sin importar BetterModel.
2. Core define tint layers como `Color + RenderPartSelector`; la selección de bones continúa provider-independent.
3. `RenderInstanceRequest` transporta opciones explícitas (sight trace, profile, tint) y mantiene un constructor compatible para callers simples.
4. `BetterModelProvider` es el único lugar que convierte profile/tint nativos a `ModelProfile`/`TrackerUpdateAction`.
5. `BetterModelFurnitureElement` no importa `kr.toxicity`, `DummyTracker`, `ModelRenderer`, `BonePredicate`, `BukkitAdapter` ni `org.bukkit.Location`.
6. Los formatos moderno/legacy de profile source conservan el comportamiento previo; la lectura NBT se mueve a un datasource nativo.
7. No se modifica el gameplay de furniture ni el contrato de show/hide/activate/deactivate.
8. Todos los gates de lifecycle, animation, rig e instance factory anteriores deben seguir verdes.

### Plan
- [x] Añadir `RenderProfile`, `RenderTintLayer` y `RenderInstanceOptions` en Core.
- [x] Extender `RenderInstanceRequest` manteniendo el constructor simple existente.
- [x] Mover la lectura de profile del source item a un `FurnitureDataSourceConfig<RenderProfile>` nativo.
- [x] Adaptar profile/tint en `BetterModelProvider` durante la creación física de tracker.
- [x] Migrar `BetterModelFurnitureElement` al `RenderInstanceRegistry` y eliminar sus imports BetterModel/Bukkit de creación.
- [x] Añadir tests/harnesses para invariantes de profile/options y compile-shape provider+furniture.
- [x] Reejecutar regresiones/boundary checks y documentar el resultado.

### Revisión — Native Profile / Tint + Furniture Factory
- `RenderProfile` conserva UUID/nombre/texturas sin tipos BetterModel; UUID cero se normaliza como identidad desconocida.
- `SourceItemProfileDataSourceConfig` vive en Core y conserva parsing moderno + SkullOwner legacy.
- `RenderInstanceOptions` snapshottea profile/tints explícitos; el constructor simple de `RenderInstanceRequest` sigue disponible.
- `BetterModelProfileAdapter` concentra la traducción a `ModelProfile.Uncompleted` y preserva fallback de lookup.
- `BetterModelFurnitureElement` tiene 0 refs/imports BetterModel/Bukkit para creación y usa el registry nativo.
- Harness profile/options: PASS.
- Harness source-item profile moderno/legacy: PASS.
- Harness RenderProfile -> BetterModel profile adapter: PASS.
- Compile-shape BetterModelProvider + Furniture: PASS.
- RenderInstanceRegistry revalidado con request extendido: PASS.
- Core sigue con 0 imports `kr.toxicity`; shutdown/animation ownership previo intacto.

## Siguiente extensión — Attachment Executor nativo

### Especificación
Añadir un executor provider-independent que conecte un `RenderAttachmentBinding` con un target de transform sin imponer scheduler de plataforma ni asumir una composición WORLD incorrecta.

Invariantes:
1. El executor es tick-driven: el owner de plataforma decide cuándo ejecutarlo; Core no crea threads.
2. El target declara/decide qué `RenderTransformSpace` soporta; el executor no convierte INSTANCE_LOCAL a WORLD inventando matemáticas/provider state.
3. El executor posee y cierra el binding, pero no toma ownership implícito del recurso target.
4. Close y tick deben ser idempotentes/race-safe.
5. La falta temporal de snapshot no cierra el binding ni aplica un transform viejo.

### Plan
- [x] Añadir `RenderAttachmentTarget` y `RenderAttachmentExecutor`.
- [x] Añadir tests de tick, ausencia de snapshot, rechazo por space, close y carreras simples.
- [ ] Exponer capability de transform target desde `RenderInstance` sólo cuando haya implementación física correcta; no inventarla para BetterModel todavía.
- [x] Documentar que executor Core está listo pero el target físico child-instance sigue pendiente.

## Milestone en curso — Native Animation Data / Sampler v1

### Especificación
Absorber el primer bloque de backend lógico de BetterModel: clips, tracks, frames e interpolación básica quedan representados y sampleados por VsRender Core. BetterModel continúa como backend físico/importador durante la transición.

Invariantes:
1. Los tipos nativos no importan BetterModel ni clases Bukkit.
2. Un clip es inmutable y valida duración, IDs, orden/duplicados de tracks y frames.
3. Cada track produce un `RenderBonePose` con translation, scale y rotation Euler en grados.
4. Sampler v1 soporta STEP y LINEAR de forma determinista; no se declara soporte Bezier/Catmull hasta implementarlos correctamente.
5. El sampling antes del primer frame y después del último es explícito y testeado.
6. Scale se modela como escala absoluta/multiplicativa de pose (`1,1,1` = identidad), evitando la codificación interna `delta+1` de BetterModel.
7. Este milestone no reemplaza todavía packets/displays/tracker; construye el estado de animación que el renderer nativo consumirá.

### Plan
- [x] Añadir `RenderVec3`, `RenderBonePose`, `RenderInterpolation`, `RenderAnimationFrame`, `RenderAnimationTrack`, `RenderAnimationClip`.
- [x] Implementar `RenderAnimationSampler` para STEP/LINEAR y clamping temporal.
- [x] Añadir tests/harness de validación, sampling y aislamiento de colecciones.
- [x] Implementar y documentar mapping BetterModel `BlueprintAnimation` -> clip nativo, sin copiar el runtime físico.

### Revisión — Attachment Executor + Native Animation Data v1
- `RenderAttachmentExecutor` es tick-driven, no crea threads y no convierte espacios de coordenadas implícitamente.
- Harness attachment executor lifecycle/space safety: PASS.
- `RenderAnimationClip/Track/Frame/BonePose/Vec3` forman el primer backend lógico de animación propio de VsRender.
- `RenderAnimationSampler` soporta STEP/LINEAR, clamp ONCE/HOLD y wrap LOOP; no declara Bezier/Catmull todavía.
- `RenderAnimationMode` ahora conserva ONCE/LOOP/HOLD.
- `BetterModelAnimationClipAdapter` traduce el `BlueprintAnimation` procesado a clips nativos preservando pose, scale, Euler rotation, skipInterpolation y globalRotation.
- `AnimatedRenderModel` expone biblioteca de clips; `BetterModelModel` ya almacena clips VsRender, no sólo nombres.
- Harness native sampler: PASS.
- Harness BetterModel BlueprintAnimation -> RenderAnimationClip: PASS.
- Harness runtime BetterModel ONCE/LOOP/HOLD: PASS.
- Compile-shape Provider + Furniture después de clip library: PASS.
- El target físico de child attachment sigue pendiente por decisión de seguridad: no se inventa composición INSTANCE_LOCAL -> WORLD.

## Extensión — Native Animation Playback Clock

### Especificación
Crear un playback state tick-driven sobre `RenderAnimationClip` para que VsRender sea owner del tiempo de animación antes de reemplazar el renderer físico.

Invariantes:
1. No crea scheduler/threads; recibe delta seconds del owner.
2. ONCE clampa al final y queda `completed`.
3. LOOP envuelve tiempo y nunca queda completed por alcanzar el final.
4. HOLD clampa al final, marca `holding` y conserva la última pose sin completar automáticamente.
5. Delta negativo/no finito se rechaza.
6. Reset vuelve exactamente al estado inicial.
7. Sampling usa exclusivamente `RenderAnimationSampler`.

### Plan
- [x] Añadir `RenderAnimationPlayback`.
- [x] Añadir tests/harness ONCE/LOOP/HOLD/zero-length/reset/invalid delta.
- [x] Documentar este clock como backend lógico, no `RenderAnimationSession` físico todavía.

### Revisión — Native Animation Playback Clock
- `RenderAnimationPlayback` es el único owner del tiempo del clip; no crea threads/schedulers.
- `RenderAnimationPlayer` compone playback + `RenderAnimationPoseTarget` sin duplicar el clock ni tomar ownership del target.
- ONCE reintenta la pose final si el target la rechaza; LOOP envuelve; HOLD aplica y conserva la pose final.
- `RenderAnimationSample` preserva `globalRotation`, que antes se perdía al reducir el sample a `RenderBonePose`.
- `RenderAnimationSampler` mantiene orden de tracks determinista mediante `LinkedHashMap` inmutable.
- Harness playback + player: PASS.
- Compile-shape `BetterModelAnimationClipAdapter` contra firmas 3.5.0: PASS.

## Milestone en curso — Native Rest Pose + Rig Pose Resolver

### Especificación
Mover a VsRender Core la composición matemática esencial de rig que todavía posee `RenderedBone`, antes de construir el renderer físico de packets.

Invariantes:
1. `RenderPart` conserva una rest pose provider-independent; constructores legacy siguen usando identidad.
2. BetterModel traduce `RendererGroup` a rest pose local durante el snapshot del rig; Core no importa BetterModel/JOML.
3. La rotación usa quaternion ZYX compatible con la semántica `MathUtil.toQuaternion` de BetterModel.
4. La jerarquía compone posición, escala y rotación en orden determinista; `globalRotation` evita heredar la rotación del parent pero no rompe la posición jerárquica.
5. El resolver no crea threads, packets ni entidades; produce snapshots inmutables para el futuro renderer nativo.
6. Parts sin track animado usan pose de animación identidad.
7. Los milestones de attachment/animation/shutdown anteriores deben seguir verdes.

### Plan
- [x] Añadir quaternion/pose resuelta provider-independent.
- [x] Extender `RenderPart` con rest pose compatible hacia atrás.
- [x] Implementar `RenderRigPoseResolver` con jerarquía parent/child.
- [x] Adaptar snapshots BetterModel para publicar rest pose local.
- [x] Añadir harness de rotación ZYX, parent scale, globalRotation y parts sin track.
- [x] Reejecutar gates de animation playback/adapter y boundary check.

### Revisión — Native Rest Pose + Rig Pose Resolver
- `RenderQuat` implementa quaternion inmutable ZYX compatible con la conversión usada por BetterModel.
- `RenderPart.restPose` captura translation/scale/rotation base; constructores previos conservan identidad.
- `BetterModelUtils.renderRig` convierte posiciones absolutas de `RendererGroup` a rest translation local parent-relative y preserva rotación base.
- `RenderRigPoseResolver` compone rest + animation y resuelve parent/child en instance space sin `RenderedBone`.
- La composición replica el orden BetterModel relevante: rotar translation por parent rotation, después aplicar parent scale y finalmente sumar parent translation.
- `globalRotation` evita heredar parent rotation, pero la posición sigue la jerarquía.
- Harness native rig pose resolver: PASS.
- Compile-shape snapshot BetterModel rest-pose: PASS.
- Boundary check: Core mantiene 0 imports `kr.toxicity`; no contiene `RenderedBone`.

## Milestone en curso — Compiled Asset + Indexed Runtime

### Especificación
Compilar los contratos descriptivos de VsRender a una representación runtime compacta antes de construir el renderer físico nativo.

Invariantes:
1. Los nombres (`partId`, `animationId`) se resuelven al compilar/cargar o al iniciar una animación; el hot path por tick no usa `Map<String,...>`.
2. El rig runtime usa índices enteros estables, orden topológico parent-before-child y buffers densos primitivos.
3. Rest pose y rest quaternion se precomputan una vez; no se reconstruyen objetos por bone en cada tick.
4. Los clips compilados apuntan directamente a `partIndex`; tracks inexistentes no crean entradas runtime.
5. Sampling escribe en un `RenderAnimationBuffer` reutilizable; resolver escribe en un `RenderPoseBuffer` reutilizable.
6. `RenderPoseBuffer` mantiene dirty mask comparando contra la pose anterior con epsilon; un renderer puede iterar sólo parts modificadas.
7. La representación pública/descriptiva (`RenderRig`, `RenderAnimationClip`) sigue compatible y actúa como formato de authoring/importación.
8. Core mantiene cero imports BetterModel/Bukkit y el nuevo runtime no crea threads ni packets.
9. La salida indexada debe ser matemáticamente equivalente al resolver descriptivo existente para los casos cubiertos.

### Plan
- [x] Añadir `CompiledRenderAsset` e `IndexedRenderRig` con IDs estables y topología precomputada.
- [x] Añadir clips/tracks indexados con arrays primitivos y validación contra el rig.
- [x] Añadir `RenderAnimationBuffer` reutilizable y sampler indexado sin mapas por tick.
- [x] Añadir `RenderPoseBuffer` denso con dirty mask y acceso por índice.
- [x] Añadir resolver indexado parent-before-child reutilizando buffers.
- [x] Añadir estado runtime por instancia (`CompiledRenderState`) con lookup de animación sólo al hacer `play`, clock compacto y salida dirty.
- [x] Añadir pruebas de equivalencia con `RenderRigPoseResolver`, dirty mask, invalidación y determinismo de índices.
- [x] Ejecutar regresiones/boundary checks y documentar resultados.
- [x] Empaquetar checkpoint de fuentes sin presentar un JAR viejo como build nuevo.


### Revisión — Compiled Asset + Indexed Runtime
- `RenderRig`/`RenderAnimationClip` quedan como contratos descriptivos/importables; `CompiledRenderAsset` los convierte una sola vez al formato de ejecución.
- `IndexedRenderRig` asigna índices enteros parent-before-child de forma determinista y precomputa rest pose + rest quaternion.
- `IndexedAnimationClip` almacena sólo tracks existentes; no reserva un array por todos los bones del rig.
- `IndexedAnimationSampler` itera únicamente tracks animados y escribe en `RenderAnimationBuffer` reutilizable; no crea maps/poses por tick.
- `IndexedRigPoseResolver` usa arrays primitivos y no crea objetos en el loop normal de bones; produce `RenderPoseBuffer`.
- `RenderPoseBuffer` expone primitive accessors y dirty mask epsilon-aware; `pose(index)` queda sólo como vista de debug/inspección.
- Buffers/clips conservan identidad del `IndexedRenderRig`; se rechaza mezclar accidentalmente assets con igual cantidad de bones.
- `CompiledRenderState` posee sólo playback mutable + dos buffers; el asset pesado se comparte entre instancias. Lookup por `animationId` ocurre en `play()`, no en `tick()`.
- Equivalencia descriptor/indexado: PASS para ONCE/LOOP/HOLD, rest pose, parent scale/rotation y `globalRotation`.
- Dirty-mask harness: 64 parts / 8 animadas => 8 dirty; frame idéntico => 0; siguiente cambio => 8.
- Micro-harness local (no JMH), 64 bones / 16 tracks: pipeline indexado observado entre 2.90x y 3.06x más rápido en tres procesos separados.
- `javac -Xlint:all`: PASS sobre el bloque Core relevante.
- Boundary check: Core mantiene 0 imports `kr.toxicity`; hot path indexado no usa `Map<String,...>`.
- Build Gradle/JAR y smoke CanvasMC continúan pendientes por toolchain/red; no se declara release binario verde.

## Milestone en curso — Native Display Runtime / Batch / LOD

### Especificación
El renderer físico debe consumir exclusivamente `CompiledRenderAsset` + `RenderPoseBuffer`; no puede conocer BetterModel, clips descriptivos ni nombres de bones en el hot path.

Invariantes:
1. El tick de render usa índices densos y buffers reutilizables; 0 lookups por nombre por frame.
2. La primera publicación fuerza todos los parts; frames siguientes consumen sólo la dirty mask.
3. El LOD acumula delta temporal y samplea únicamente cuando corresponde, sin congelar el tiempo lógico de animación.
4. Sin viewers o fuera de distancia, no se generan frames/packets; al reaparecer se hace catch-up en una sola evaluación.
5. El borde Bukkit convierte poses a metadata de Display y agrupa todos los updates dirty en un solo bundle por viewer/frame.
6. Core mantiene 0 imports de Bukkit/BetterModel.
7. Este bloque no inventa todavía el payload visual/model item de cada bone; sólo toma ownership del transform físico.

### Plan
- [x] Añadir política LOD/culling provider-independent.
- [x] Añadir batch reutilizable de parts dirty y runtime de instancia compilada.
- [x] Añadir backend Bukkit de transform para Display entities usando packet batching existente.
- [x] Probar primera publicación, dirty-only, catch-up LOD, culling y stop/rest.
- [x] Validar fronteras Core y compilar el backend Bukkit contra stubs de las firmas usadas.
- [x] Documentar y congelar checkpoint de fuentes sin JAR falso.


### Revisión — Native Display Runtime / Batch / LOD
- `RenderLodPolicy` + `RenderLodLevel`: cadencias 20/10/5/2.5 updates/s aproximadas a 20 TPS (`1/2/4/8` ticks), con culling total fuera del rango.
- `CompiledRenderRuntime`: acumula delta mientras no corresponde samplear; al restaurar visibilidad hace catch-up una sola vez y fuerza todos los parts.
- `RenderFrameBatch`: buffer reutilizable; primera publicación `all`, luego `dirty` solamente.
- `BukkitDisplayPoseSink`: transforma poses nativas a `DisplayData.Translation/Scale/LeftRotation` y manda un único `sendPackets(...)` por viewer/frame; el network manager existente crea el bundle.
- Importante: si cambia un parent, sus descendants pueden quedar dirty aunque no tengan track propio, porque cambia su pose mundial; esto es correcto y necesario para el renderer físico.
- Harness runtime: PASS (first publish, cull/restore catch-up, LOD accumulation, dirty propagation).
- Harness cadencia 20 ticks: 20 frames near / 11 mid / 6 far / 3 very-far / 0 culled.
- Harness Bukkit pose sink: PASS contra firmas stub equivalentes de `Player`, `DisplayData`, packet proxy y JOML.
- Regresiones: `INDEXED_RUNTIME_HARNESS_PASS`, `COMPILED_STATE_HARNESS_PASS`, `DIRTY_MASK_PASS`.
- Build Gradle completo y smoke CanvasMC siguen pendientes por el toolchain/red del entorno; no se declara release verde.

## Milestone en curso — Native Display Entity Lifecycle + BetterModel Static Visual Import

### Especificación
Cerrar el primer ciclo físico completo de VsRender para parts estáticos: importar payload visual desde BetterModel una vez, asignar entidades Item Display propias, y controlar spawn/update/despawn desde VsRender sin `DummyTracker`/`RenderedBone` en el runtime.

Invariantes:
1. Core continúa sin imports Bukkit/BetterModel; el payload NMS vive exclusivamente en el backend Bukkit.
2. Los entity IDs se asignan sólo a parts visuales; locators/null/dummy parts no generan entidades.
3. `show()` envía spawn + item payload + pose actual en un único bundle lógico por viewer.
4. `tick()` publica únicamente dirty transforms y usa el mismo runtime LOD ya validado.
5. `hide()` y `close()` son idempotentes y sólo remueven las entidades propias de la instancia.
6. El adapter BetterModel sólo importa parts cuyo item estático existe; bones dependientes de `BoneItemMapper` dinámico quedan explícitamente fuera de esta ruta nativa y pueden seguir por compatibilidad.
7. El payload importado conserva position/offset/scale de `TransformedItemStack`; no se finge equivalencia visual si esos transforms se pierden.
8. No se elimina la ruta BetterModel existente hasta tener smoke real CanvasMC.

### Plan
- [x] Añadir sidecar Bukkit compilado por part (`BukkitDisplayPartPayload` / asset nativo).
- [x] Implementar `BukkitNativeDisplayInstance` con show/hide/tick/close y ownership de entity IDs.
- [x] Extender el sink para parts no visuales y batching multi-viewer.
- [x] Importar payload estático desde `RendererGroup.getItemStack()` en el adapter BetterModel.
- [x] Aplicar transform visual (position/offset/scale) sin allocations por part en el hot path.
- [x] Añadir harnesses de lifecycle, no-visual parts, dirty updates y close idempotente.
- [x] Reejecutar regresiones del compiled runtime/LOD y boundary checks.
- [x] Documentar revisión y empaquetar checkpoint reproducible.


### Revisión — Native Display Entity Lifecycle + BetterModel Static Visual Import
- `BukkitDisplayPartPayload` conserva item NMS + transform visual `position/offset/scale` exclusivamente en el borde Bukkit; Core no conoce ese payload.
- `BukkitNativeDisplayAsset` alinea el sidecar con `CompiledRenderAsset` por `partIndex` y cuenta sólo parts visuales.
- `BukkitNativeDisplayInstance` asigna IDs/UUIDs estables únicamente a Item Displays reales, soporta show/hide/tick/close idempotentes y remueve sólo sus entidades.
- `BukkitDisplayPoseSink` soporta multi-viewer, ignora parts sin entidad y compone offset rotado + position + scale visual con la pose resuelta.
- `BetterModelNativeDisplayAssetAdapter` importa sólo parts estáticos con `BoneItemMapper.EMPTY`, visibles por defecto y `BukkitItemStack`; cualquier mapper dinámico/part oculta hace fallback completo.
- `BetterModelProvider` activa la ruta nativa sólo para modelos sin animaciones, sin sight-trace/profile/tint y con anchor sin pitch/yaw; el resto conserva `DummyTracker` como fallback.
- NativeDisplayLifecycleHarness: PASS.
- BetterModelNativeDisplayAssetAdapterHarness: PASS.
- BetterModelProviderNativeRouteHarness: PASS, incluyendo fallbacks animated/sight/profile/tint/rotated/adapter-reject.
- Regresiones runtime/LOD/indexed/dirty: PASS.
- Boundary: Core `kr.toxicity` imports = 0; Core render `org.bukkit` imports = 0; runtime Bukkit nativo `DummyTracker|RenderedBone` refs = 0.
- Anchor pitch/yaw nativo queda explícitamente pendiente; hasta implementarlo correctamente, esas instancias no migran.
- Build Gradle/JAR + smoke CanvasMC siguen pendientes por toolchain/red; no se declara release binario verde.

## Milestone en curso — Native Anchor Rotation + Region Animation Host

### Especificación
Cerrar la primera ruta nativa animada de VsRender para modelos BetterModel cuyo payload visual ya es importable, eliminando `DummyTracker` del runtime físico de esas instancias.

Invariantes:
1. `WorldPosition.xRot/yRot` se aplican como rotación de entidad Display, separadas del quaternion de pose del bone.
2. Los modelos animados nativos son tick-driven por el scheduler de región/owner de la plataforma; Core no crea threads.
3. El tick usa el `CompiledRenderRuntime` existente a 20 Hz y mantiene LOD/dirty batching; no aparece un segundo reloj de animación.
4. `RenderAnimationController`/`RenderAnimationSession` nativos controlan `play/stop/finish` sin llamar BetterModel en runtime.
5. Una sesión ONCE notifica finish una sola vez; LOOP/HOLD sólo terminan al cerrarse.
6. Close es idempotente: cancela scheduler, cierra session/controller y remueve sólo Displays propios.
7. BetterModel queda sólo como importador para modelos con payload estático seguro; profile/tint/sight-trace/mappers dinámicos conservan fallback.
8. La ruta nativa puede aceptar clips animados; no se elimina fallback hasta smoke CanvasMC real.

### Plan
- [x] Añadir anchor xRot/yRot al lifecycle nativo y validar spawn packet.
- [x] Añadir controller/session nativo sobre `CompiledRenderRuntime` con completion tick-driven.
- [x] Añadir host Bukkit owner-region que programe/cancele ticks sólo cuando el asset tiene animaciones.
- [x] Habilitar modelos BetterModel animados elegibles en `BetterModelProvider`.
- [x] Añadir harnesses de anchor rotation, scheduler ownership, ONCE/LOOP/HOLD y close race/idempotencia.
- [x] Reejecutar regresiones de compiled runtime/dirty/LOD/static lifecycle y boundary checks.
- [x] Documentar revisión y empaquetar checkpoint reproducible sin JAR preview reciclado.

### Revisión — Native Anchor Rotation + Region Animation Host
- [x] Anchor `xRot/yRot` viaja en el spawn packet del Item Display; bone quaternion sigue en metadata de transformación.
- [x] `CompiledRenderAnimationController` nativo implementa ONCE/LOOP/HOLD y finish exactamente una vez.
- [x] `BukkitNativeRenderInstance` programa sólo assets animables en el owner region (`delay=1`, `period=1`) y assets estáticos no crean task.
- [x] El hot tick salta runtime idle, ONCE completed y HOLD finalizado; LOOP sigue activo.
- [x] `CompiledRenderRuntime.stop()` preserva/publica rest pose en vez de perder el dirty frame al siguiente resolve.
- [x] `BetterModelProvider` permite animated + rotated cuando el payload es context-free; sight/profile/tint/adapter rejection siguen fallback.
- [x] Controller harness: PASS (ONCE/LOOP/HOLD, wrong rig, close idempotente).
- [x] Host harness: PASS (owner chunk, 20 Hz, anchor rotation, native finish, static no-task, cancel on close).
- [x] Tick/close race harness: PASS x2000.
- [x] Provider route harness: PASS (static/animated/rotated native; sight/profile/tint/reject fallback).
- [x] BetterModel visual payload adapter regression: PASS.
- [x] Indexed/compiled/dirty/LOD/display runtime regressions: PASS.
- [x] Boundary: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- [ ] Gradle/JAR completo + smoke CanvasMC: pendiente hasta disponer del toolchain/red; no se declara release binario verde.

## Milestone — Native Entity Anchor + Player Limb Runtime

### Especificación
Extender el runtime nativo packet-backed desde anchors fijos/region-owned a entidades móviles y jugadores, preservando owner-thread correcto y sin reintroducir `DummyTracker`/`RenderedBone` en el runtime.

Invariantes:
1. Una instancia entity-bound se programa exclusivamente en el `EntityScheduler`/owner del `Entity` correspondiente; no queda fijada al chunk inicial.
2. Cada tick toma posición/rotación actual del owner y actualiza el anchor de los Item Displays sin recrear entidades.
3. Viewers se sincronizan desde el tracking real del owner; el player owner puede incluirse explícitamente para player-limb rendering.
4. El runtime compilado/animación sigue siendo el único owner del clock y dirty poses; mover el anchor no fuerza recompilar/sampling por nombre.
5. Player limb payload dinámico se resuelve en el borde BetterModel a partir de `BoneRenderContext`; Core no importa tipos BetterModel/Bukkit.
6. Si skin/equipment/context dinámico no puede resolverse de forma segura, se usa fallback BetterModel completo; no se aproxima visualmente.
7. Close/retire son idempotentes y no dejan repeating tasks ni futures pendientes.
8. No se elimina el fallback BetterModel hasta smoke real CanvasMC.

### Plan
- [x] Añadir anchor mutable packet-backed a `BukkitNativeDisplayInstance` y actualización de posición/rotación sin respawn.
- [x] Añadir `BukkitEntityNativeRenderInstance` owner-entity con `PlatformExecutor.runRepeating(..., entity)` y retired callback.
- [x] Sincronizar viewers desde `Entity.getTrackedBy()` y owner player cuando corresponda.
- [x] Añadir adapter de payload dinámico BetterModel para PLAYER limbs usando contexto/skin resuelto una vez por sesión.
- [x] Migrar `BetterModelPlayerAnimations` a native cuando el limb sea importable; conservar fallback para conflictos/unsupported.
- [x] Añadir harnesses de follow-anchor, scheduler entity ownership, viewer sync, retire/close race y player payload safety.
- [x] Reejecutar regresiones de compiled runtime/dirty/LOD/region host y boundary checks.
- [x] Documentar revisión y empaquetar checkpoint reproducible sin JAR preview reciclado.

### Revisión — Native Entity Anchor + Player Limb Runtime
- `BukkitNativeDisplayInstance` soporta anchor mutable y viewer reconciliation sin respawn; cambio de mundo remueve el snapshot anterior antes de respawn del host.
- `BukkitEntityNativeRenderInstance` usa exclusivamente `PlatformExecutor.runRepeating(..., entity)`, sigue posición/pitch/body-yaw del owner y sincroniza viewers desde tracking real.
- Retire libera el task handle sin cancelarlo de nuevo; `tick ↔ retire ↔ close` pasó 2000 carreras sin doble cancelación ni excepción.
- `BetterModelNativeDisplayAssetAdapter.adaptPlayer(...)` completa `BoneRenderContext` una sola vez y materializa skin/equipment a payload Bukkit antes de entrar al runtime nativo.
- Fallback exacto: `HEAD/HEAD_WITH_CHILDREN`, player montado, entity scale != 1, payload no-Bukkit, part oculta o mapper que falle no migran a native.
- `BetterModelPlayerAnimations` invalida el ticket al iniciar un nuevo `play`, por lo que una resolución async anterior no puede crear un host atrasado; harness `play A → play B` PASS.
- `PlayerItemBreakEvent`, inventory/held/swap/drop, world/death/quit invalidan la sesión para no conservar equipment viejo.
- Native STARTED y fallback STARTED conservan `detail=""`, evitando cambiar el contrato público del service.
- Harness player payload: PASS (dynamic mapper, transform/offset/scale, HEAD/scale/mounted fallback).
- Harness player service: PASS (native route, legacy fallback, async supersession y shutdown pending).
- Entity host harness: PASS (EntityScheduler owner, body yaw, movement, viewers, retire).
- Entity close race: PASS x2000.
- Regresiones: indexed runtime, compiled state, dirty mask, LOD, display lifecycle, region animated host/race, BetterModel payload/provider route: PASS.
- Boundary: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- Gradle `:core:test`: BLOQUEADO por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1. No se declara JAR/release verde ni smoke CanvasMC.


## Milestone — Native Head/Body Follow + Live Player Payload Refresh

### Especificación
Reducir los fallbacks restantes de player/entity sin filtrar semánticas BetterModel a Core y sin recrear la instancia completa ante cambios ordinarios de equipment.

Invariantes:
1. Core describe seguimiento dinámico de rotación mediante una política provider-independent por part; no importa `BoneTags`, BetterModel ni Bukkit.
2. El adapter BetterModel traduce `HEAD`/`HEAD_WITH_CHILDREN` únicamente al importar el rig.
3. El runtime entity-bound obtiene body/head/pitch del owner en su `EntityScheduler` y aplica el follow sin un segundo clock de animación.
4. La transformación dinámica de cabeza debe preservar jerarquía, escala, dirty masks y rest/animation pose; no se aproximan quaternions con Euler en el hot path.
5. Cambios de equipment/skin reemplazan únicamente payloads de display compatibles cuando sea seguro; no recrean `CompiledRenderAsset`, IDs ni animation state.
6. Si profile/scale/passenger/mapper no puede refrescarse exactamente, se conserva fallback/invalidation actual.
7. `tick ↔ refresh ↔ close` debe ser race-safe e idempotente.
8. Core mantiene 0 imports BetterModel/Bukkit y native runtime mantiene 0 `DummyTracker`/`RenderedBone`.

### Plan
- [x] Añadir política nativa de follow de rotación por part y compilarla a índices.
- [x] Aplicar modifier dinámico de head/body sobre pose resuelta sin allocations por part.
- [x] Traducir `BoneTags.HEAD` / `HEAD_WITH_CHILDREN` al contrato nativo en el adapter.
- [x] Habilitar player native para rigs head-follow que cumplan los demás guards.
- [x] Añadir reemplazo live de payload visual por part sin cambiar entity IDs/animation state.
- [x] Conectar refresh de equipment seguro desde `BetterModelPlayerAnimations`.
- [x] Añadir harnesses de head subtree, dirty propagation, payload refresh y carreras close/refresh.
- [x] Reejecutar regresiones/boundaries y congelar checkpoint reproducible.

### Revisión / evidencia
- `HeadFollowHarness`: PASS (`HEAD` y `HEAD_WITH_CHILDREN`, subtree y dirty estable).
- `BetterModelHeadPolicyHarness`: PASS (`BoneTags` traducidos sólo en el adapter).
- `PlayerAssetRefreshHarness`: PASS (payload PLAYER re-resuelto sin recompilar rig/clip; guards scale/mount conservados).
- `NativePayloadRefreshHarness`: PASS (visual→visual conserva entity ID; air↔item crea/elimina sólo el display afectado).
- `HeadServiceRefreshHarness`: PASS (refresh live no recrea host ni reinicia playback; completion tardía tras `stop()` se ignora; legacy conserva interrupt).
- `CompiledStateHarness`: PASS.
- `IndexedRuntimeHarness`: PASS.
- `DirtyMaskHarness`: PASS (`8 dirty → 0 estable → 8`).
- `LodCadenceHarness`: PASS (`20/11/6/3/0` frames según distancia).
- `EntityNativeRenderHostHarness`: PASS.
- `EntityNativeRenderCloseRaceHarness`: PASS x2000.
- `NativeAnimatedRenderHostHarness`: PASS.
- `NativeAnimatedCloseRaceHarness`: PASS x2000.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0; `IndexedRigPoseResolver` sin arrays temporales.
- Diff contra `VsRender-native-entity-player-checkpoint`: sólo archivos del runtime/adapter/player service + `RenderPartRotationPolicy` + `tasks/todo.md`; sin borrados funcionales.
- `bash gradlew :core:test`: BLOQUEADO por `UnknownHostException: services.gradle.org` descargando Gradle 9.7.1. No se declara build/JAR/Canvas smoke verde.

## Milestone — Native General Entity Instances

### Especificación
Extender la abstracción propia de VsRender para que un modelo pueda estar ligado a una entidad móvil, sin reutilizar `RenderInstanceRequest` de anchors fijos y sin exponer Bukkit/BetterModel en Core.

Invariantes:
1. La request entity-bound usa `core.entity.Entity` como owner y conserva `RenderInstanceOptions`.
2. La resolución por provider sigue siendo determinista y soporta provider explícito/legacy sin callbacks externos bajo locks.
3. BetterModel sólo importa rig/clips/payload; una entidad elegible corre en `BukkitEntityNativeRenderInstance` y `EntityScheduler`.
4. Los mappers dinámicos de entidad (`main/off hand`) sólo se aceptan cuando exista refresh live con owner claro; en este corte su resolución exacta queda disponible vía `adaptEntityPayloads`, pero la ruta inicial native los rechaza y hace fallback completo.
5. Scale/passenger/profile/tint/sight-trace o semánticas aún no equivalentes usan fallback completo, no híbrido parcial.
6. Fallback BetterModel entity-bound conserva animation/attachment/viewer lifecycle detrás de `RenderInstance`.
7. Close/retire/request superseding siguen race-safe e idempotentes.
8. Core mantiene 0 imports Bukkit/BetterModel y native runtime 0 `DummyTracker`/`RenderedBone`.

### Plan
- [x] Crear `RenderEntityInstanceRequest` + `RenderEntityInstanceProvider` en Core.
- [x] Añadir resolución async entity-bound a `RenderInstanceRegistry` sin romper create fijo.
- [x] Añadir import `adaptEntity` / payload refresh para modelos GENERAL ligados a entidad.
- [x] Implementar route nativa BetterModelProvider -> `BukkitEntityNativeRenderInstance`.
- [x] Añadir fallback `EntityTracker` detrás de `RenderInstance` para casos no elegibles.
- [x] Añadir harnesses de registry async/provider selection, native route y fallback.
- [x] Reejecutar boundaries/lifecycle y documentar checkpoint.

### Revisión / evidencia
- `RenderEntityInstanceRegistryHarness`: PASS (provider explícito no cae a otro provider, legacy recorre sólo entity providers, CompletionStage async preservado).
- `EntityAssetAdapterHarness`: PASS (payload GENERAL estable nativo; scale/mounted fallback; mapper dinámico rechazado en ruta inicial; `adaptEntityPayloads` resuelve mapper exacto para futuro refresh).
- `BetterModelEntityProviderHarness`: PASS (eligible -> `BukkitEntityNativeRenderInstance`; import no soportado/sightTrace -> fallback `EntityTracker`).
- `BetterModelEntityFallbackHarness`: PASS (show/hide/locator attachment/close idempotente detrás de `RenderInstance`).
- `CompiledStateHarness`: PASS.
- `IndexedRuntimeHarness`: PASS.
- `LodCadenceHarness`: PASS (`20/11/6/3/0`).
- `EntityNativeRenderHostHarness`: PASS.
- `EntityNativeRenderCloseRaceHarness`: PASS x2000.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- `bash gradlew :core:test`: BLOQUEADO por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1; no se declara JAR/smoke verde.
- Diff contra `VsRender-native-head-live-payload-checkpoint`: 3 archivos nuevos + 3 modificados del contrato/provider/adapter + `tasks/todo.md`; sin borrados funcionales.

## Milestone — Native General Entity Live Equipment + Owner Scale/Passenger Offset

### Especificación
Eliminar tres fallbacks importantes del runtime entity-bound GENERAL: mappers dinámicos de main/offhand, `entity scale != 1` y owners montados, manteniendo paridad de lifecycle con BetterModel sin introducir semánticas BetterModel en Core.

Invariantes:
1. El owner entity-bound expone su escala y offset de pasajero como estado de anchor/plataforma; Core no importa BetterModel/Bukkit/NMS.
2. La escala global del owner se aplica a la pose física completa (translation y scale) una única vez por frame, sin recompilar rig/clip ni multiplicar payload visual dos veces.
3. El passenger offset se calcula desde la plataforma propietaria en el EntityScheduler y se incorpora al anchor móvil sin fijar la instancia al vehículo/chunk inicial.
4. Los mappers dinámicos GENERAL (`mainHand`/`offHand`) se resuelven al crear y se refrescan en vivo sobre la misma `BukkitEntityNativeRenderInstance`; entity IDs y animation state se conservan cuando el part sigue siendo visual.
5. Un refresh asíncrono obsoleto después de close/retire o de una generación nueva no puede mutar la instancia.
6. Los cambios de equipment no deben forzar resampling de animación ni resetear playback.
7. Si el contexto dinámico deja de ser representable de forma segura, se cierra la ruta nativa o se mantiene fallback completo; no se deja payload stale.
8. Head/body follow, LOD, dirty masks y viewer sync existentes deben seguir verdes.
9. Core mantiene 0 imports BetterModel/Bukkit; native runtime mantiene 0 `DummyTracker`/`RenderedBone`.

### Plan
- [x] Introducir estado provider-independent de owner transform (`scale` + offset) en el host entity-bound sin romper callers actuales.
- [x] Aplicar owner scale/offset al backend de poses con dirty/LOD correctos y sin allocations por part.
- [x] Reemplazar el guard `scale == 1 / !mounted` del adapter BetterModel por extracción exacta del transform owner.
- [x] Habilitar mappers dinámicos GENERAL en import inicial y refresh live de payloads.
- [x] Conectar refresh owner para main/offhand/equipment sin reiniciar animation state.
- [x] Añadir harnesses de scale, passenger offset, main/offhand refresh y close race.
- [x] Reejecutar regresiones/boundaries y congelar checkpoint reproducible.

### Revisión / evidencia
- `OwnerTransformParityHarness`: PASS. Fórmula nativa validada contra BetterModel: `(pose + offsetRotado) * ownerScale + itemPosition - passengerAttachment`; scale visual multiplicado una sola vez.
- `LiveHandPayloadHarness`: PASS. visual→visual conserva entity ID; item→AIR elimina sólo ese Display; AIR→item crea únicamente el Display afectado; animation state no se reinicia.
- `EntityOwnerTransformHarness`: PASS. `LivingEntity` scale y `EntityAttachment.PASSENGER` se extraen en el borde Bukkit y se convierten a `BukkitDisplayOwnerTransform`.
- `BetterModelEntityHandBindingHarness`: PASS. `RIGHT_ITEM`/`LEFT_ITEM` se compilan a `MAIN_HAND`/`OFF_HAND`; mappers dinámicos desconocidos conservan fallback completo.
- `EntityOwnerTransformCloseRaceHarness`: PASS x2000 (`tick ↔ close`, sin doble cancelación ni excepción).
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- Diff contra `VsRender-native-general-entity-checkpoint`: 4 archivos nuevos + 8 modificados, 0 eliminados. Cambios limitados a adapter BetterModel, proxy NMS de attachments, backend Bukkit y `tasks/todo.md`.
- Corrección conceptual: `BaseEntity.passengerPosition()` representa siempre el punto `EntityAttachment.PASSENGER`; no es una condición de “está actualmente montado”. El offset nativo se calcula siempre y queda en cero/valor correspondiente según la entidad.
- `bash gradlew :core:test --no-daemon --max-workers=2`: BLOQUEADO por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1. No se declara build/JAR/Canvas smoke verde.

## Milestone — Native Entity Visual State + Damage Parity

### Especificación
Absorber el estado visual que todavía pertenece a BetterModel `EntityTracker`: invisibility/glow y feedback de daño, sin volver a introducir tipos BetterModel en Core ni inferir eventos por polling.

Invariantes:
1. Invisibility y glow del owner se traducen a metadata de Item Display; `followMobInvisibility` conserva la configuración importada del provider.
2. Damage tint usa la misma semántica de CustomModelData colors y duración de BetterModel; el item base nunca se muta.
3. El evento de daño se recibe explícitamente desde Bukkit y se enruta por UUID a instancias nativas; no se adivina por health/noDamageTicks.
4. Un tint activo sobre main/offhand sigue usando el payload más reciente y al finalizar restaura ese payload, no uno obsoleto.
5. Modelos que declaran auto-clips BetterModel `damage`, `death` o `jump` conservan fallback completo hasta implementar layering/event animation nativo; no se reemplaza una overlay/event animation por un `play()` exclusivo aproximado.
6. show/refresh/close y tick↔damage↔close deben ser idempotentes/race-safe.
7. Core mantiene 0 imports Bukkit/BetterModel y native runtime 0 `DummyTracker`/`RenderedBone`.

### Plan
- [x] Añadir estado visual nativo (invisible/glowing) al lifecycle de Item Display.
- [x] Añadir tint helper Bukkit compatible con CustomModelData colors y cache por part.
- [x] Añadir damage tint lifecycle sin mutar payload base.
- [x] Añadir registry/listener de daño por UUID para instancias entity-bound.
- [x] Pasar `followMobInvisibility` desde BetterModelProvider/PlayerAnimations al host nativo.
- [x] Mantener fallback explícito para modelos con auto-clips `damage`/`death`/`jump` hasta animation layering nativo.
- [x] Añadir harnesses visual state, tint duration/dynamic payload y damage/close race.
- [x] Reejecutar regresiones/boundaries/Gradle gate y congelar checkpoint.

### Revisión / evidencia
- `BukkitDisplayEntityVisualState`: invisibility/glow se publican como SharedFlags de los Item Displays sin respawn; estado inicial y cambios posteriores PASS.
- `BukkitDisplayTintSupport`: PASS contra semántica BetterModel de `CustomModelData.colors`; preserva floats/flags/strings, multiplica colores RGB y no muta el item base.
- `BukkitNativeDisplayInstance`: PASS para tint 0xFF8080, retrigger bloqueado durante ventana activa, restauración en el mismo ciclo 10→0 de BetterModel y payload main/offhand actualizado durante tint sin cambiar entity ID.
- `BukkitNativeEntityRenderRegistry` + listener Bukkit: PASS; `EntityDamageEvent` MONITOR + `ignoreCancelled=true` enruta el UUID exacto sin polling.
- `BetterModelProvider` visual-state gate: PASS. Eligible → native con `followMobInvisibility` importado; auto `damage/death/jump` → fallback; backend tint no soportado → fallback.
- `damage ↔ close` registry race: PASS x2000, sin leaks de UUID/instancias.
- Regresiones: `CompiledStateHarness`, `IndexedRuntimeHarness`, `DirtyMaskHarness`, `LodCadenceHarness`, `NativeDisplayRuntimeHarness`, `NativeDisplayLifecycleHarness`, `HeadFollowHarness`, `NativeAnimatedRenderHostHarness` PASS; close race animada PASS x2000.
- Regresiones de entity payload: visual host/damage registry, owner transform, live main/offhand y BetterModel hand binding PASS.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- Diff contra `VsRender-native-entity-live-equipment-checkpoint`: 4 archivos nuevos + 7 modificados, 0 eliminados; cambios limitados a backend/registry/listener Bukkit, BetterModel routing y `tasks/todo.md`.
- `bash gradlew :core:test --no-daemon --max-workers=2`: BLOQUEADO por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1. No se declara build/JAR/Canvas smoke verde.

## Milestone — Native Damage/Jump Event Animation Interrupt v1

### Especificación
Migrar la semántica BetterModel demostrablemente equivalente para auto `damage` y `jump`: clips play-once con `override=false` que preemptan el rig completo. Mantener fallback para `death` y cualquier event clip `override=true` hasta tener stack/prioridad por bone y lifecycle de removal nativo.

Invariantes:
1. `damage`/`jump` reinician su clip si vuelven a dispararse mientras están activos; el evento más nuevo gana, igual que la inserción más reciente a prioridad equivalente en BetterModel.
2. Un playback LOOP/HOLD preemptado vuelve a arrancar desde el comienzo al terminar el evento, igual que `AnimationIterator.clear()` de BetterModel.
3. Un playback ONCE preemptado queda terminado y ejecuta su callback una sola vez al finalizar el evento; no se reanuda.
4. Cerrar la sesión base durante un evento no detiene el clip transitorio; al finalizar queda rest pose.
5. Un nuevo playback debe cancelar cualquier rest-frame pendiente dejado por `stop()` y tomar ownership inmediatamente.
6. Un nuevo playback manual sin sesión base puede preemptar el evento transitorio.
7. `damage/jump override=true` y cualquier `death` continúan en fallback completo.
8. Damage tint sigue independiente del clip y funciona aunque el modelo no tenga animación `damage`.

### Plan
- [x] Añadir interrupt transitorio full-rig play-once al controller compilado.
- [x] Corregir `CompiledRenderRuntime.play()` para cancelar rest-frame pendiente de un playback anterior.
- [x] Conectar `onOwnerDamage()` al interrupt además del tint.
- [x] Conectar `EntityJumpEvent`/`PlayerJumpEvent` al mismo routing nativo.
- [x] Relajar fallback para `damage/jump override=false`; conservar `death`/override en BetterModel.
- [x] Añadir gates de restart, evento→evento, LOOP restore, ONCE preemption y close-during-event.
- [x] Reejecutar visual/tint/provider/race + regresiones y boundaries finales.
- [x] Congelar checkpoint reproducible sin declarar build completo verde.

### Revisión parcial
- `EventGate`: PASS. Playback nuevo supersede rest pendiente; damage reinicia LOOP; ONCE interrumpida termina exactamente una vez; retrigger damage reinicia; damage→jump conserva la sesión suspendida; close durante transient termina en rest.
- `NativeEntityEventRoutingGate`: PASS. Registry enruta damage+jump a todas las instancias del UUID y unregister no deja entradas.
- `BetterModelAutoEventGuardGate`: PASS. damage/jump `override=false` → native; damage/jump `override=true` → fallback; death → fallback.
- Listener usa `MONITOR + ignoreCancelled=true`, igualando los eventos automáticos de BetterModel para damage/jump.
- `CompiledRenderRuntime.play()` cancela `forceCurrentPose` pendiente de `stop()`: gate dedicado PASS; evita que el primer tick del playback nuevo sea reemplazado por rest.
- `BukkitDisplayTintSupport`: PASS (regresión CustomModelData tint).
- `OwnerTransformParityHarness`, `LiveHandPayloadHarness`, `EntityOwnerTransformHarness`, `EntityOwnerTransformCloseRaceHarness x2000`, `BetterModelEntityHandBindingHarness`: PASS.
- Boundaries finales: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- `bash gradlew :core:test --no-daemon --max-workers=2`: BLOQUEADO únicamente por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1.

## Milestone en curso — Native Death Terminal Lifecycle + Per-Bone Override Foundation

### Especificación
Cerrar el fallback terminal de `death` sin degradar lifecycle, y preparar/remover el fallback de auto-eventos `override=true` únicamente cuando exista selección por bone equivalente a BetterModel.

Invariantes:
1. `death` nativo es terminal: no reanuda playback base ni acepta nuevos playbacks/eventos una vez iniciado.
2. `death` sólo entra por ruta nativa cuando el clip existe y `override=false`; `override=true` continúa en compat hasta que el layer stack per-bone esté verificado.
3. Una muerte válida mantiene el clock aun con 0 viewers para poder completar el clip y cerrar físicamente la instancia.
4. El owner muerto puede seguir siendo anchor mientras siga válido; si el owner es retirado antes de completar, cleanup inmediato e idempotente.
5. Al completar `death`: unregister UUID, cancelar scheduler propio, remover displays y cerrar animation controller exactamente una vez.
6. `death ↔ close`, `death ↔ damage/jump`, retrigger y viewer-loss no pueden reiniciar ni duplicar el terminal.
7. La capa per-bone debe usar índices/arrays en hot path, sin `Map<String,...>` por tick ni allocations por bone/tick.
8. La precedencia de capas debe ser determinista: prioridad descendente y, a igual prioridad, inserción más reciente primero, equivalente al `PriorityMap` auditado de BetterModel.
9. Un clip `override=true` reclama cada bone con track y todo su subtree; los descendientes sin track reciben pose de animación identidad y enmascaran la capa inferior. Un clip no-override reclama el rig completo.
10. No retirar fallback `override=true` hasta validar presencia por bone, restart/stop y dirty propagation con tests.

### Plan
- [x] Implementar terminal event ownership en `CompiledRenderAnimationController`.
- [x] Añadir avance de runtime cuando está culled/no-viewers sólo para lifecycle terminal explícito.
- [x] Rutear `EntityDeathEvent` al registry nativo y completar cleanup terminal de `BukkitEntityNativeRenderInstance`.
- [x] Relajar BetterModel fallback para `death override=false` únicamente.
- [~] Añadir tests de death natural, 0 viewers, retrigger, damage-after-death, close race y owner removal. (Core/races PASS; owner-removal queda cubierto por branch físico y requiere smoke Bukkit/Canvas.)
- [x] Diseñar/implementar layer stack indexed per-bone con prioridad/insertion ordering.
- [ ] Habilitar `damage/jump override=true` sólo después de integrar el stack al controller y validar selección por subtree, dirty propagation y restauración/preemption por bone.
- [x] Reejecutar regresiones y boundaries; intentar Gradle real sin maquillar el resultado.
- [x] Actualizar review/evidencia y congelar checkpoint reproducible.


### Revisión — corte objetivo ~85%
- `death override=false` ahora entra por lifecycle terminal nativo: no restaura base, bloquea nuevos playbacks/eventos y conserva ownership hasta `close()`.
- El runtime tiene avance culled opt-in: death progresa sin viewers sin publicar frames; el culling normal conserva comportamiento previo.
- `EntityDeathEvent` enruta por UUID al host nativo. Si el owner desaparece o muere sin terminal soportado, el host usa `close()` y cancela su repeating task; `retired()` queda reservado al callback real del scheduler.
- BetterModel fallback se relajó sólo para `death override=false`; `death override=true` y `damage/jump override=true` siguen en compat.
- `IndexedAnimationLayerStack` implementa prioridad descendente + inserción más reciente, selección por part index y máscara `override` por subtree. LOOP/HOLD preemptados reinician por bone; ONCE preemptado termina sólo en ese bone.
- `IndexedAnimationClip` incorpora lookup directo `trackByPart` para evitar búsquedas/string maps en el hot path del layer stack.
- `DeathGate`: PASS; terminal con 0 viewers, callback único, retrigger idempotente y rechazo de death override.
- `DeathRaceGate`: PASS x2000 `death ↔ close` + x2000 `death ↔ damage`; sin excepción ni ownership final inválido.
- `LayerStackGate`: PASS; subtree override, empate por inserción y estado de preemption independiente por bone.
- Compilación aislada de Core render con `javac -Xlint:all`: PASS.
- Boundaries: Core render BetterModel imports = 0; Bukkit imports = 0; runtime nativo `DummyTracker|RenderedBone` refs = 0.
- Gradle real intentado de nuevo: BLOQUEADO por `UnknownHostException: services.gradle.org` descargando Gradle 9.7.1. No se declara JAR/Canvas smoke verde.
- Estado estimado de fusión tras este corte: **~85%**. El siguiente bloqueo funcional es conectar `IndexedAnimationLayerStack` al runtime/controller para retirar `override=true`, seguido por hitboxes/nametags/mounts y smoke standalone.

## Milestone en curso — Activar Per-Bone Override Runtime + retirar auto-event fallback

### Especificación
Conectar `IndexedAnimationLayerStack` al runtime/controller real sin duplicar clocks. El controller nativo será el owner de capas para sesiones/eventos; el API directo `CompiledRenderRuntime.play()` conserva su ruta single-playback para callers simples.

Invariantes:
1. No coexistirán dos clocks activos sobre el mismo pose buffer: single-playback y layered-playback son modos mutuamente excluyentes.
2. Las mutaciones de capas pueden usar IDs/ordenamiento; el tick por part continúa sin `Map<String,...>` ni allocation por bone/tick.
3. La sesión base usa una capa única; `damage/jump` usan una capa transient reutilizable y el último evento reemplaza al anterior en vez de reanudarlo.
4. `override=false` reclama todo el rig; `override=true` reclama tracks + subtree y deja avanzar partes no afectadas de la capa inferior.
5. LOOP/HOLD preemptados se reinician sólo en los bones afectados; ONCE preemptado termina sólo en esos bones.
6. Al retirar una capa, la pose seleccionada inferior se resuelve inmediatamente con delta 0 para no dejar un frame obsoleto.
7. `death` terminal acepta override y cierra al completar únicamente sus bones afectados; no reanuda ni acepta nuevos owners.
8. Completion callbacks globales continúan exactamente una vez.
9. No retirar fallback BetterModel hasta que controller + runtime + adapter gates demuestren damage/jump/death override=true.
10. Core mantiene 0 imports Bukkit/BetterModel; runtime nativo mantiene 0 `DummyTracker`/`RenderedBone`.

### Plan
- [x] Añadir modo layered mutuamente exclusivo a `CompiledRenderState`/`CompiledRenderRuntime`.
- [x] Mover `CompiledRenderAnimationController` a capas base/transient/terminal.
- [x] Añadir tests de dirty restoration, subtree progression y completion/retrigger.
- [x] Habilitar `damage/jump/death override=true` en el guard BetterModel sólo tras gates verdes.
- [x] Reejecutar death/lifecycle/race/regresiones/boundaries.
- [x] Actualizar BUILD-STATUS/VALIDATION/todo review y congelar checkpoint reproducible.

### Revisión — Per-Bone Override Runtime
- `CompiledRenderState`/`CompiledRenderRuntime` tienen modo layered mutuamente excluyente con el playback simple; ambos usan el mismo `RenderAnimationBuffer` y `RenderPoseBuffer`, sin segundo clock/pipeline.
- `CompiledRenderAnimationController` usa capas indexadas `base/transient/terminal`; `damage/jump` reemplazan la misma transient layer y `death` conserva ownership terminal.
- `override=true` reclama tracks + subtree; `override=false` reclama rig completo. Las partes inferiores no afectadas continúan avanzando y las partes preemptadas restauran/reinician según semántica BetterModel.
- Al retirar una capa se resuelve inmediatamente la capa inferior/rest con delta 0 y dirty sólo en parts que cambiaron.
- `BetterModelProvider`, `BetterModelPlayerAnimations` y `BetterModelNativeDisplayAssetAdapter` ya no fuerzan fallback por auto-eventos override; damage/jump/death override=true entran por runtime nativo cuando el resto de capabilities es compatible.
- `RuntimeRegressionGate`: PASS; playback simple, stop→rest, replay ownership y culling conservados.
- `LayerStackGate`: PASS; prioridad, inserción, subtree override y preemption independiente por bone.
- `OverrideControllerGate`: PASS; base + damage/jump/death override, progression de parts no afectadas, dirty restoration y terminal sin viewers.
- `DeathGate`: PASS; `death override=true` aceptado, callback único y ownership terminal.
- `DeathRaceGate`: PASS x2000 `death ↔ close` + x2000 `death ↔ damage`.
- Core render compila con `javac -Xlint:all` sobre 43 fuentes: PASS.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; runtime nativo `DummyTracker|RenderedBone` refs = 0; guard legacy auto-event = 0 refs.
- Gradle real reintentado con `bash gradlew :core:test --no-daemon --max-workers=2`: BLOQUEADO exclusivamente por `UnknownHostException: services.gradle.org` al descargar Gradle 9.7.1. No se declara JAR ni Canvas smoke verde.


## Milestone en curso — Native Rig Hitboxes

### Especificación
Absorber hitboxes de entidad sin volver a acoplar collision/gameplay al renderer visual. La topología y geometría viven en Core; Bukkit posee el backend físico/routing.

Invariantes:
1. Core no importa Bukkit/BetterModel. Una hitbox es metadata provider-independent asociada a `RenderPart`.
2. Preservar dos conceptos separados de BetterModel: offset local del centro (`hitBoxPoint`) y caja centrada/dimensiones. No colapsarlos en un único offset.
3. El fallback mínimo de BetterModel para `hitbox` sin caja explícita es 0.1×0.1×0.1 centrado.
4. El importer marca hitbox cuando el bone se llama `hitbox`, posee tag HITBOX o su mount controller puede montar; mounts se consumirán en su milestone propio.
5. El runtime compilado expone índices de hitbox y geometría densa; no busca names/tags por tick.
6. Gameplay/raycast debe poder consultar AABB actual aunque un part no tenga Item Display; locator/null/display visibility no decide existencia de hitbox.
7. El pose de hitbox debe derivarse del rig resuelto, no de `RenderedBone` ni `DummyTracker`.
8. Damage/interact routing debe resolverse por owner UUID + part index y cleanup idempotente; no retener BetterModel objects.
9. No declarar colisión física vanilla si el backend inicial sólo cubre target/interact/damage; cualquier limitación debe quedar explícita.
10. No retirar fallback de hitbox hasta que geometry + lifecycle + routing tengan gates físicos/aislados suficientes.

### Plan
- [x] Añadir contrato `RenderHitBox` y metadata opcional en `RenderPart`.
- [x] Compilar hitbox indices/geometría a `IndexedRenderRig`.
- [x] Importar semántica BetterModel (`hitbox`/HITBOX/mount-capable + box/center/MIN).
- [x] Añadir resolver de centro desde `RenderPoseBuffer` y buffer denso provider-independent; owner/world transform queda en el backend Bukkit.
- [x] Diseñar backend Bukkit packet-only de target/interact/damage separado de `BukkitNativeDisplayInstance`.
- [~] Añadir lifecycle/registry/routing y tests de create/remove/owner cleanup. Registry/cleanup + forwarding source-shape están hechos; smoke físico Canvas y projectile routing siguen pendientes.
- [x] Reejecutar per-bone/death regressions + boundaries antes de retirar fallback de hitbox-only.

### Revisión parcial — Native Rig Hitboxes
- `RenderHitBox` conserva `centerOffset` separado de extents centrados y `MIN = 0.1³`.
- `IndexedRenderRig` compila índices y geometría dense; `RenderHitBoxBuffer`/`IndexedHitBoxPoseResolver` siguen el pose resuelto sin BetterModel/Bukkit.
- `CompiledRenderState` posee el hitbox buffer y lo resuelve junto al pose; `CompiledRenderRuntime.advanceForGameplay()` permite 20 TPS exactos sin segundo clock aunque el visual esté bajo LOD/culling.
- Backend Bukkit: `BukkitNativeRigHitBoxInstance` usa entidades `Interaction` packet-only con envelope horizontal conservador y AABB exacta X/Y/Z en `BukkitNativeRigHitBoxRegistry`.
- Attack/interact packet routing valida raycast contra la AABB exacta y reescribe al entity ID real del owner; así el daño/interacción vanilla lo procesa el owner, no un fake entity.
- World change limpia viewers del hitbox y fuerza respawn; close desregistra IDs y elimina packet entities de forma idempotente.
- `BetterModelNativeDisplayAssetAdapter` permite hitbox-only en la ruta ENTITY nativa, pero sigue bloqueando SEAT/SUB_SEAT/TAG/MOB_TAG/PLAYER_TAG y mount controllers; static/player conservan fallback.
- `HitBoxGeometryGate`: PASS.
- `GameplayCadenceGate`: PASS.
- `RuntimeRegressionGate`, `LayerStackGate`, `DeathGate`, `OverrideControllerGate`, `DeathRaceGate x2000+x2000`: PASS.
- Parse gate de los 6 archivos Bukkit/compat tocados: PASS.
- Limitación explícita: no se declara todavía projectile interception ni colisión física vanilla; se mantiene fallback para mount/seat/nametag.
- Gradle real continúa bloqueado por `UnknownHostException: services.gradle.org`; no se declara JAR/Canvas smoke.

## Milestone — Native Entity Nametags + Sight Trace + Mount Contract Foundation

### Especificación
Absorber TAG/MOB_TAG/PLAYER_TAG y sight-trace sin BetterModel runtime, preservando el punto local del bone y la política de visibilidad upstream. Preparar mounts con contrato provider-independent sin activar un backend de conducción parcial.

### Plan
- [x] Separar `anchorOffset` genérico de la geometría `RenderHitBox`; nametag no depende de que el bone sea hitbox.
- [x] Añadir `RenderNameTagMode` y compilar índices/modos a `IndexedRenderRig`.
- [x] Importar BetterModel TAG / MOB_TAG / PLAYER_TAG y `hitBoxPoint` como anchor local.
- [x] Implementar TextDisplay packet-only para nametags entity-bound, con customName live y lifecycle per-viewer.
- [x] Reproducir `EntityUtil.isCustomNameVisible/isInPoint`: mismo mundo, <=5 bloques y cono ±10°.
- [x] Habilitar nametags en eligibility nativa ENTITY; static/player siguen fallback cuando declaran capabilities físicas no migradas.
- [x] Implementar `RenderSightVisibility` equivalente a BetterModel `canSee` y aplicarlo a rutas native static/entity.
- [x] Capturar `minSight/maxSight` en el borde BetterModel; runtime no llama BetterModel por tick.
- [x] Añadir `RenderMountPolicy` provider-independent y mapping de built-ins BetterModel (disabled/passenger/walk/fly); custom queda `UNSUPPORTED`.
- [ ] Implementar backend mount real con passenger carrier + WALK/FLY input/jump/dismount parity antes de quitar ese fallback.
- [x] Reejecutar hitbox/death/override regressions y boundaries.

### Revisión / evidencia
- `NameTagMetadataGate`: PASS; tres modos e índices densos + anchor genérico preservados.
- `NameTagVisibilityGate`: PASS; rango, mundo y orientación equivalentes a BetterModel custom-name visibility.
- `SightVisibilityGate`: PASS; min/max + view cone equivalentes a `EntityUtil.canSee`.
- `MountPolicyGate`: PASS; PASSENGER/WALK/FLY y unsupported se mantienen tipados sin BetterModel en Core.
- Nametag backend: TextDisplay packet-only, billboard CENTER, interpolation=3 y transform `(-1/40, -0.225, 0)`; customName se refresca sin recrear el rig.
- Viewer lifecycle corregido: perder tracking envía remove packet antes de retirar el viewer del set; world change/close limpian idempotentemente.
- Host entity-bound comparte un único sample gameplay para hitboxes + nametags; LOD visual consume el pose ya actualizado con delta 0.
- Native static/entity sight-trace usa el mismo math pure-Core; no hay consulta BetterModel en hot path.
- `HitBoxGeometryGate`, `GameplayCadenceGate`, `RuntimeRegressionGate`, `LayerStackGate`, `OverrideControllerGate`, `DeathGate`: PASS.
- `DeathRaceGate`: PASS x2000 death↔close + x2000 death↔damage.
- Parse gate de fuentes Core/Bukkit/compat tocadas: PASS.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- Mounts: sólo contrato/import foundation. No se declara conducción, salto/vuelo ni mount lifecycle nativo todavía; fallback permanece.
- Gradle/JAR/Canvas smoke no se declaran verdes hasta ejecutar wrapper Java 25 real.

## Milestone en curso — Native Mount / Seat Runtime + Projectile Hitboxes (objetivo ~94–95%)

### Especificación
Este bloque elimina los fallbacks físicos restantes que impiden declarar la ruta entity-bound casi standalone.

Invariantes:
1. `SEAT` y `SUB_SEAT` deben resolverse desde metadata del rig nativo; Core no importa Bukkit/BetterModel.
2. Cada seat tiene identidad estable por part-index, pose resuelta desde el mismo `CompiledRenderRuntime` y lifecycle idempotente con owner/viewers/world-change/close.
3. `PASSENGER_ONLY` sólo transporta pasajeros; `WALK` y `FLY` pueden controlar al owner con input del rider sin crear un segundo clock de animación.
4. Montar/desmontar y mutar entidades ocurre en el scheduler del owner/rider; Netty sólo encola intención/value-state.
5. `canDismountBySelf`, `canJump`, `canBeDamagedByRider` se preservan como política explícita.
6. Projectile interception de hitboxes debe usar la AABB exacta nativa y reenviar al owner; no se considera paridad de colisión si sólo existe el envelope de `Interaction`.
7. No se elimina fallback hasta que el gate equivalente esté verde.
8. No se declara JAR/release/smoke verde mientras Gradle/Java25/Canvas real sigan bloqueados.

### Plan
- [ ] Añadir índice/buffer/resolver nativo de mounts por part-index y tests Core.
- [ ] Implementar carrier Bukkit server-side para seats con mount/dismount/lifecycle y world migration.
- [ ] Implementar input/motion `PASSENGER_ONLY/WALK/FLY` con reglas de jump/fly/rider-damage.
- [ ] Enrutar interacción de hitbox mountable al seat exacto sin romper forwarding al owner.
- [ ] Implementar projectile interception contra AABB exacta y forwarding al owner sin duplicar hits.
- [ ] Ejecutar gates de mounts/projectiles + todas las regresiones death/override/hitbox/nametag/sight.
- [ ] Auditar referencias runtime BetterModel/CraftEngine externas y actualizar BUILD-STATUS/VALIDATION.
- [ ] Congelar checkpoint reproducible + patch + SHA sólo si el árbol reconstruye exactamente.

### Revisión
Pendiente hasta cerrar este milestone.

## Milestone en curso — Native Mount Runtime + Projectile Hitbox Parity (~95%)

### Especificación
Cerrar la ruta nativa de `SEAT` / `SUB_SEAT` y de proyectiles sobre hitboxes sin volver a introducir BetterModel en el hot path.

Invariantes:
1. `SEAT` y `SUB_SEAT` usan carriers server-side reales y mantienen pose desde el mismo `CompiledRenderRuntime`; no existe un segundo clock de animación.
2. `PASSENGER_ONLY`, `WALK` y `FLY` preservan las reglas importadas: input, strafe/reversa, fricción, salto, no-gravity/no-AI y protección de daño del rider.
3. Mount/dismount/world-change/death/close deben limpiar registry, pasajeros y estado mutado del owner de forma idempotente y segura para scheduler/Folia.
4. Proyectiles resuelven el AABB nativo exacto, respetan la colisión vanilla previa y delegan el impacto final a Paper/vanilla; no se reimplementa damage/deflection/piercing.
5. Los registries de mount/hitbox no pueden dejar entradas stale tras close/dismount/unregister.
6. Core render mantiene 0 imports Bukkit/BetterModel; runtime nativo mantiene 0 `DummyTracker`/`RenderedBone`.
7. La colisión física vanilla general de hitboxes (push/entityInside) no se declara equivalente hasta existir backend real; debe quedar documentada como limitación si sigue pendiente.
8. No se declara 95% hasta repetir gates anteriores y auditar todos los fallbacks BetterModel/CraftEngine restantes.

### Plan
- [x] Implementar pose buffer/index de mounts y controller Core para PASSENGER_ONLY/WALK/FLY.
- [x] Implementar carriers `Interaction` reales por seat y registry rider/carrier.
- [x] Integrar input del rider, movimiento NMS, salto/vuelo, restore del owner y lifecycle death/world-change/close.
- [x] Implementar projectile interception contra AABB exacto y delegación a `Projectile#hitEntity`.
- [x] Recompilar shape actual de mounts/projectiles y añadir gates de cleanup/race/projectile ordering.
- [x] Reejecutar regresiones death/override/hitbox/nametag/sight + boundaries.
- [x] Auditar fallbacks BetterModel/CraftEngine que todavía pueden ejecutar runtime físico.
- [x] Corregir fallos reales encontrados por la auditoría; no maquillar la colisión física faltante.
- [x] Actualizar BUILD-STATUS/VALIDATION/review y congelar checkpoint reproducible si el corte alcanza ~95% verificable.

### Revisión — Native Mount Runtime + Projectile / Compatibility Tranche (~95%)
- `MountCoreGate`: PASS; pose/index, WALK/FLY, reverse/strafe, no-gravity MoveType.FLY y jump parity.
- `MountBukkitShape`: PASS contra las fuentes actuales; carriers Interaction reales + owner motion/lifecycle compilan en el fixture de frontera.
- `MountRegistryGate`: PASS; `MountRegistryRaceGate`: PASS x5000 tras cerrar race unregister↔mounted con target active/sin rider stale.
- `ProjectileRegistryGate`: PASS; `ProjectileInterceptorGate`: PASS, incluyendo prioridad de colisión vanilla previa y cleanup del tracker.
- `TintLayerGate`: PASS; selector includeChildren, precedencia de capa posterior e inmutabilidad del asset base.
- `EntityProfileNativeRouteGate`: PASS; profile entity-bound entra por `RenderSource.completeContext()` y no requiere EntityTracker para la ruta nativa.
- `AttachmentCoreGate` + `NativeAttachmentBindingGate`: PASS; locator index/pose, quaternion→Euler y close idempotente.
- `CoreRegressionGate`: PASS; terminal death, transient restore, override subtree, nametag/sight e hitbox MIN siguen verdes.
- Boundaries: Core render BetterModel imports = 0; Core render Bukkit imports = 0; native Bukkit runtime `DummyTracker|RenderedBone` refs = 0.
- Fallbacks restantes documentados en `docs/VSR-RUNTIME-FALLBACK-AUDIT-95.md`: static profile async, custom/unsupported physical semantics, player safety fallback y dependencia externa BetterModel como importer/context source.
- No se declara full vanilla physical collision para hitboxes ni standalone `VsRender.jar`; Gradle 9.7.1 sigue bloqueado por DNS y el runtime local es Java 21, no Java 25.
- `PatchRebuildExactGate`: PASS contra el checkpoint ~90%; ZIP determinista + `unzip -t` también PASS.


## Milestone en curso — Native Static Profile + Final Runtime Parity (objetivo ~98%)

### Especificación
Cerrar los últimos fallbacks físicos normales sin convertir el porcentaje en una métrica cosmética. El objetivo es que las rutas representables de block/furniture/entity/player usen runtime nativo incluso con profile/skin externo, y que los límites restantes queden reducidos a compatibilidad excepcional + build/smoke.

Invariantes:
1. Static profile no puede bloquear el thread del servidor mientras BetterModel completa skin/contexto.
2. El lifecycle viewer-facing debe sobrevivir a materialización async: show/hide/close antes de completar no pueden perder viewers ni revivir una instancia cerrada.
3. La materialización async no puede introducir BetterModel en Core; Core sólo expone un proxy/deferred provider-independent.
4. Si el asset static profile resulta no representable, el fallback legacy se conserva explícitamente y queda contenido en el adapter.
5. Hitbox physical parity debe avanzar sólo con semántica verificable; no se declara push/collision vanilla exacta si no existe cuerpo server-side equivalente.
6. Boundaries previos (Core BetterModel/Bukkit = 0; runtime nativo DummyTracker/RenderedBone = 0) deben permanecer verdes.
7. El porcentaje ~98% exige reejecutar gates del checkpoint ~95%, no sólo compilar archivos nuevos.
8. JAR/Canvas smoke continúa fuera del porcentaje de source/runtime hasta disponer Java 25 + Gradle 9.7.1.

### Plan
- [x] Añadir `DeferredRenderInstance` provider-independent con lifecycle/race tests.
- [x] Resolver static profile/skin async a `BoneRenderContext` y materializar `BukkitNativeRenderInstance` sin DummyTracker en la ruta normal.
- [x] Mantener fallback legacy sólo para asset no representable/error y probar close-before-complete + viewer replay.
- [x] Auditar y reducir referencias físicas `DummyTracker/EntityTracker` restantes a fallbacks excepcionales.
- [x] Revalidar mounts/projectiles/tint/profile/attachments + regresiones Core.
- [x] Auditar hitbox physical parity y cerrar sólo comportamientos que puedan implementarse sin una entidad NMS falsa.
- [x] Actualizar matriz de fallbacks, BUILD-STATUS/VALIDATION y congelar checkpoint/patch/SHA reproducible.

### Revisión — Native Static Profile + Final Runtime Parity (~98%)
- `DeferredRenderInstanceGate`: PASS x5000; bind/close no revive instancias ni deja callbacks post-close.
- `DeferredRenderInstanceJUnitGate`: PASS x1000 sobre el test fuente agregado; replay, hide y close-before-bind verificados.
- `StaticProfileNativeRouteGate`: PASS; `RenderSource.completeContext()` async, sin `join/Future.get`, materialización nativa en scheduler de chunk y fallback legacy sólo unsupported/error.
- `BetterModelProviderShapeGate`: PASS contra boundary stubs con firmas de scheduler/request/provider.
- Gates 95 reejecutados: MountCore, MountBukkitShape current-source, MountRegistry, MountRegistryRace x5000, ProjectileRegistry, ProjectileInterceptor, TintLayer, AttachmentCore, NativeAttachmentBinding y CoreRegression: PASS.
- Diff vs checkpoint oficial ~95: el runtime físico previo quedó byte-idéntico; cambios de código limitados a `BetterModelProvider`, `BetterModelNativeDisplayAssetAdapter`, nuevo `DeferredRenderInstance` y su test.
- Boundaries: Core BetterModel imports = 0; Core Bukkit imports = 0; native runtime `DummyTracker|RenderedBone` refs = 0.
- Hitbox hard-collision audit: no se reutiliza el FastNMS collider actual porque no reproduce owner/passenger/sibling exclusion ni `entityInside/lava/block effects`; queda explícitamente en el tramo final, sin falso positivo.
- `PatchRebuildExactGate`: PASS desde el checkpoint oficial ~95; ZIP determinista doble + `unzip -t`: PASS.
- Full Gradle sigue bloqueado antes de compilar por `UnknownHostException: services.gradle.org`; Java local 21.0.11 vs target Java 25. No JAR/Canvas smoke reclamado.
- Clasificación: ~98% source/runtime/harness; ~2% concentrado en physical hitbox-body parity, BetterModel-free import/bootstrap y release build/smoke.

## Milestone cerrado — Cierre VsRender 98→100 en VirgolandStudio

### Especificación
Cerrar el 2% final desde el checkpoint 98 verificado, trabajando en `DaniDevz/VirgolandStudio` y usando su runner self-hosted Linux/X64 para build real con Java 25/Gradle.

### Plan
- [x] Verificar checkpoint 98 por SHA-256 y `unzip -t`.
- [x] Confirmar repositorio destino `DaniDevz/VirgolandStudio` y runner self-hosted Linux/X64 existente.
- [x] Importar el checkpoint 98 en una rama aislada de VirgolandStudio sin tocar Narrative; gate tracked-tree exacto y recovery de fuentes ignoradas verificados.
- [x] Implementar physical hitbox-body owner-aware con backend NMS 26.2, AABB exacta, exclusión owner/passenger/sibling/mount, push delegado y forwarding de entityInside/lava.
- [x] Desacoplar bootstrap/runtime obligatorio de BetterModel conservando compatibilidad/import opcional.
- [x] Ejecutar Java 25 + Gradle + Core/Bukkit/compatibility/native26/build en el self-hosted runner y revisar logs reales.
- [x] Ejecutar smoke standalone Paper 26.2 sin BetterModel instalado hasta alcanzar `Done`.
- [x] Actualizar VALIDATION y esta revisión con evidencia real.
- [x] Publicar release final con JAR, source ZIP, SHA256, README y log de smoke.
- [ ] Smoke físico/manual específico en CanvasMC 26.2 con jugador real: QA de plataforma posterior, no bloqueante del cierre de source/runtime/release.

### Revisión final — 100% source/runtime/release
- Checkpoint 98 reconstruido en runner y comparado contra manifiesto: 3189/3189, 0 missing, 0 extra, 0 changed/mode.
- Import bug por `.gitignore` anidado corregido con recovery + `git add -f`; 32 fuentes omitidas quedaron trackeadas.
- Java 25.0.4 / Gradle 9.7.1 operativos en `simplementejose`.
- Core, Bukkit y BetterModel-compatibility tests: PASS.
- `:bukkit:native26:compileJava`: PASS con Paper dev bundle 26.2.
- Standalone boundary gate: PASS; Core/runtime nativo sin BetterModel tracker legacy y BetterModel declarado opcional.
- Physical hitbox backend 26.2 empaquetado dentro del JAR final.
- Smoke real Paper 26.2 sin BetterModel: PASS hasta `Done`; durante el cierre se detectaron y corrigieron bindings proxy incompatibles con 26.2 (`getAttachments` y movement block accessor).
- `:bukkit:paper-loader:shadowJar`: PASS.
- Release validada: `vsrender-final-36bd820c2be2-37647338953`.
- JAR de release: `vsrender-paper-plugin-26.9.2-vsrender.6.jar`.
- SHA-256 del JAR construido en la corrida final: `abc0a23ebb645bcb33bdb202b415614333da77be43eeb27e2af0f507ec13c1b5`.
- La release contiene JAR + source ZIP + checksums + README + log de smoke.
- Workflows temporales de bootstrap/CI intermedia eliminados; queda sólo la CI final de VsRender.

## Cierre documental y limpieza final — 2026-10-07

### Plan
- [x] Confirmar que el run final `37647338953` terminó verde sobre el commit `36bd820c2be2b1bf3b1ba3524ca5a0dec9f89183`.
- [x] Confirmar que el único workflow VsRender trackeado es `.github/workflows/vsrender-final-ci.yml`.
- [x] Auditar el diff contra `main` y verificar que no haya `target/`, `build/`, `runPaper/`, `.gradle/`, logs, dumps, backups ni workflows temporales trackeados.
- [x] Alinear `README-VSRENDER.md` y `VSR-VALIDATION.md` con la release final `26.9.2-vsrender.6`.
- [x] Marcar los documentos de checkpoints intermedios como históricos sin borrar su evidencia original.
- [x] Mantener separado el QA manual con jugador real en CanvasMC 26.2; no bloquea el cierre source/runtime/release ya validado.

### Revisión
- Rama auditada: `vsrender-final-100`.
- CI final: PASS, run `37647338953`, job `final-validation`.
- Commit validado por CI: `36bd820c2be2b1bf3b1ba3524ca5a0dec9f89183`.
- HEAD previo al cierre documental: `55b56e3c25274f74463e61a44dbc8997e3f561d7`.
- Release: `vsrender-final-36bd820c2be2-37647338953`.
- JAR: `vsrender-paper-plugin-26.9.2-vsrender.6.jar`.
- SHA-256 del JAR: `abc0a23ebb645bcb33bdb202b415614333da77be43eeb27e2af0f507ec13c1b5`.
- Limpieza de repositorio: PASS; no se detectó basura temporal trackeada en el diff de VsRender.
- Estado de cierre: **100% source/runtime/release**. Único trabajo posterior explícito: QA manual visual/físico con jugador real sobre CanvasMC 26.2.

## Promoción a main + artifact final — 2026-10-07

### Plan
- [x] Verificar que `vsrender-final-100` esté por delante de `main` y no detrás: 70 commits ahead / 0 behind antes de la promoción.
- [x] Hacer que `vsrender-final-ci.yml` valide `main` y la rama final sin checkout hardcodeado.
- [x] Añadir publicación de un GitHub Actions artifact con JAR, source ZIP, SHA-256, README de validación y log de smoke.
- [x] Evitar releases duplicadas desde ramas de validación: la GitHub Release final se publica sólo desde `main`.
- [x] Mergear el corte final a `main`: merge commit `dcbb2fadd8e7aea7c02f5cf9a196bc9256df27e0`.
- [x] Confirmar que el primer run self-hosted de `main` ejecutó Core/Bukkit/compat/native26/build/smoke correctamente.
- [x] Identificar el fallo de delivery: GitHub Actions rechazó `upload-artifact` por cuota de almacenamiento agotada; no fue un fallo de VsRender.
- [x] Replanificar delivery: generar un ZIP final en el runner y publicarlo como asset autoritativo de GitHub Release; mantener Actions artifact sólo como best-effort.
- [x] Confirmar el segundo run self-hosted verde y verificar el ZIP final + assets de release.

### Revisión
- Primer run en `main`: `37678283136`; Core/Bukkit/compatibility/native26/build/smoke PASS. Falló únicamente el storage de `actions/upload-artifact` por cuota agotada.
- Fallback mergeado a `main`: `7d312d13da69cc878af87e3d4e577225b7c299a0`.
- Run final: `37679375348`, job `final-validation`: **SUCCESS**.
- Core, Bukkit, BetterModel compatibility, Native 26.2 compile, boundaries, Paper plugin build y smoke standalone hasta `Done`: PASS.
- GitHub Release: `vsrender-final-7d312d13da69-37679375348`: publicada correctamente sobre el SHA de `main`.
- Artifact autoritativo generado por el runner: `VsRender-final-artifact-7d312d13da69.zip` (18,631,712 bytes), publicado como Release asset junto al JAR, source ZIP, SHA-256, README y smoke log.
- El intento de GitHub **Actions artifact** continúa rechazado por la cuota de almacenamiento; `fetch_workflow_run_artifacts` devuelve vacío. No se lo declara entregado.
- La limitación de cuota no afecta el JAR ni la release; el canal autoritativo de delivery es el Release asset mientras la cuota de Actions siga llena.

## Milestone — Native Content Pipeline v1

### Especificación
Implementar los primeros cuatro bloques posteriores al cierre de VsRender:
1. Importar directamente archivos Blockbench `.bbmodel` a contratos nativos VsRender, sin BetterModel en el camino de importación.
2. Definir un formato/cache propio de asset nativo con manifest versionado, hash de fuente y datos suficientes para reconstruir rig, clips, geometría y texturas.
3. Compilar la geometría nativa a archivos de resource pack de Minecraft de forma determinista.
4. Definir una representación común del mismo asset para ITEM, BLOCK, ENTITY, FURNITURE, PLAYER_ATTACHMENT y VEHICLE.

### Invariantes
- Core no importa BetterModel ni Bukkit para parsear `.bbmodel`, representar geometría, persistir cache o generar archivos de resource pack.
- El importer no inventa soporte: canales/rotaciones/formatos no representables fallan con error explícito.
- IDs de parts y modelos son deterministas y estables entre imports idénticos.
- La cache se invalida por versión de schema + SHA-256 del `.bbmodel`.
- Texturas embebidas `data:image/png;base64,...` se extraen; referencias externas quedan declaradas como tales y deben resolverse explícitamente.
- El resource pack se genera a un directorio destino aislado y nunca modifica el source `.bbmodel`.
- Un asset tiene una sola identidad y puede declarar múltiples contextos de uso sin duplicar rig/animaciones/geometría.
- BetterModel permanece sólo como compatibility/import fallback para contenido legacy.

### Plan
- [x] Añadir contratos `NativeRenderAsset`, geometría/cubos/caras/texturas y `RenderUsageContext`/representación común.
- [x] Implementar `BbModelImporter` con jerarquía, locators, cubos, texturas y animation tracks position/rotation/scale STEP/LINEAR.
- [x] Implementar codec/cache versionado `VsRenderNativeAssetCodec` + `VsRenderNativeAssetCache` con SHA-256.
- [x] Implementar `NativeResourcePackCompiler` que emita model JSON, item model definitions, texturas y manifest determinista.
- [x] Conectar el asset descriptivo con `CompiledRenderAsset` existente sin duplicar runtime.
- [x] Añadir tests de importer, cache invalidation, resource-pack output y multi-context representation.
- [x] Añadir CI de pull request para VsRender y ejecutar Core/Bukkit/compat/native26/build/smoke en runner self-hosted antes de merge.
- [x] Documentar formatos, límites soportados y layout `plugins/VsRender/native-assets`.
- [x] Revisar diff, evitar basura temporal y mergear a `main` sólo con gates verdes.

### Revisión
- Implementación funcional presente en PR #36 / rama `vsrender-native-content-pipeline`.
- Entrada opt-in: `resources/<pack>/blueprint/native/**/*.bbmodel`.
- ID derivado: `<namespace>:<ruta-relativa-sin-.bbmodel>`.
- Cache: `<data-folder>/native-assets/assets/v1/<asset+hash>/<source-sha256>/`.
- Provider first-party: `vsrender_native`, siempre registrado y sin BetterModel.
- Bloques y furniture exponen `native_model`; ENTITY/VEHICLE/PLAYER_ATTACHMENT quedan disponibles por el provider entity-owned para callers que construyan `RenderEntityInstanceRequest` con contexto explícito.
- Gate limpio pre-merge: run `37705177081`, job `final-validation`: **SUCCESS**.
- PR #36 mergeado a `main` en `4bce12058a44f17b5e17a7adadc3c74405b55e78`.
- Gate final sobre `main`: run `37705426978`: **SUCCESS**.
- Core, Bukkit, BetterModel compatibility, Native 26.2 compile, standalone boundaries, Paper shadow JAR y smoke Paper 26.2 hasta `Done`: PASS.
- Release final del milestone: `vsrender-final-4bce12058a44-37705426978`.
- Bundle autoritativo del runner: `VsRender-final-artifact-4bce12058a44.zip` (18,789,467 bytes), junto con JAR, source ZIP, SHA-256, README y smoke log.
- GitHub Actions artifact sigue sin almacenarse: el log reporta `Artifact storage quota has been hit` y el endpoint de artifacts devuelve vacío. No se declara como entregado; la GitHub Release es el canal autoritativo.
- Auditoría pre-merge: 42 archivos previstos, 0 temporales sospechosos y 0 rutas Narrative.
- Estado: **primeros cuatro puntos implementados, integrados en runtime/pack pipeline, mergeados y validados en main**.

## Milestone — VsRender Independence Cleanup

### Objetivo
Terminar VsRender como renderer first-party independiente: sin BetterModel/ModelEngine como motores paralelos y sin subsistemas de gameplay de CraftEngine que ya no pertenezcan al renderer.

### Reglas de alcance
- Mantener únicamente infraestructura que VsRender todavía necesita para render, rig/animation, hitboxes, mounts/attachments, resource-pack, protocolo/scheduler y bootstrap Bukkit/Canvas.
- Eliminar BetterModel y ModelEngine del runtime, build, config y registries.
- No tocar Narrative.
- No borrar código CraftEngine sólo por nombre de paquete: primero demostrar que no es dependencia transitiva del build/runtime.
- Rebranding profundo de paquetes Java sólo después de que el árbol funcional quede mínimo; evitar una mega-migración mecánica que oculte regresiones.
- Mantener licencia/atribución upstream obligatoria.

### Plan
- [x] Inventariar dependencias KEEP / MIGRATE / DELETE y registrar conteos.
- [x] Eliminar BetterModel: bootstrap, provider, adapters, elements, player animations, tests y Gradle deps.
- [x] Eliminar ModelEngine: provider, elements, utils, bootstrap/config y Gradle deps.
- [x] Remover rutas/configs legacy `better_model` / `model_engine` y cualquier fallback paralelo.
- [ ] Auditar subsistemas CraftEngine no usados por VsRender y eliminar sólo los que queden sin referencias de runtime/build.
- [ ] Reemplazar nombres/configs de producto visibles que todavía expongan CraftEngine por VsRender donde no sean namespaces internos vendorizados.
- [ ] Ejecutar Core/Bukkit/native26/build/boundary/smoke sin BetterModel/ModelEngine.
- [ ] Auditar diff: 0 Narrative, 0 temporales, 0 motores paralelos.
- [ ] Mergear a `main` sólo con gates verdes y documentar release final.

### Revisión
- BetterModel: adapters/provider/bootstrap/elements/tests/deps eliminados físicamente.
- ModelEngine: provider/elements/utils/deps/registro eliminados físicamente.
- Bridge legacy `ExternalModel / ModelProvider / RenderModelRegistry`: eliminado; los requests requieren provider explícito.
- Furniture legacy `blueprint|better_model|model_engine`: eliminado; `native_model` es la ruta first-party.
- Primer gate después de la limpieza: Core/Bukkit/compat/native26 PASS; boundary viejo falló porque todavía exigía BetterModel. Gate actualizado para exigir exactamente 0 motores externos.
- Pendiente: gate actualizado completo + audit final de branding/subsistemas + merge.
