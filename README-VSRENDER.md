# VsRender — bloques reales sobre CanvasMC

VsRender final basado en el código público de CraftEngine bajo GPLv3. Versión validada: 26.9.2-vsrender.6. El runtime nativo normal no requiere BetterModel instalado; BetterModel queda como compatibilidad/import opcional.

## Contenido
- JAR del fork completo: bloques nativos, estados, selección/colisión, red, persistencia, ítems, pack y servicios de soporte. Los módulos originales se conservan; no es una extracción sólo de bloques ni está optimizado aún.
- Fuentes públicas completas, licencia, build Gradle y atribución upstream.
- Demo de mesa .bbmodel como bloque nativo; no añade displays ni entidades Interaction.
- Informe y evidencia de validación.

## Instalación de prueba
1. Usar CanvasMC 26.2 con Java 25 en un servidor de prueba.
2. No instalar a la vez CraftEngine, la preview antigua de VsRender ni otro JAR de este fork. Separar la carpeta antigua plugins/VsRender con respaldo: no hay migrador.
3. Copiar `vsrender-paper-plugin-26.9.2-vsrender.6.jar` a `plugins/` y arrancar una vez para crear configuración y recursos internos. El primer arranque puede resolver bibliotecas necesarias.
4. Apagar, copiar demo/vsrender_demo a plugins/VsRender/resources/vsrender_demo y volver a arrancar.
5. Como operador ejecutar /vsr reload all. Esperar la generación del pack en plugins/VsRender/generated/resource_pack.zip. La fase de hosting depende de la configuración; para prueba local basta el ZIP generado.
6. Activar ese ZIP en el cliente y recargar recursos con F3+T.
7. Ejecutar /vsr item get vsrender:demo_table y colocar el ítem en el suelo como un bloque normal.

La mesa ahora usa un estado visual de hojas: mantiene el piso visible debajo del modelo. Los nuevos modelos usan texturas vanilla sin tintindex, así que no reciben el color del bioma.

La selección y colisión de estas demos corresponden a un bloque entero. El modelo visual es una mesa: no se prometen patas con colisión individual. Comprobar textura, contorno, rotura/drop y reinicio en cliente.

## Native Content Pipeline v1

VsRender ya incluye un camino first-party para contenido nuevo, separado de BetterModel. Un archivo `.bbmodel` colocado en `resources/<pack>/blueprint/native/` se importa a un asset nativo, se cachea por SHA-256 y se compila al subtree reservado `resourcepack/assets/<namespace>/**/native/`.

El provider es `vsrender_native`. El mismo asset declara los contextos ITEM, BLOCK, ENTITY, FURNITURE, PLAYER_ATTACHMENT y VEHICLE sin duplicar rig, animaciones ni geometría. Para Block y Furniture existe el elemento `native_model`. Ver `VSR-NATIVE-CONTENT.md` para layout, límites de Blockbench, cache y reglas de resource pack.

## Nuevos bloques
- `/vsr item get vsrender:demo_lamp`: lámpara, clic derecho con mano vacía enciende/apaga; luminancia nativa 15/0 y dos modelos visuales.
- `/vsr item get vsrender:demo_chair`: silla de madera; Shift + clic derecho con mano vacía cambia orientación.
- `/vsr item get vsrender:demo_sofa`: sillón rojo modular, colocado de frente al jugador; Shift + clic derecho con mano vacía gira. Módulos del mismo sillón y orientación se unen lateralmente sin brazos entre sí; colocar un módulo perpendicular detrás forma una esquina interior. Retirar un vecino restaura brazos/esquina.
- `/vsr item get vsrender:demo_table`: mesa original con carrier corregido.

Los muebles son bloques nativos, sin entidades display. Sentarse no está implementado en estas demos. Las formas físicas continúan siendo cubos completos.

## Estatua gigante
`/vsr item get vsrender:demo_statue_giant`: estatua de piedra con pedestal 3×3 y altura 5. Se coloca desde el centro del pedestal, de frente al jugador. Ocupa 15 celdas, cada una con selección y colisión de cubo completo; los huecos permanecen libres. Requiere todas esas celdas vacías y dentro de una región accesible y cargada. Romper cualquier pieza retira el conjunto y produce un solo ítem en supervivencia. Las piezas conservan su relación al reiniciar. La reutilización del mismo punto espera a que termine cualquier retirada diferida.

## Estantería y soporte invisible (.5)
Estas dos demos requieren el mod `Virgoland-Client-0.2.43-dev.jar` del paquete, en Fabric para Minecraft 26.2 con Java 25. Reemplazar el mod anterior; conservar Client Bridge y los otros plugins instalados. El mod completo mantiene UI, cámara y funciones anteriores.

