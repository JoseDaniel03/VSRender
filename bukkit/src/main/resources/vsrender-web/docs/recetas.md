# Crafting Studio · Recetas

Desde la sección **Crafting Studio** del administrador web podés crear, editar, eliminar y exportar recetas sin instalar nada nuevo.

## Tipos disponibles
- Mesa de trabajo con forma: seleccioná los ingredientes en una cuadrícula de 3 × 3. Dos casillas que contienen el mismo ID comparten ingrediente.
- Mesa de trabajo sin forma: hasta nueve ingredientes; el orden no importa.
- Horno, ahumador, alto horno y fogata: un ingrediente de entrada, tiempo de cocción en ticks y experiencia.
- Cortapiedras: un ingrediente de entrada y un resultado.

Usá IDs completos como `minecraft:carrot` o `cocina:tomate`. Los ítems personalizados deben existir en algún pack instalado y habilitado. El resultado también debe existir.

## Dónde se guardan
Las recetas creadas por el editor se escriben en `resources/<pack>/configuration/recipes/<id>.yml`, dentro de la sección `recipes:`. VSRender las procesa con sus serializadores nativos `shaped`, `shapeless`, `smelting`, `smoking`, `blasting`, `campfire_cooking` y `stonecutting`.

## Seguridad
Al editar o eliminar un YAML existente, la web genera primero una copia de seguridad en `.vsrender-web-backups`. No altera definiciones ajenas dentro del mismo archivo. Si otro proceso modificó el archivo desde que lo abriste, la web te pedirá actualizar antes de guardar.

## Aplicar en el juego
Guardá la receta y usá **Aplicar al juego** o `/vsr reload`. Esta primera edición no construye estaciones personalizadas: utiliza las mesas y procesadores de Minecraft y VSRender existentes.

## Funciones que quedan para otra etapa
Mesas de cocina con interfaces, ingredientes dinámicos y platos variables necesitan su propia integración con los comportamientos existentes. Crafting Studio no cambia la composición dinámica de alimentos ni crea un motor de recetas paralelo.
