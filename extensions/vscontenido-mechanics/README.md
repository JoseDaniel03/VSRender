# VSContenido — extensión interna completa del núcleo

## Objetivo

VSContenido se integra en **la misma JAR de VSRender**, bajo `extensions/vscontenido-mechanics`; nunca se instala su antiguo plugin. VSRender sigue siendo dueño de los ítems, bloques, entidades, furniture, resource packs, UI y renderizado; esta biblioteca aporta el núcleo de **comportamientos y mecánicas**. VSClient es responsable de las teclas, del transporte de acciones y de los efectos visuales del cliente.

## Fuentes portadas (instantánea de octubre 2026)

- 49 fuentes Java de `DaniDevz/VirgolandStudio/modules/items/items-api/src/main/java`.
- 173 fuentes Java de `DaniDevz/VirgolandStudio/modules/items/items-core/src/main/java`.
- En total **222 clases/archivos de API y Core** conservados con sus paquetes originales, más las clases propias `capability` de integración con VSRender.
- Todos los archivos de origen se copiaron sin depender de otro plugin. Se mantiene `LICENSE` y atribución del proyecto VSRender; el código original pertenece al mismo usuario.
- No se importa `items-paper`, `items-blocks`, `models`, `sessions`, `commerce` ni sus sistemas de HUD/GUI/render. Algunos DTO de Core contienen *referencias descriptivas a imágenes/modelos*, pero no instalan otro renderizador. La API de render efectiva es la de VSRender.

## Subsystems disponibles en la biblioteca

| Área | Ejemplos |
|---|---|
| Comportamientos | triggers, conditions, actions, `BehaviorCompiler`, `BehaviorEngine`, presupuestos de ejecución |
| Estados y persistencia | `StateContainer`, `StateBinaryCodec`, instancias, stack policy |
| Armas y proyectiles | `FireControlRuntime`, spread, recoil, virtual projectiles, impactos, munición, recarga por etapas |
| Accesorios | slots, compatibilidad, registro, resolutores de modificadores, persistencia |
| Consumibles | perfiles, efectos, consumo y router de acciones |
| Mantenimiento | desgaste, suciedad, óxido, lubricación, averías y reparaciones |
| Sistemas | energía, fluidos, procesos temporizados, cooldown |
| Mundo | daño por área, ignición, agarre y lanzamiento de bloques, atracción de entidades |
| Otros | dual wield, inventario, estado visual descriptivo, secuencias cinemáticas, definición/esquemas, migraciones y codecs de Studio |

## Contrato YAML universal

La sección `vscontenido:` es adicional y reutilizable bajo objetos existentes de tipo `items`, `blocks`, `furniture` y `entities`. Los metadatos de modelos y renderizado se definen como siempre en VSRender.

```yaml
items:
  demo:herramienta:
    material: iron_pickaxe
    vscontenido:
      controles:
        activar: mouse_left
      energia:
        maxima: 100
      cooldown:
        ticks: 10

blocks:
  demo:maquina:
    vscontenido:
      acciones:
        iniciar:
          proceso: cocinar
      energia:
        maxima: 600

furniture:
  demo:telescopio:
    vscontenido:
      fov:
        aumento: 3.0
      controles:
        observar: mouse_right

entities:
  demo:dron:
    vscontenido:
      controles:
        interactuar: key_e
      estado:
        energia: 100
```

Los valores se indexan en `BukkitPackManager.vsContenidoDefinitions()` y son accesibles por **tipo + ID** mediante `VsContenidoDefinitions`. El parser valida restricciones básicas de `fov`, `controles`, `bala` y `recarga`, conserva otras capacidades como información extensible y el importador ZIP realiza las mismas comprobaciones.

## Estado y límites reales

**Código de API/Core portado en su totalidad**, pero esto **no equivale a port funcional completo del viejo plugin**: todavía faltan adaptadores al runtime Paper/Folia de VSRender y el receptor de acciones de VSClient. Ninguna entrada YAML sin ejecutor específico debe prometer efectos en juego.

No reintegrar la antigua identidad de ítems de VSContenido ni duplicar registro de ítems, renderizado, GUI, paquetes o schedulers. Los coordinadores antiguos `items-paper` serán la referencia al implementar adaptadores nuevos de VSRender y se deberán reescribir para los servicios nativos, no copiar como plugin autónomo.

## Compilación y pruebas

```bash
./gradlew :extensions:vscontenido-mechanics:test
./gradlew :bukkit:paper-loader:shadowJar
```

La extensión está incluida en Gradle `settings.gradle.kts` y como `implementation` del artefacto Paper único de VSRender. GitHub Actions ejecuta pruebas de la extensión antes de generar el JAR.
