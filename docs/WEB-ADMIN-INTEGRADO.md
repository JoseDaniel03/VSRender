# Administrador web integrado VSRender 26.3

La web está dentro de la JAR, sin Python, sin Node y sin contenido de ejemplo.

Comandos: /vsr web start, /vsr web status, /vsr web stop.
El puerto es 127.0.0.1:3050 y el acceso usa token de sesión privado.
El comando start muestra el enlace completo.

Se editan los YAML reales dentro de plugins/VsRender/resources/.
El botón Aplicar al juego solicita recargar configuraciones del plugin.
Los modelos y texturas pueden requerir además regenerar y reenviar el resource pack.
Los archivos existentes tienen respaldo en plugins/VsRender/.vsrender-web-backups/.

Primera tanda: CRUD de ítems, packs, importar/exportar YAML/ZIP de un ítem,
plantillas básicas y documentación. La cocina dinámica, cultivos completos,
Crafting Studio y GUI Studio visual quedan pendientes.

La web está deshabilitada por defecto; no se deben compartir sus enlaces.
No se modifica el motor de shaders ni el renderizado.
