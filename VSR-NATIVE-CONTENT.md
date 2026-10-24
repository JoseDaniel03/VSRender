# VsRender Native Content Pipeline v1

Estado: implementación first-party para los cuatro bloques posteriores al cierre del runtime VsRender.

## Flujo

```text
.bbmodel
  -> BbModelImporter
  -> NativeRenderAsset
  -> VsRenderNativeAssetCache
  -> NativeResourcePackCompiler
  -> CompiledRenderAsset
  -> BukkitNativeDisplayAsset
  -> host posicional o entity-owned
```

BetterModel no participa en este camino. Continúa disponible sólo como compatibilidad/import legacy.

## Entrada y asset ID

Cada pack puede reservar:

```text
<data-folder>/resources/<pack>/blueprint/native/
```

Sólo los `.bbmodel` dentro de ese subtree son importados por este pipeline.

Ejemplo:

```text
pack namespace: salvarea
blueprint/native/vehicles/train.bbmodel
=> asset: salvarea:vehicles/train
```

Los nombres deben ser resource locations Minecraft válidos en minúsculas.

## Formato nativo y cache

`NativeRenderAsset` contiene:

- identidad y SHA-256 de la fuente;
- resolución del proyecto y `java_block_version`;
- `RenderRig` provider-independent;
- clips de animación;
- cubos, caras y UV;
- texturas embebidas o resource locations externas;
- `NativeRenderRepresentation` con contextos de uso.

El codec `VsRenderNativeAssetCodec` usa manifest versionado. La cache es content-addressed:

```text
<data-folder>/native-assets/assets/
  v1/
    <asset-id-normalizado>_<hash-id>/
      <sha256-del-bbmodel>/
        manifest.json
        textures/*.png
```

Cambiar el `.bbmodel` produce otro SHA y fuerza una nueva importación. Un manifest corrupto o incompatible se reconstruye desde la fuente.

## Blockbench soportado en v1

Importación directa:

- groups;
- locators/null objects;
- cubes;
- faces + UV;
- inflate, shade, light emission y rescale;
- texturas PNG embebidas;
- referencias de textura externas explícitas;
- animaciones position / rotation / scale;
- interpolación LINEAR y STEP;
- LOOP / HOLD / ONCE;
- `java_block_version` para semántica de rotación.

No se degrada contenido silenciosamente. Expresiones no numéricas y curvas como Bezier/Catmull-Rom se rechazan en v1.

Para modelos con `java_block_version >= 1.21.11`, el compilador conserva la rotación moderna `{x,y,z}` cuando corresponde. Formatos anteriores no aceptan una rotación multi-axis que no puedan representar fielmente.

## Resource pack

La compilación genera exclusivamente dentro del subtree reservado `native/` del mismo pack:

```text
resourcepack/assets/<namespace>/
  models/native/<asset-isolado>/<part>.json
  items/native/<asset-isolado>/<part>.json
  textures/native/<asset-isolado>/textures/*.png
  vsrender/native/<asset-isolado>/manifest.json
```

El nombre físico del asset y de cada part incluye un sufijo hash estable para evitar colisiones después de sanitizar IDs.

La generación usa staging. Todos los `.bbmodel` se importan y compilan antes de reemplazar el subtree reservado del pack. Un fallo de importación no reemplaza el registry vivo.

## Representación común

`RenderUsageContext` define seis usos:

- `ITEM`
- `BLOCK`
- `ENTITY`
- `FURNITURE`
- `PLAYER_ATTACHMENT`
- `VEHICLE`

Los tres primeros contextos fijos/posicionales que no siguen una entidad usan el host posicional cuando corresponde; ENTITY, VEHICLE y PLAYER_ATTACHMENT usan el host entity-owned. El asset, rig y clips siguen siendo los mismos.

El provider first-party tiene ID:

```text
vsrender_native
```

Está registrado siempre. No depende de que BetterModel esté instalado.

## Block y Furniture

Se registró el elemento first-party:

```text
native_model
```

para Block Entity y Furniture. El campo `model` recibe el asset ID nativo, por ejemplo:

```text
salvarea:vehicles/train
```

Furniture conserva conditions, sight trace y tint layers a través de los contratos nativos existentes.

## ITEM

El pipeline genera item-model definitions por part para los Item Displays del renderer. El binding final de un gameplay Item a un asset/estado concreto pertenece a VSContenido; este milestone no duplica esa capa de gameplay.

## Seguridad de reload

`NativeRenderPackLoader` construye un registry temporal. Sólo después de importar todos los packs sustituye `NativeRenderAssetRegistry`.

Los archivos generados se limitan a la carpeta reservada `native/`; el loader no borra modelos/resource-pack manuales fuera de ella.

## Validación

Los gates requeridos antes de merge son:

- `:core:test`;
- `:bukkit:test`;
- `:bukkit:compatibility:test`;
- `:bukkit:native26:compileJava`;
- standalone boundary audit;
- `:bukkit:paper-loader:shadowJar`;
- smoke standalone Paper 26.2 hasta `Done`.

El QA visual manual con jugador real continúa siendo una validación de plataforma separada.


## Importación desde el editor web

El importador de paquetes de la web acepta los archivos `.bbmodel` de
`blueprint/native/` (con PNG relativos en esa carpeta) dentro del **mismo ZIP**
que `pack.yml`, los YAML y los recursos normales. Los valida con
`BbModelImporter` antes de instalar. Al hacer **Aplicar al juego**, la recarga
utiliza `NativeRenderPackLoader` y `NativeResourcePackCompiler` sobre ese
árbol. No se utiliza BetterModel ni un conversor del navegador. El editor no
crea de manera implícita los bindings de items ni las máquinas interactivas.
