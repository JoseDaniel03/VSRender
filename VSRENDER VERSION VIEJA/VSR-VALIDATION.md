# Validación VsRender 26.9.2-vsrender.6 — cierre final

> **Estado autoritativo:** el cierre final está documentado en `VsRender standalone final 98→100 — 2026-10-07`. Las secciones anteriores se conservan como evidencia histórica de cada checkpoint; sus menciones a Gradle bloqueado, BetterModel requerido o smoke pendiente describen el estado de ese momento y quedaron superadas por la validación final.

CanvasMC 26.2 build962 y Java25. Cliente basado en DaniDevz/VirgolandStudio main 08522953c9f916b69dfec2bc93289287061758ae; cambios locales incluidos en fuentes-client, sin publicar en main.

## Builds y pruebas
- Cliente Fabric completo: :fabric:build exit0, verifyRuntimeJar y 405 tests sin fallos. Incluye UI, medios, cámara y el renderer nuevo.
- Servidor: :bukkit:paper-loader:shadowJar :core:test :bukkit:test exit0; 14 tests core. Se reutilizaron clases sin cambios de compatibility y compatibility:legacy mediante exclusión de sus tareas compileJava. La primera verificación quedó esperando dependencias externas de esas integraciones; la verificación final usó un checkout/caché de build separado. No se afirma recompilación de esas integraciones ni prueba contra sus plugins externos.
- El entorno bloquea Unix-domain sockets: la herramienta Loom temporal informa que no los soporta. Ese ajuste afecta sólo el build y no entra en el mod. Loom se fijó a 1.17.21. Java25 confía en el certificado del proxy de build.
- Test nuevo de ajuste de altura/profundidad observado fallando antes de corregirlo; suite completa del cliente verde después. ShelfLayout observado con cuatro fallos antes de implementarse.
- Geometría/configuración: dos tests de muebles, verificador de estatua y verificador de estantería.

## Smoke final de servidor
Dos arranques/apagados del mismo mundo: todas las marcas NativeProbe, StatueProbe y ShelfProbe PASS, y las tres marcas RESTART_PASS en el segundo arranque.

- Mesa conserva suelo; lámpara luminancia real 0/15; silla; sillón conexiones, esquinas y cuatro direcciones de colocación.
- Estatua:15 piezas, cuatro orientaciones, obstrucción/veto de construcción/altura, retirada y un drop, vecino y piedra de reemplazo intactos.
- Estantería:15 piezas + soporte invisible; un ítem por intercambio; nombre/componentes conservados; modos parado/según tamaño/acostado; anchura grande impide ocupar posición vecina; rechazo no consume inventario.
- Snapshot real codificado por ShelfManager y leído por ShelfWire del cliente: dimensión, posición e ítem coinciden.
- Retirar soporte con contenido produce un único drop de ese contenido. Representación sin entidades; los drops vanilla son entidades normales.
- Contenido de estantería restaurado tras reinicio.
- Pack ZIP válido,38 modelos VsRender y96 variantes visuales de hojas. Modelos de estantería/soporte/caja coinciden con los archivos fuente. Las apariencias repetidas del pedestal y de los pisos se comparten; actualizar desde .4 exige regenerar el caché visual y pack como explica README.

## Alcance pendiente de validación manual
No se abrió un cliente gráfico ni se certifica el resultado visual/GPU. El renderer y el mod completo compilan; las pruebas cubren protocolo, caché, transformaciones y lógica de servidor. ShelfProbe usa Player/Inventory controlados y llama al gestor real; no reproduce un usuario con ratón ni certifica todas las protecciones externas. Los eventos de cancelación de colocación con jugador real siguen fuera del probe anterior. Colisión/selección: cubo completo por celda. Tamaño: perfiles de anchura y ajuste del modelo; no reconocimiento automático autoritativo de modelos arbitrarios en el servidor.

