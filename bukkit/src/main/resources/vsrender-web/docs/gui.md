# GUI Studio · Inventarios visuales

La sección **GUI Studio** del administrador web permite crear, editar y eliminar plantillas básicas de inventario compatibles con el comando `/vsr gui open`. No necesita una aplicación aparte.

## Editor
- Seleccioná un pack habilitado, elegí un ID y un título.
- Configurá entre 1 y 6 filas. Cada fila tiene 9 casillas.
- Seleccioná una casilla y asignale un ítem por su ID completo, por ejemplo `minecraft:emerald` o `cocina:plato`.
- Acción **Sin acción**: el botón está presente y no ejecuta comandos.
- Acción **Cerrar inventario**: al hacer clic se cierra la interfaz.
- Para quitar un botón, borrá su ID de ítem.
- Guardá los cambios y aplicá la recarga de VSRender.

Las GUIs se guardan en `resources/<pack>/configuration/gui/<id>.yml`. Al editar o eliminar, se realiza un respaldo previo en `.vsrender-web-backups`. Las plantillas existentes que usan ese formato también aparecen en la web.

## Abrir desde Minecraft
/vsr gui open cocina mesa_preparacion

Para crear por comando, también podés usar:
/vsr gui create cocina mesa_preparacion 3

El pack y el nombre son obligatorios; la cantidad de filas puede omitirse y por defecto es 3. Si falta algún argumento, la ayuda muestra un ejemplo y los packs disponibles.

## Alcance de la primera versión
GUI Studio utiliza `BasicGui` a través del comando ya existente, sin modificar el navegador de ítems ni el sistema de mochilas. Fondos personalizados, estados reactivos, acciones avanzadas y estaciones de cocina interactivas quedan para futuras tandas. No se ejecutan scripts arbitrarios desde la web.
