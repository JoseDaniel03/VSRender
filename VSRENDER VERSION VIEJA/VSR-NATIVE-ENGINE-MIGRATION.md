# VsRender — migración a motor nativo

> **Documento histórico de migración:** las secciones siguientes describen checkpoints progresivos y conservan sus bloqueos originales como evidencia. Para el estado final usar `VSR-VALIDATION.md`: VsRender 26.9.2-vsrender.6 cerró source/runtime/release con BetterModel opcional y smoke Paper 26.2 verde el 2026-10-07.

## Objetivo
VsRender absorbe progresivamente las capacidades útiles de CraftEngine y BetterModel hasta que ambos queden como adaptadores opcionales, no como runtimes requeridos.

## Estado de este checkpoint

### Modelo
VsRender posee contratos Core propios (`RenderModel`, `RenderModelProvider`, `RenderModelReference`, `RenderModelRegistry`). `ExternalModel` y `ModelProvider` continúan como bridges legacy para no romper el fork mientras se migra.

### Rig, parts y locators
La topología es provider-independent mediante `RenderRig`, `RenderPart`, `RenderRigType`, `RenderRigReference`, `RenderRigProvider` y `RenderRigRegistry`. BetterModel traduce sus `ModelRenderer` a snapshots; los locators se convierten en `RenderPartKind.LOCATOR` y `RenderRig.attachmentPoints()`.

### Attachment runtime
Core ahora define:
- `RenderTransformSpace`;
- `RenderPartTransform`;
- `RenderAttachmentBinding`.

`BetterModelAttachmentBinding` resuelve el locator en el tracker y publica posición + rotación Euler como snapshot `INSTANCE_LOCAL`. Ningún `RenderedBone`/`Vector3f` de BetterModel cruza hacia Core.

Este checkpoint **no afirma todavía attachment físico completo**: falta un executor nativo que aplique de forma continua ese pose a una instancia/recurso hijo móvil y, donde corresponda, componga world transform/scale. La API ya deja de requerir BetterModel para consumir el socket.

### Animation runtime
VsRender posee `RenderAnimationKey`, `RenderAnimationMode`, `RenderAnimationSession` y ahora `RenderAnimationController` instance-scoped. `BetterModelAnimationController` es el único borde que llama físicamente `Tracker.animate()`/`stopAnimation()`.

`BetterModelPlayerAnimations` ya no implementa la reproducción directamente: crea el tracker temporal y delega la animación al controller nativo. `AnimationSessionBook` sigue siendo owner de replace/stop/completion y el fix de futures/shutdown se preserva.

### Render instances y factories
`RenderInstance` expone capabilities opcionales (`attachment`, `animations`) con defaults vacíos para mantener compatibilidad con implementaciones existentes.

Además Core incorpora:
- `RenderInstanceRequest`;
- `RenderInstanceProvider`;
- `RenderInstanceRegistry`.

`CompatibilityManager` registra automáticamente la capability cuando un model provider también implementa `RenderInstanceProvider`. `BetterModelProvider` materializa el tracker físico detrás de ese contrato.

El camino de bloques ya es:

`BetterModelBlockEntityElement -> RenderInstanceRequest -> RenderInstanceRegistry -> BetterModelProvider -> BetterModelRenderInstance`

Por eso `BetterModelBlockEntityElement` tiene 0 imports/llamadas a la API BetterModel. El provider externo sigue siendo backend, pero dejó de ser API de consumo del bloque.

### Furniture
Furniture ya consume `RenderInstance` para lifecycle, pero su creación todavía usa BetterModel porque profiles/skins y tint pre-update siguen siendo provider-specific. No se migran a opciones opacas: el siguiente paso es definir `RenderProfile`/skin y tint layer nativos y recién entonces pasar furniture al factory común.

### Lifecycle y shutdown
Se conserva el fix Canvas/Folia: `BetterModelPlayerAnimations` es `AutoCloseable`, `BukkitCraftEngine` lo cierra explícitamente, `PendingAnimationResults` usa `PendingFutureRegistry` race-safe y todo pending future termina al shutdown.

## Verificación de este checkpoint
- attachment + animation runtime adapter harness: PASS;
- player animation integration (`INVALID_RESOURCE -> STARTED -> STOPPED -> shutdown -> UNAVAILABLE`): PASS;
- RenderInstanceRegistry explicit/fallback: PASS;
- BetterModelProvider + block element compile-shape: PASS;
- regresiones PendingFutureRegistry/AnimationSessionBook/ModelRegistry/PartSelector/RigRegistry: PASS;
- Core: 0 imports `kr.toxicity`;
- llamadas físicas `animate/stopAnimation`: sólo `BetterModelAnimationController`;
- block element: 0 BetterModel API;
- diff contra checkpoint anterior: 0 archivos eliminados.