## Native damage/jump event interrupt v1
- `damage` y `jump` full-rig con `override=false` pueden ejecutarse en runtime nativo como interrupts ONCE.
- LOOP/HOLD suspendidos reinician desde 0 al terminar el evento; ONCE suspendida finaliza exactamente una vez.
- `damage/jump override=true` y `death` continúan en BetterModel fallback.
- `play()` nuevo invalida cualquier rest-frame pendiente dejado por `stop()`.
- Event routing usa Paper `EntityJumpEvent`/`PlayerJumpEvent` y Bukkit `EntityDamageEvent` a prioridad MONITOR, ignoreCancelled.

## Native death terminal + per-bone priority foundation (~85% checkpoint)
- `death override=false` puede entrar por el runtime nativo y usa ownership terminal: no restaura playback base y cierra el host al completar.
- Death progresa incluso con 0 viewers mediante avance culled explícito; el culling normal no cambió.
- `EntityDeathEvent` se enruta al registry por UUID; damage/jump se ignoran una vez iniciado death.
- Owner inválido/muerto sin terminal soportado usa `close()` y cancela el repeating task; `retired()` queda reservado al callback de retiro del scheduler.
- `death override=true` continúa en BetterModel fallback.
- Se añadió `IndexedAnimationLayerStack`: prioridad descendente, inserción más reciente, máscara de subtree para `override=true` y clocks/preemption independientes por part.
- El layer stack todavía no está conectado al controller productivo, por lo que `damage/jump override=true` siguen en fallback y no se afirma paridad antes de ese wiring.
- Gates locales: DeathGate PASS; DeathRaceGate PASS x2000+x2000; LayerStackGate PASS; Core render javac PASS; boundaries BetterModel/Bukkit en Core 0.
- Gradle/JAR/Canvas smoke continúan bloqueados por `UnknownHostException: services.gradle.org` al intentar descargar Gradle 9.7.1.

## Native per-bone override runtime
- `IndexedAnimationLayerStack` ya está conectado al `CompiledRenderState`/`CompiledRenderRuntime` y al controller productivo; no existe un segundo pose buffer ni un segundo clock concurrente.
- Playback simple y playback layered son modos mutuamente excluyentes.
- Capas base/transient/terminal reproducen prioridad e inserción de BetterModel y permiten que un override reclame tracks + subtree sin detener parts no afectados.
- Remover una capa resuelve de inmediato la capa inferior/rest con delta 0; dirty propagation se limita a parts cuyo resultado cambió.
- `damage`, `jump` y `death` con `override=true` dejan de requerir fallback BetterModel por esa razón. Otros blockers del provider (sight trace/profile/tint/platform support) siguen aplicándose independientemente.
- `OverrideControllerGate`: PASS con base LOOP, reemplazo damage→jump, parts no afectadas, restauración dirty y death terminal override con 0 viewers.
- `RuntimeRegressionGate`, `LayerStackGate`, `DeathGate` y `DeathRaceGate x2000+x2000`: PASS.
- Core render `javac -Xlint:all`: PASS; boundaries BetterModel/Bukkit Core = 0 y runtime nativo legacy refs = 0.
- Gradle completo sigue bloqueado por DNS al descargar Gradle 9.7.1; no se afirma JAR nuevo ni smoke standalone.

## Native hitbox / nametag / sight-trace validation — 2026-10-06

PASS:
- `HitBoxGeometryGate`
- `GameplayCadenceGate`
- `NameTagMetadataGate`
- `NameTagVisibilityGate`
- `SightVisibilityGate`
- `MountPolicyGate` (contract/import foundation only)
- `RuntimeRegressionGate`
- `LayerStackGate`
- `OverrideControllerGate`
- `DeathGate`
- `DeathRaceGate` x2000 close + x2000 damage
- Java parser gate for touched Bukkit/compat sources
- Core BetterModel imports: 0
- Core Bukkit imports: 0
- native runtime `DummyTracker|RenderedBone`: 0

Not claimed:
- Native mount controller runtime (WALK/FLY/passenger lifecycle)
- Projectile interception / full vanilla collision parity for native hitboxes
- Gradle full build / new JAR / Canvas standalone smoke (toolchain download remains external gate)

