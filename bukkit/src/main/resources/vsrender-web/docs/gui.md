# GUI y comandos

GUI Studio visual está pendiente, pero el comando permite crear y previsualizar una GUI básica.

## Crear una GUI
/vsr gui create <pack> <nombre> [filas]

Ejemplo:
/vsr gui create cocina mesa_preparacion 3

El primer argumento es el nombre de un pack existente y habilitado; el segundo es un identificador sin espacios (letras, números, guion o guion bajo). Las filas pueden ser de 1 a 6. Si omitís las filas, se crean 3.

No uses comillas para los nombres simples. Si escribís `/vsr gui create cocina`, el comando mostrará que falta el nombre, el uso correcto, un ejemplo y los packs disponibles.

## Abrir dentro del juego
/vsr gui open cocina mesa_preparacion

El pack y el nombre son obligatorios tanto para crear como para abrir.

## Importante
Esta GUI es una plantilla editable, no una estación de cocina ni un editor dinámico de recetas. Los archivos se crean dentro de `resources/<pack>/configuration/gui/`.
