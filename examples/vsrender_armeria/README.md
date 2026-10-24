# Armería modular — VSRender 26.3

**Este directorio conserva las configuraciones fuente del paquete**: ítems, armas, recetas y mesas nativas. Las texturas PNG se entregan en el ZIP completo del pack generado en la conversación. Esta carpeta de GitHub no es el paquete visual completo: para instalarlo directamente se necesita también el directorio `resourcepack/assets/armas/textures/`.

- 20 ítems, incluidas las dos mesas
- 17 recetas `shaped` y 3 recetas `smithing_transform`
- 2 estaciones funcionales basadas en `open_window`: crafteo y herrería
- Sin motores externos ni plugins nuevos

Instalar el ZIP completo como `plugins/VsRender/resources/armas/`. Recargar con `/vsr reload all`, regenerar el resource pack y pedir las estaciones con `/vsr item get armas:mesa_armado` y `/vsr item get armas:mesa_mejora`.

**Limitaciones de v1:** las recetas funcionan también en estaciones vanilla y no son exclusivas de las mesas personalizadas; las mejoras no preservan encantamientos (`merge_components: false`); el visor no proporciona zoom y las armas no tienen modelos 3D personalizados. Validación estática completada, falta prueba en servidor.
