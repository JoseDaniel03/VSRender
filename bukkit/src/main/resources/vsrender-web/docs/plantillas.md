# Plantillas de ítems

## Ingrediente
Permite configurar nombre, ID, material, PNG opcional, descripción y apilamiento. Se identifica visualmente con el lore **Ingrediente**. No genera automáticamente efectos ni recetas.

## Alimento
Produce un ítem consumible con `data.food`: nutrición, saturación y consumo con el hambre llena opcional. El componente `minecraft:consumable` controla duración, animación y hasta dos efectos de estado con nivel, segundos y probabilidad independientes.

## Bebida
Usa los componentes del alimento con animación de beber. Puede configurar recipiente de retorno mediante `data.use_remainder` con un material existente (por ejemplo, una botella vacía). Verificá el comportamiento del recipiente dentro del juego.

## Plato genérico
Elegí sopa, ensalada, guiso, estofado o tarta. La selección rellena automáticamente el nombre, ID, material y recipiente sugeridos; todos son editables. Se genera **un ID por tipo de plato**, no uno por combinación de ingredientes. La categoría queda visible en el lore. La estación de cocina para composición dinámica sigue pendiente.

## Apilamiento
El componente `data.max_stack_size` del ítem se escribe explícitamente para respetar el valor elegido, incluso cuando el material vanilla tiene otro límite (1–99). Minecraft puede tener particularidades para recipientes apilados: probalo dentro del juego.

## Texturas y vistas previas
El campo `texture` acepta `namespace:item/mi_comida`. Su PNG debe estar en `resources/<pack>/resourcepack/assets/<namespace>/textures/item/mi_comida.png`.

Desde el formulario podés elegir un PNG existente o subir otro (máximo 500 KB, 1024 × 1024). El administrador busca `texture`, `textures`, `model`, `models`, `legacy_model`, capas generadas y archivos JSON del resource pack. Para un material vanilla intenta usar imágenes incluidas en los packs locales. Si no existe el PNG, muestra un icono alternativo identificado por el material. No simula el renderizador 3D de Minecraft ni descarga sprites externos.

## Componentes
Los formularios usan el sistema de ítems existente de VSRender, sin motores Java separados. El YAML de consumo combina `data.food`, `data.components.minecraft:consumable` y, si se requiere, `data.use_remainder`. Los efectos se expresan en ticks para Minecraft y niveles con amplificador de base cero.

## Cultivos
La plantilla Verdura cultivable crea producto, semillas y bloque con estados de crecimiento y loot en un único YAML, reutilizando `crop_block` y `bush_block`. Admite 2–8 etapas, velocidad y texturas; hasta que se cambie su apariencia se ve como trigo vanilla.

## Aplicar los cambios
Guardá el ítem, aplicá la recarga de VSRender y regenerá/enviá el resource pack si cambiaste su apariencia. Probá los ítems consumibles y los efectos en el servidor antes de usarlos en una partida importante.
