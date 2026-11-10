# VSRender → VSMedia Resource Packs

Desde esta integración, **VSRender prioriza VSMedia** para enviar los Resource Packs de cada jugador. VSRender sigue generando, seleccionando, publicando y obteniendo sus URLs/hash como antes, pero entrega los descriptores a VSMedia cuando está activo.

- El envío usa `offerPluginResourcePacks(Plugin, UUID, List<Map<String,String>>)`, detectado por reflexión, sin dependencias nuevas de Gradle.
- Si VSMedia acepta el lote, VSRender **no envía ningún paquete adicional** y no retira paquetes en el cliente. VSMedia administra capas/UUID/hash y efectúa los envíos a través del scheduler de entidad Folia.
- Si VSMedia no está instalado, está deshabilitado o rechaza un lote, VSRender ejecuta su mecanismo de entrega anterior.
- No se recomprimen texturas en Minecraft ni se bloquea el hilo de región para construir packs.
- El bridge usa el lote de URL/UUID/SHA-1 ya preparado por jugador; nunca sustituye preferencias individuales por un stack global.
- Para compatibilidad las URLs deben ser HTTPS y disponer de hash SHA-1 válido. Si la configuración de VSRender emplea HTTP local, continuará con su propio sender hasta que tenga un host HTTPS compatible.
- Para prevenir duplicación real deben actualizarse **ambos** JAR, VSMedia y VSRender. Typewriter solo escucha el estado del pack y no requiere un sender adicional.