## Native static-profile async bridge + final source/runtime parity — ~98% checkpoint
- Static MODEL + external profile no longer requires `DummyTracker` on the normal representable path.
- `BetterModelNativeDisplayAssetAdapter.adaptStaticProfile` resolves `RenderSource.Dummy.completeContext()` asynchronously and compiles the resolved profile payload directly into the native asset.
- `DeferredRenderInstance` keeps viewer intent while the profile resolves and never blocks the caller/server thread.
- Deferred lifecycle race semantics: close-before-bind wins permanently; a late delegate is closed; viewer replay/hide is serialized against close so no callback occurs after delegate retirement.
- Profile completion materializes the native host on the target chunk/platform scheduler. Scheduler rejection or host construction failure retires the proxy instead of leaving it pending forever.
- Supported static-profile path is native; the legacy `DummyTracker` helper remains only for unsupported/adapter-error compatibility fallback.
- `DeferredRenderInstanceGate`: PASS x5000; source JUnit-equivalent `DeferredRenderInstanceJUnitGate`: PASS x1000.
- `StaticProfileNativeRouteGate`: PASS; no `join`/future blocking, native adapter + chunk scheduler + explicit exceptional fallback verified.
- `BetterModelProviderShapeGate`: PASS against boundary stubs.
- Re-run gates: MountCore PASS, MountBukkitShape current-source PASS, MountRegistry PASS, MountRegistryRace PASS x5000, ProjectileRegistry PASS, ProjectileInterceptor PASS, TintLayer PASS, AttachmentCore PASS, NativeAttachmentBinding PASS, CoreRegression PASS.
- Diff proof vs official ~95% checkpoint: runtime physical files are byte-identical; the 95→98 code delta is limited to the two BetterModel compatibility files plus `DeferredRenderInstance` and its test.
- Boundaries remain green: Core render BetterModel imports 0; Core render Bukkit imports 0; native Bukkit runtime `DummyTracker|RenderedBone` refs 0.
- Full physical hitbox body parity remains intentionally open: current native exact-AABB target/projectile behavior is not claimed to reproduce BetterModel push/collidable/entityInside/lava/block effects or owner/passenger/sibling exclusion in a hard NMS collider.
- External BetterModel import/profile bootstrap remains required. Removing that plugin/API requirement belongs to the standalone final tranche.
- Gradle re-attempt still fails before compilation with `UnknownHostException: services.gradle.org`; local Java is 21.0.11, target is Java 25. No rebuilt JAR/Canvas smoke is claimed.
- ~98% reproducibility: patch over official ~95% checkpoint rebuilds the complete tree exactly; normalized ZIP double-generation and `unzip -t` PASS.


## VsRender standalone final 98→100 — 2026-10-07

Estado: **100% source/runtime/release** sobre la rama `vsrender-final-100`.

Validación final en self-hosted `simplementejose`, Java 25.0.4 / Gradle 9.7.1:
- `:core:test`: PASS.
- `:bukkit:test`: PASS.
- `:bukkit:compatibility:test`: PASS.
- `:bukkit:native26:compileJava`: PASS.
- Standalone boundary audit: PASS.
- `:bukkit:paper-loader:shadowJar`: PASS.
- JAR: `vsrender-paper-plugin-26.9.2-vsrender.6.jar`.
- El JAR contiene `Native26RigPhysicalBody` y `BukkitNativeRigPhysicalBodyBridge`.
- SHA-256 observado en la corrida final: `abc0a23ebb645bcb33bdb202b415614333da77be43eeb27e2af0f507ec13c1b5`.
- Smoke standalone Paper 26.2 sin BetterModel instalado: PASS hasta `Done`.
- BetterModel queda como compatibilidad/import opcional; no es dependencia obligatoria del runtime nativo.