El build Gradle completo continúa bloqueado porque el wrapper requiere Gradle 9.7.1 y el entorno no resuelve `services.gradle.org`. Por lo tanto no se recicla ni etiqueta ningún JAR anterior como si contuviera estos cambios. Smoke real CanvasMC sigue pendiente.

## Próximo bloque
1. Definir profile/skin y tint runtime nativos y migrar furniture al `RenderInstanceRegistry`.
2. Añadir executor de attachment para conducir una child instance/recurso mediante `RenderAttachmentBinding`.
3. Generalizar la factory para entity/player instances y mover entity animation al mismo `RenderAnimationController`.
4. Empezar a reemplazar físicamente el tracker BetterModel por backend VsRender propio, manteniendo el adapter como fallback temporal.

## Avance posterior — profile/tint, attachment executor y animation data
Furniture ya usa `RenderProfile`, `RenderTintLayer` y `RenderInstanceOptions` y crea por `RenderInstanceRegistry`; `BetterModelFurnitureElement` no toca API BetterModel/Bukkit para materializar el modelo.

Core incorpora `RenderAttachmentExecutor`: el owner de plataforma llama `tick()`, el executor lee el binding y entrega el pose sólo si el target soporta su `RenderTransformSpace`. No se inventa conversión `INSTANCE_LOCAL -> WORLD`; por eso el child-target físico permanece pendiente.

Se agregó el primer backend lógico de animación propio (`RenderAnimationClip`, `RenderAnimationTrack`, `RenderAnimationFrame`, `RenderBonePose`, `RenderVec3`, `RenderAnimationSampler`). VsRender ya puede samplear STEP/LINEAR y conservar ONCE/LOOP/HOLD.

`BetterModelAnimationClipAdapter` traduce `BlueprintAnimation` 3.5.0 ya procesado a estos clips nativos. `BetterModelModel` implementa `AnimatedRenderModel` y expone la biblioteca de clips nativa. Esto mueve datos y matemática base a VsRender, aunque el tracker/packet renderer físico siga perteneciendo temporalmente a BetterModel.

## Avance posterior — playback nativo + rest pose / rig resolver
VsRender ya no conserva sólo nombres de animación: `RenderAnimationPlayback` posee el clock lógico ONCE/LOOP/HOLD y `RenderAnimationPlayer` aplica samples a un target sin scheduler propio. `RenderAnimationSample` preserva `globalRotation`, evitando perder la semántica parent/local al salir de BetterModel.

El rig nativo ahora contiene `restPose` por part. El adapter BetterModel traduce `RendererGroup.position` y la rotación base a una pose local parent-relative. `RenderRigPoseResolver` compone rest pose + sample animado usando quaternion ZYX y produce `RenderResolvedPose` en instance space. La matemática parent/child relevante deja de depender de `RenderedBone`.

Esto todavía no sustituye el renderer físico de displays/packets. El próximo corte correcto es hacer que un backend nativo de VsRender consuma `RenderResolvedPose` para materializar/actualizar Item Displays usando la infraestructura de packets ya presente en el fork de CraftEngine. Hasta que ese backend exista y pase smoke visual, BetterModel sigue siendo fallback físico.

## Avance posterior — compiled asset / indexed runtime
VsRender incorpora una etapa explícita de compilación runtime. `RenderRig` y `RenderAnimationClip` siguen siendo contratos descriptivos; `CompiledRenderAsset` los convierte a `IndexedRenderRig` e `IndexedAnimationClip` antes de ejecutar.

El rig compilado usa índices parent-before-child, arrays primitivos y rest quaternion precomputado. Los clips almacenan sólo tracks existentes y apuntan directamente a `partIndex`. `RenderAnimationBuffer` y `RenderPoseBuffer` son reutilizables por instancia; el último incluye dirty mask para que el futuro renderer pueda omitir transforms sin cambios.

`CompiledRenderState` concentra el estado mutable mínimo por instancia: active clip, mode/time y los dos buffers. Los assets pesados quedan compartidos. El lookup por string ocurre en `play(animationId)`; `tick(delta)` no necesita maps ni tipos externos.

El pipeline indexado fue comparado contra el resolver descriptivo para ONCE/LOOP/HOLD, jerarquía, rest pose, parent scale/rotation y `globalRotation`. Los resultados coinciden dentro del epsilon del runtime. En un micro-harness local de 64 bones/16 tracks se observó aproximadamente 2.9–3.1x de speedup; es evidencia orientativa y no reemplaza benchmark JMH ni smoke CanvasMC.

El siguiente backend físico debe leer `RenderPoseBuffer` y enviar solamente índices dirty, habilitando packet batching, culling y LOD sin reintroducir búsquedas por nombre.
