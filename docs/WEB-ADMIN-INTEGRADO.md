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