Cierre físico de hitboxes:
- backend NMS específico de 26.2;
- AABB exacta por hitbox;
- exclusión de owner, pasajeros, hitboxes hermanas y participantes del mount;
- push delegado al owner;
- efectos `entityInside` y lava reenviados al owner;
- interacción/daño/proyectiles siguen en la capa exact-AABB existente para evitar doble impacto.

Durante el smoke real se encontraron y corrigieron incompatibilidades heredadas del proxy de CraftEngine con 26.2, incluyendo `Entity#getAttachments()` y el accessor de movement-block. Estos fallos no se ocultaron: cada cambio se revalidó hasta obtener CI completa verde.

Release:
- tag: `vsrender-final-36bd820c2be2-37647338953`;
- incluye JAR, source ZIP, `VsRender-final.sha256`, `README-VsRender-final.md` y `vsrender-paper-26.2-smoke.log`.

Límite de validación restante: no se realizó todavía un recorrido manual con jugador real sobre CanvasMC 26.2. Ese QA visual/físico de plataforma queda separado del cierre de source/runtime/release y no implica código pendiente conocido.

## Native Content Pipeline v1 — 2026-10-07

Estado: **implementado y mergeado a main**.

Alcance cerrado:
- importación directa `.bbmodel -> NativeRenderAsset`, sin BetterModel;
- cache/codec propio versionado por schema + SHA-256;
- compilación determinista a resource pack nativo;
- representación común para ITEM, BLOCK, ENTITY, FURNITURE, PLAYER_ATTACHMENT y VEHICLE;
- provider first-party `vsrender_native`;
- elementos first-party `native_model` para Block y Furniture;
- reutilización de `CompiledRenderAsset` y hosts nativos existentes, sin segundo runtime.

Semántica Blockbench v1:
- groups, locators/null objects, cubes, faces/UV y texturas embebidas/externas;
- animación position/rotation/scale con STEP/LINEAR y modos ONCE/LOOP/HOLD;
- expresiones y curvas no representables fallan explícitamente;
- `java_block_version` se conserva en el asset/cache;
- desde 1.21.11 se conserva la rotación moderna `{x,y,z}`; formatos anteriores rechazan multi-axis no representable en lugar de degradarlo.

Persistencia y pack:
- fuente: `resources/<pack>/blueprint/native/**/*.bbmodel`;
- asset ID: `<namespace>:<ruta-relativa-sin-.bbmodel>`;
- cache: `<data-folder>/native-assets/assets/v1/.../<source-sha256>/`;
- salida reservada: `resourcepack/assets/<namespace>/**/native/`;
- el loader usa staging y reemplaza sólo el subtree reservado después de importar/compilar todas las fuentes.

Validación:
- PR #36.
- Gate pre-merge: run `37705177081`: SUCCESS.
- Merge a main: `4bce12058a44f17b5e17a7adadc3c74405b55e78`.
- Gate main: run `37705426978`: SUCCESS.
- `:core:test`: PASS.
- `:bukkit:test`: PASS.
- `:bukkit:compatibility:test`: PASS.
- `:bukkit:native26:compileJava`: PASS.
- standalone boundaries: PASS.
- `:bukkit:paper-loader:shadowJar`: PASS.
- smoke standalone Paper 26.2: PASS hasta `Done`.

Release:
- tag: `vsrender-final-4bce12058a44-37705426978`;
- bundle: `VsRender-final-artifact-4bce12058a44.zip`;
- JAR: `vsrender-paper-plugin-26.9.2-vsrender.6.jar`;
- incluye source ZIP, checksums, README y log de smoke.

Delivery note:
- el intento de `actions/upload-artifact` fue rechazado por cuota de almacenamiento de Actions;
- el step es best-effort y el job final siguió correctamente;
- el endpoint de Actions artifacts permanece vacío;
- la GitHub Release, verificada con todos sus assets, es el canal autoritativo.

QA manual visual/físico con jugador real sobre CanvasMC 26.2 continúa siendo una validación de plataforma separada y no un pendiente de implementación de estos cuatro puntos.
