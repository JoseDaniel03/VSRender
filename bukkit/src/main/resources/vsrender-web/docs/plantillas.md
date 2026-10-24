# Biblioteca de plantillas VSRender 26.3

Usá **Plantillas** dentro del administrador web, elegí una categoría, configurá el pack y generá su YAML. No hay ítems de demostración ni motores Java adicionales. Las nuevas creaciones se guardan en `resources/<pack>/configuration/`.

## Ítems y cocina
- **Objeto simple:** material vanilla, nombre, ID, textura PNG, lore y apilamiento. No consume hambre.
- **Ingrediente:** objeto no consumible, marcado como ingrediente en su descripción.
- **Alimento, bebida y plato:** alimentación, saturación, animación, tiempo, dos efectos opcionales y recipientes de retorno, conforme al sistema de consumo existente.
- **Cultivo:** producto, semilla, bloque cultivable y sus etapas dentro de un único archivo.

## Herramientas y equipo
- **Herramienta o arma:** base vanilla con durabilidad y valor de daño máximo; requiere stack igual a 1. Usá `iron_pickaxe`, `iron_sword`, etc. La plantilla no cambia atributos de combate del material.
- **Equipable:** base vanilla de armadura y ranura de cabeza, pecho, piernas o pies. Las capas/modelos 3D se configurarán manualmente si se quieren reemplazar los originales.

## Bloques
- **Bloque sólido:** genera un ítem colocable (`block_item`) y su definición `blocks` vinculada, con modelo cúbico, sonido, dureza, resistencia y loot que devuelve el bloque.
- **Mineral:** variante de bloque minable con pico. Por seguridad su botín inicial es el mismo bloque; aún no hay editor para drops variables o fortuna.
- **Mueble sencillo:** utiliza el `furniture_item` existente con display de ítem, hitbox de suelo y loot básico. Requiere textura personalizada; no crea modelos 3D avanzados.

## Plantillas avanzadas de construcción

- **Bloque luminoso:** crea un bloque colocable con emisión de luz fija ajustable de 0 a 15. Se reutiliza `settings.overrides.luminance`. No incluye un interruptor.
- **Bloque orientable:** usa la propiedad `facing` de cuatro direcciones y las apariencias rotadas del modelo. Su textura es obligatoria; conviene un PNG con un dibujo asimétrico para notar la rotación. Utiliza la generación `minecraft:block/template_glazed_terracotta` nativa.
- **Taladro lineal:** pico con durabilidad y comportamiento `range_mining_item`, para extender el minado en 1–3 bloques siguiendo la orientación del jugador. Se limita el alcance desde el formulario. No se añade un motor nuevo de minería.
- **Asiento:** mueble de suelo con hitbox y coordenada de asiento definidas por `seats`. Usa una textura 2D como apariencia inicial.
- **Expositor:** mueble con comportamiento `display_item_furniture` y hitbox de tipo `shulker` del ejemplo oficial, para poner y retirar objetos.

## Texturas
Para ítems: `namespace:item/nombre`. Para bloques: `namespace:block/nombre`.
Podés seleccionar PNG existentes del pack o subirlos desde el formulario (máximo 500 KB, hasta 1024×1024). Los PNG se almacenan en la carpeta `textures/item` o `textures/block` apropiada del resource pack.

Los bloques sin textura propia usan `minecraft:block/stone` como apariencia de base. Los modelos nativos de Minecraft no se renderizan en 3D dentro de la web; se muestran sprites 2D cuando existen.

## Guardado y edición
Los bloques generados se mantienen junto con sus ítems colocables al importarlos, exportarlos o eliminarlos. La modificación de los atributos físicos detallados de un bloque ya generado se hace en el YAML del pack; la ficha web permite modificar las propiedades del ítem y no debe borrar la sección `blocks`. Se crean respaldos automáticos antes de modificar o eliminar archivos existentes.

Después de crear algo, **Aplicar al juego**; si cambiaste PNGs, regenerá y distribuí el resource pack.

## Restricciones conocidas
La biblioteca no genera scripts, mesas especiales, modelos 3D, drops complejos ni recetas automáticamente. Usá Crafting Studio para asociar recetas reales a los ítems y bloques generados.
