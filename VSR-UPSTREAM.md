# VsRender: upstream attribution

Upstream: https://github.com/Xiao-MoMi/craft-engine
Baseline revision: b3079c5ac170fff2cb2a4c4dad184771a236cd1b
License: GPLv3; preserve LICENSE and upstream notices.

VsRender is now the product/runtime identity. BetterModel and ModelEngine adapters have been removed; the first-party rendering backend is `vsrender_native`.

The vendored implementation still keeps parts of the original `net.momirealms.craftengine` package namespace, persistence keys and low-level infrastructure where changing them would be a destructive migration with no runtime benefit. Those internal names do not represent a parallel CraftEngine engine.

User-facing plugin identity, data folder, commands, generated pack description and documentation use VsRender. Upstream attribution remains deliberately intact.

## Instalación del fork
Usar un servidor de pruebas limpio. Este fork también usa plugins/VsRender: NO mezclar esa carpeta con la preview 0.1.x; hacer respaldo y separar completamente los datos. No existe migrador entre los dos formatos. Integraciones externas que dependen del nombre CraftEngine requieren adaptación a VsRender. No modificar mundos de producción durante esta fase.

Revisión vsrender.2: demo con carrier no oclusivo, modelos nativos de lámpara/silla/sofá, estados de luz, conexiones laterales opcionales en SofaBlockBehavior y actualización al colocarse. Las configuraciones sin left/right conservan el comportamiento anterior.

Revisión vsrender.3: opción face_player (por defecto false), activada sólo para la demo de sillón, corrige el frente al colocar. Catálogo de futuras propuestas; sin implementar aún un sistema de colocación multibloque gigante.

## Cambios 26.9.2-vsrender.4
MultiblockBlockBehavior y MultiblockBlockItemBehavior registran estructuras nativas con estados part/facing. BlockItemBehavior respeta el resultado de placeBlock y emite BlockMultiPlaceEvent para los nuevos conjuntos. Validación por celda, rollback de snapshots y retirada sin drops secundarios; MultiblockRemovalReservations impide reutilizar un ancla durante limpieza regional pendiente. Demo de estatua, quince modelos por pieza y preview de ítem, con pruebas y fuentes incluidas.

## Cambios .5
ShelfManager guarda ítems reales, valida espacio y protección, sincroniza vistas con el cliente y restaura intercambios ante fallo de persistencia. ItemEventListener delega interacción después de CustomBlockInteractEvent y antes del uso genérico; MultiblockBlockBehavior retira contenidos una vez. ShelfLayout prueba límites y solapamientos. El cliente Fabric existente añade ShelfWire/Cache/Transforms y extracción/submission de ItemStackRenderState; no crea entidades para el contenido. Se compila en Java25/Minecraft26.2 y conserva módulos anteriores.
