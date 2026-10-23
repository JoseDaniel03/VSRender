# Catálogo de materiales vanilla para la web de VSRender

El plugin exporta al iniciar `plugins/VsRender/content-data/minecraft-materials.json`.

- Origen: `org.bukkit.Material.values()`, filtrando `isItem()` y `!isLegacy()`.
- Contenido: `schema_version`, `minecraft_version` y `materials` ordenados por ID.
- Cada entrada: `id` (ej. `minecraft:stone`), `block` y `max_stack_size`.
- No hay una lista fija de materiales dentro de la web ni peticiones a sitios externos.
- La web local debe leer **solo** ese archivo y ofrecer búsqueda; la escritura de YAML
  sigue dentro de `resources/<pack>/configuration`.
- El archivo es informativo, no una API HTTP ni un editor gráfico incorporado en Java.
- Para refrescar el catálogo tras actualizar Minecraft, reiniciar el servidor para
  volver a exportarlo; la web puede recargar el JSON sin reiniciar.
- En modo demo se muestra una muestra explícita. **Nunca** se confunde con el
  catálogo completo del servidor.

El nombre de archivo y contrato `schema_version=1` son estables. El frontend debe
dar un mensaje claro si falta, no utilizar una lista antigua silenciosamente.
Referencia: https://jd.papermc.io/paper/26.1.1/org/bukkit/Material.html