- `/vsr item get vsrender:demo_shelf`: tres bloques de ancho, cinco pisos y 15 posiciones. Colocar desde el centro del piso inferior, mirando hacia donde quedará el respaldo.
- `/vsr item get vsrender:demo_invisible_stand`: soporte de una celda sin geometría visible. Conserva selección y colisión de cubo para interactuar.
- Clic derecho con un ítem: deposita una unidad en la posición seleccionada. Shift al depositar: empieza acostado.
- Clic derecho con mano vacía: retira el ítem, conservando nombre y componentes.
- Shift + clic derecho vacío: cambia parado → según tamaño → acostado.
- `/vsr item get vsrender:demo_shelf_wide_box`: caja ancha; insertar en el centro y pasar al modo según tamaño para comprobar una pieza que ocupa tres posiciones.
- Romper cualquier pieza retira la estantería y devuelve su contenido. Los ítems exhibidos no son entidades; los drops normales sí lo son.

El modo según tamaño ajusta el modelo a un ancho reservado manteniendo sus proporciones y limitando altura/profundidad al compartimento. El ancho del servidor es un perfil, no una inferencia automática del modelo del cliente. `plugins/VsRender/shelf-items.yml` permite definir `widths` por ID vanilla o personalizado; se lee al arrancar. Stone y `vsrender:demo_shelf_wide_box` vienen con ancho 2.5 para probar un objeto grande: colocarlo en la posición central bloquea sus vecinas. Defaults: bloque .8 e ítem .45. El PDC FLOAT `vsrender:shelf_width` permite sobrescribir el perfil por ítem. El soporte invisible acepta hasta ancho 1.

### Actualización del caché visual desde .4
Con el servidor apagado, respaldar y retirar únicamente `plugins/VsRender/cache/visual_block_states.json`, reemplazar JAR/demo, arrancar y ejecutar `/vsr reload all`. La nueva demo comparte las apariencias idénticas de pedestal/pisos: 96 apariencias en lugar de reservar una por cada pieza. El caché visual anterior mantiene asignaciones antiguas y puede agotar el pool. Conservar `custom_block_states.json`, el resto de cachés y los datos del mundo. Regenerar y reemplazar el resource pack del cliente.

## Catálogo de bloques
Ver `CATALOGO-BLOQUES.md`: lista de bloques propuestos, con dimensiones y comportamientos a probar. La estatua gigante 3×3×5 ya está incluida; monumentos, fuentes, arcos y otros bloques permanecen propuestos.

## Orientación del sillón
La demo declara `face_player: true` en el comportamiento `sofa_block`. Mirar al norte coloca el frente al sur, hacia quien lo coloca; las otras tres direcciones siguen la misma regla. Se conserva el comportamiento upstream si se omite esa opción. No gira automáticamente sillones ya colocados: para esos usar Shift + clic derecho vacío.

## Actualizar desde el primer fork nativo
Con el servidor apagado, reemplazar el JAR de VsRender y copiar `demo/vsrender_demo` sobre `plugins/VsRender/resources/vsrender_demo`. Conservar el resto de recursos y datos. Arrancar, ejecutar `/vsr reload all` y regenerar/reemplazar el ZIP del cliente; F3+T. El pack anterior conserva el carrier opaco y no sirve para verificar la corrección. Esto aplica a los forks nativos 26.9.2-vsrender.1 y .2, no a la preview 0.1.x.

## Comandos y compatibilidad
/vsr y /vsrender sustituyen a /ce y /craftengine. Los permisos ce.* y las clases net.momirealms permanecen para reducir cambios internos. Las integraciones externas que declaren depend CraftEngine requieren adaptación. Los comandos /vsr import, place e inspect de la preview anterior no pertenecen a este fork.

## Verificación
Release final validada en el runner self-hosted `simplementejose` con Java 25.0.4 y Gradle 9.7.1. Pasan `:core:test`, `:bukkit:test`, `:bukkit:compatibility:test`, `:bukkit:native26:compileJava` y `:bukkit:paper-loader:shadowJar`. El smoke standalone de Paper 26.2 sin BetterModel instalado alcanza realmente `Done`.

El JAR final incluye el backend físico NMS 26.2 de hitboxes owner-aware. La release `vsrender-final-36bd820c2be2-37647338953` contiene el JAR, ZIP de fuentes, SHA-256, README de validación y log del smoke. SHA-256 del JAR: `abc0a23ebb645bcb33bdb202b415614333da77be43eeb27e2af0f507ec13c1b5`.

El único alcance no ejecutado en este cierre es un recorrido manual visual/físico con jugador real sobre CanvasMC 26.2. Ese QA de plataforma es posterior y no cambia el estado **100% source/runtime/release**. Ver `VSR-VALIDATION.md` para la evidencia completa.

## Compilar
Desde fuentes, con Java 25: bash gradlew :bukkit:paper-loader:shadowJar :core:test --no-daemon --max-workers=2. Artefacto en target. No son necesarios VS Engine, CraftEngine instalado ni BetterModel para esta demo.

## Licencia y procedencia
Upstream https://github.com/Xiao-MoMi/craft-engine, SHA b3079c5ac170fff2cb2a4c4dad184771a236cd1b. Conservar LICENSE y VSR-UPSTREAM.md. Las dependencias mantienen sus licencias originales. No se incluyen implementaciones premium privadas.
