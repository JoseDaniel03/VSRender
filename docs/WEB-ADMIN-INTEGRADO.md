# VSRender 26.3 — Administrador web integrado

La JAR contiene la web negra sin demo, con administración directa de los YAML reales.

## Activación
- Desde consola: `vsr web start`
- Desde Minecraft (con permisos): `/vsr web start`
- Estado: `/vsr web status`
- Detener: `/vsr web stop`

Usa `127.0.0.1:3050` y enlace privado con token. No debe exponerse a Internet. La web anterior en Python debe estar cerrada.

## Novedades
- Vista de tarjetas y lista, con previsualización de la **textura PNG real** del resource pack, si existe. El material vanilla se identifica mediante una ficha textual cuando no hay PNG; no se simula render 3D.
- Cuatro plantillas: ingrediente, alimento, bebida y plato. Cada una tiene formulario adecuado, incluyendo textura/lore; los consumibles tienen nutrición, saturación, duración, animación, efecto (nivel/duración/probabilidad), recipiente y permiso de comer con hambre llena.
- Las plantillas usan `data.food` y `data.components.minecraft:consumable` como datos existentes de Minecraft; no añaden un motor de comida.
- El YAML puede editarse desde la ficha avanzada, con respaldos antes de guardar.

## Vista previa del resource pack
Para que aparezca una imagen personalizada, en el YAML usar por ejemplo:

```yaml
texture: cocina:item/guiso
```

La imagen debe estar en `resources/<pack>/resourcepack/assets/cocina/textures/item/guiso.png`. También se buscan texturas en modelos JSON existentes y `model.generation.textures`.

## Comandos GUI
Con pack explícito: `/vsr gui create cocina mesa_preparacion 3` y `/vsr gui open cocina mesa_preparacion`. Se puede escribir solo el nombre si existe un único pack habilitado; de lo contrario el comando indica el formato.

## Recarga
Guardar modifica YAML en `plugins/VsRender/resources/`; la recarga del motor es independiente. El botón «Aplicar al juego» solicita recargar. Cambios de texturas/modelos pueden requerir regenerar y distribuir nuevamente el resource pack.

## Pendientes
No están implementados: ingredientes dinámicos por unidad de guiso, creación completa de cultivos desde formulario, Crafting Studio ni GUI Studio.

No se modifica el motor de shaders ni el render de Minecraft.

### Plantilla de cultivo y miniaturas
La web incluye Verdura cultivable (producto + semillas + bloque) usando YAML y comportamientos de cultivo ya existentes. Las miniaturas buscan la textura real tanto en el pack del ítem como en otros packs locales que contengan la referencia. La visualización sigue siendo 2D, no un renderer 3D de Minecraft.


## Importación de modelos Blockbench en un único ZIP

En **Packs → Importar pack ZIP**, además de las configuraciones y texturas,
se admite un árbol `blueprint/native/` con archivos `.bbmodel` y PNG
externos relativos al modelo. Por ejemplo:

```text
faro12/
  pack.yml
  configuration/items/armas.yml
  blueprint/native/escopeta.bbmodel
  blueprint/native/mesa/banco.bbmodel
  blueprint/native/mesa/paleta.png
  resourcepack/assets/faro12/textures/item/escopeta.png
```

Los `.bbmodel` se validan **antes** de instalar el pack: JSON, texturas,
geometría y animaciones compatibles con el importador nativo. No se aceptan
rutas absolutas, URI de archivos locales ni referencias a texturas fuera del
directorio nativo. Los archivos se instalan solo si la validación completa
tiene éxito. Si falla un modelo, el pack anterior no se modifica.

Tras importar el ZIP, seleccioná **Aplicar al juego** o ejecutá
`/vsr reload all`: el pipeline nativo lee los archivos
`blueprint/native/**/*.bbmodel`, genera los modelos, texturas y manifiestos
en el resource pack, y registra los rigs y clips. Distribuí después el
resource pack actualizado a los jugadores. La importación no crea
automáticamente mecánicas de ensamblaje, enlaces a ítems de gameplay ni
eventos de animación: esas asociaciones siguen requiriendo configuración.

Límites actuales del importador web: 8 MB de ZIP comprimido,
16 MB descomprimido, 2 MB por archivo y 256 entradas.


## Importar ZIP completo desde Creaciones

El botón **Creaciones → Importar ítem o ZIP** detecta un `pack.yml`
en la raíz o en la carpeta principal del ZIP y deriva al importador de packs.
Así no aplica el límite de 48 archivos del importador individual a los
paquetes completos. El límite para packs sigue siendo 256 entradas,
8 MiB comprimidos, 16 MiB extraídos y 2 MiB por archivo.

La sección **Vanilla** lista los materiales de Minecraft disponibles en el
servidor; **Creaciones** lista los ítems personalizados de los YAML.
Ambos catálogos cargan de forma independiente, de modo que una falla al
leer materiales vanilla o plantillas no oculta las creaciones guardadas.
Los errores al procesar YAML se indican con el archivo que falló.
