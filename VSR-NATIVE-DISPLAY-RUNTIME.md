# VsRender Native Display Runtime — checkpoint

> **Documento histórico de checkpoint:** conserva la evidencia y limitaciones del corte en que fue escrito. El estado actual autoritativo es `VSR-VALIDATION.md`: la CI final, el build Java 25/Gradle 9.7.1, el backend NMS 26.2 y el smoke standalone sin BetterModel quedaron validados el 2026-10-07.

Este milestone conecta el runtime compilado con un borde físico de Display sin devolver ownership a BetterModel.

## Pipeline

`CompiledRenderState -> RenderPoseBuffer -> RenderFrameBatch -> BukkitDisplayPoseSink -> Display metadata packets`

## Hot path

- Los clips y rigs ya están indexados.
- `CompiledRenderRuntime` no hace lookups por nombre por tick.
- La primera publicación fuerza todos los parts.
- Las siguientes publicaciones incluyen sólo poses dirty.
- El batch es reutilizable y se consume sincrónicamente.

## LOD / culling

La política por defecto usa intervalos 1/2/4/8 ticks y culling total fuera de 128 bloques. Mientras una instancia está culled no se samplea el rig. El delta se acumula y se aplica en una sola evaluación al volver a ser visible; esa restauración fuerza un frame completo para evitar estado cliente incompleto.

## Borde Bukkit

`BukkitDisplayPoseSink` reutiliza infraestructura ya presente en CraftEngine:

- `DisplayData.Translation`
- `DisplayData.Scale`
- `DisplayData.LeftRotation`
- `TransformationInterpolationDuration`
- `ClientboundSetEntityDataPacket`
- `Player.sendPackets(...)` / bundle packet del network manager

Este checkpoint sólo toma ownership del **transform físico**. Spawn/despawn y payload visual por bone (item/model generado) se mantienen para el siguiente milestone; no se inventa un formato opaco para esos datos.

## Validación local

- Native display runtime harness: PASS.
- LOD cadence harness: PASS (20/11/6/3/0 frames en 20 ticks para near/mid/far/very-far/culled).
- Bukkit display pose sink harness: PASS.
- Indexed runtime regression: PASS.
- Compiled state regression: PASS.
- Dirty mask regression: PASS.
- `javac -Xlint:all` sobre el runtime puro: PASS.

El build Gradle completo y smoke CanvasMC siguen bloqueados por disponibilidad de Gradle 9.7.1/red en este entorno, por lo que no se publica un JAR nuevo como si estuviera verificado.

## Extensión: lifecycle físico + import estático BetterModel

El siguiente corte ya toma ownership de `spawn/update/despawn` para el subconjunto estático seguro.

Pipeline:

`BetterModel ModelRenderer (import-time only) -> CompiledRenderAsset + BukkitDisplayPartPayload -> BukkitNativeDisplayInstance -> Item Display packets`

Reglas de migración automática actuales:

- modelo sin animaciones;
- `sightTrace = false`;
- sin profile ni tint layers;
- anchor con pitch/yaw cero;
- cada part visual usa `BoneItemMapper.EMPTY`;
- part visible por defecto;
- item representable como `BukkitItemStack`.

Si una sola condición no se cumple, `BetterModelProvider` usa el backend BetterModel existente. No hay migración parcial que pueda dejar un modelo visualmente incompleto.

`BukkitNativeDisplayInstance` asigna entity IDs sólo a parts visuales. Locators/null/dummy parts siguen participando del rig y dirty propagation, pero no generan entidades. El payload visual mantiene `TransformedItemStack.position/offset/scale`; el sink compone ese transform con la pose resuelta antes de construir metadata.

La orientación de anchor distinta de cero sigue deliberadamente en fallback hasta implementar y validar la composición pitch/yaw nativa contra BetterModel. Esto evita activar furniture rotada con una equivalencia no demostrada.

## Extensión: anchor rotation + host regional + animación nativa

Este corte habilita la misma ruta física nativa para modelos context-free con clips de animación y para anchors con pitch/yaw distintos de cero.

Pipeline runtime:

`BetterModel (import-time) -> CompiledRenderAsset -> BukkitNativeRenderInstance -> RegionScheduler -> CompiledRenderRuntime -> dirty Display packets`

Propiedades:

- `WorldPosition.xRot/yRot` se envían como rotación de entidad en el spawn del Item Display; el quaternion de cada bone continúa separado en `DisplayData.LeftRotation`.
- `CompiledRenderAnimationController` vive en Core y expone `RenderAnimationController/RenderAnimationSession` sin tipos Bukkit/BetterModel.
- ONCE notifica finalización exactamente una vez; LOOP y HOLD permanecen hasta close.
- `BukkitNativeRenderInstance` usa el `PlatformExecutor` regional existente a 1 tick de delay / 1 tick de período, sobre el chunk owner del anchor.
- Assets sin clips no crean repeating task.
- Assets animables con runtime idle/completed/holding no resamplean poses; el task regional queda barato.
- `stop()` fuerza una publicación de rest pose antes del siguiente sampling para no perder el dirty transition.
- `close()` cancela task, cierra controller/session y remueve sólo los Displays de la instancia; la carrera tick/close está serializada por ownership de lifecycle.

Reglas de native route BetterModel después de este corte:

- clips animados: permitidos;
- anchor pitch/yaw: permitido;
- `sightTrace`: fallback;
- profile dinámico: fallback;
- tint layers: fallback;
- `BoneItemMapper` dinámico, part oculta o payload no representable: fallback completo desde el adapter.

La animación física ya no usa `DummyTracker`/`RenderedBone` para este subconjunto. BetterModel queda como importador de rig/clip/payload durante la creación del asset.
