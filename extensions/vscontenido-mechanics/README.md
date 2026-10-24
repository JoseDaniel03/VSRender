# VSContenido Mechanics — extensión interna de VSRender

Biblioteca Java **compilada y embebida** en la JAR existente de VSRender. No es un plugin, no registra listeners ni crea resource packs, HUD, interfaces, modelos o motores de renderizado.

## Procedencia

Fuentes autorizadas de `DaniDevz/VirgolandStudio/modules/items/items-api` y `modules/items/items-core` (repositorios del mismo propietario). Las clases trasladadas conservan los paquetes `com.virgolandstudio.items` para mantener contratos de comportamiento. El repositorio de origen no se modifica.

## Implementado en esta tanda

- `FireControlRuntime` y `CompiledFireControl`: semi/ráfaga/auto y cadencia basada en ticks sin un scheduler por arma.
- `AmmoFeedRuntime`, `AmmoContainerState` y `AmmoExtractionRuntime`: munición, recámara, depósitos internos/cilindros, carga y extracción.
- `CompiledSpread`, `CompiledRecoil`, `SpreadSampler`, `CompiledCurve` y `CurveCompiler`: precisión, retroceso y curvas.
- `AttachmentLoadout`, `AttachmentLoadoutCodec`, `AccessoryRegistry`, `AccessoryModifierSet`: ranuras, definiciones y bonificaciones de accesorios sin interfaz.
- `MagazineRegistry`, `MagazineInstance`, `MagazineBinaryCodec`, `AmmoContainerBinaryCodec`: cargadores y munición con persistencia binaria independiente de Bukkit.
- `ItemInputAction`: FIRE, RELOAD, FIRE_MODE, INSPECT, etc. El hardware/teclas pertenece a VSClient.

No se trasladan `items-paper`, `items-blocks`, `models`, `sessions`, `commerce` ni GUIs.

## Encaje y límite actual

```text
VSClient -> [adaptador Paper pendiente] -> VSRender
             -> VSContenido Mechanics (esta librería)
             -> ítems, proyectiles y render nativos de VSRender
```

Esta tanda instala **lógica compartida real y tests** en VSRender; **no activa todavía disparo funcional para FARO-12**. El adaptador Paper pendiente debe recibir acciones del VSClient Bridge, vincular el estado del objeto VSRender y despachar daño/sonidos/animaciones. No se crea una JAR separada del servidor.

## Pruebas

```sh
./gradlew :extensions:vscontenido-mechanics:test
./gradlew :bukkit:paper-loader:shadowJar
```
