# VSRender

Motor autónomo de contenido y renderizado para servidores Minecraft VSEngine / Canvas.

**Estado de migración:** traslado desde `DaniDevz/VirgolandStudio/modules/vsrender-engine` en preparación. La última validación documentada del motor original corresponde a Paper 26.2; **VSEngine 26.3 todavía no está verificado**. No instalar en producción hasta completar la migración y las pruebas.

## Alcance

- Importación nativa de modelos Blockbench `.bbmodel`.
- Renderizado propio `vsrender_native`, rigs, animaciones y hitboxes.
- Ítems, bloques, muebles, estructuras multibloque y generación de resource packs.
- Plugin independiente, sin BetterModel ni CraftEngine como dependencias obligatorias.

## Procedencia y licencia

Este proyecto parte de VSRender, desarrollado en VirgolandStudio, sobre código abierto de [CraftEngine](https://github.com/Xiao-MoMi/craft-engine), bajo licencia **GPLv3**. El port debe conservar el archivo `LICENSE`, las atribuciones originales y `VSR-UPSTREAM.md` del módulo fuente.

## Port a VSEngine 26.3

1. Importar el árbol completo del módulo original como raíz del repositorio.
2. Verificar el build original sin modificar comportamiento.
3. Actualizar integración NMS y schedulers para Minecraft 26.3.
4. Validar los ejemplos nativos y la generación de resource packs.
5. Publicar un JAR únicamente tras compilarlo y probarlo.
