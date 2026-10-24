# Introducción
VSRender incluye una web real dentro de la JAR. No necesita Python ni modo demo.
## Iniciar
Desde consola: vsr web start. Dentro de Minecraft, con permisos: /vsr web start.
## Detener
Ejecutá /vsr web stop. La web se apaga automáticamente cuando se detiene VSRender.

## Importar paquetes completos (ZIP)
En la sección **Packs**, pulsá **Importar pack ZIP** para subir un pack como Armería.
El ZIP puede tener `pack.yml` en la raíz o una carpeta `armas/` que contenga `pack.yml`,
`configuration/` (ítems, bloques y recetas) y `resourcepack/assets/` (modelos y texturas).
No es lo mismo que **Importar ítem**, que acepta solamente un YAML o ZIP de ítem individual.
La importación completa valida las rutas y crea el pack sin sobrescribir uno existente.
Después ejecutá `/vsr reload all` y distribuí el resource pack actualizado.
