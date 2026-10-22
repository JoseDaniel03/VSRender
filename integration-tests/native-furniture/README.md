# Probe de mobiliario (sólo pruebas)

NativeProbe no se instala en producción: coloca y retira bloques en el chunk 0,0 a Y=100. Preparar un servidor CanvasMC 26.2 Java 25 con VsRender y la demo instalada, dejando esos espacios libres. Compilar con javac usando como classpath el JAR de VsRender y los JARs de las bibliotecas de Canvas, empaquetar las clases junto a paper-plugin.yml como NativeProbe.jar e instalar ambos en plugins.

El probe trabaja en RegionScheduler después de cargar el chunk. Los marcadores VSR_FURNITURE_PASS y VSR_NATIVE_PASS indican que completó los casos. VSR_NATIVE_FAIL debe revisarse incluso si el servidor arrancó. Reiniciar y verificar además VSR_NATIVE_RESTART_PASS para la mesa. La luz se verifica tras veinte ticks para permitir la propagación asíncrona.

El caso de suelo verifica el carrier enviado; no sustituye la inspección visual en cliente. Los casos de sofá ejercitan orientación norte, unión lateral y una esquina oeste. Las demás rotaciones se validan estructuralmente en el pack.

Geometría sin servidor: `python3 integration-tests/native-furniture/verify_models.py` desde fuentes. Comprueba que los respaldos de esquina continúen el respaldo del vecino perpendicular y que los brazos se eliminen sólo en las uniones correctas.

Orientación .3: el probe prueba las cuatro direcciones por getStateForPlacement con una mirada controlada en BlockPlaceContext; exige frente hacia el jugador para demo_sofa y dirección previa para default:sofa. El marcador es VSR_SOFA_FACING_PASS. Para el smoke local, desactivar update-checker/metrics y métricas bStats y usar una IP explícita de hosting apropiada al entorno; no hace falta consultar la IP pública ni el servicio de actualizaciones para esta prueba.

StatueProbe.java prueba la estatua mediante el comportamiento de ítem configurado, en un mundo de prueba con coordenadas (8,110,8) y (12,110,8). Ejecutar únicamente en fixture: modifica y limpia esas zonas. run_statue.py en evidencia automatiza dos arranques y generación de pack; requiere ambas marcas VSR_NATIVE_PASS y VSR_STATUE_PASS. Los probes de colocación usan jugador null y no cubren cancelación de eventos de colocación con jugador real.

ShelfProbe.java modifica el chunk0,0 en (8,125,8) y (12,125,8). Compilar junto a ShelfWire.java del cliente para verificar el snapshot real. Usa Player/Inventory controlados para llamar al gestor de interacción real, y comprueba transferencia, metadata, modos, solapamiento, drop y persistencia. No instalar este probe en producción.
