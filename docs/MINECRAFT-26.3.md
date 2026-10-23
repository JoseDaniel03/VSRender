# Port 26.3 — mejoras nativas

**Estado al 22/10/2026:** migración de fuentes y dependencias completada. La compilación y pruebas Java de Core, Bukkit y el módulo native26 contra Paper 26.3 ya finalizaron exitosamente en CI. La validación de comportamiento en un servidor Canvas/Folia 26.3 continúa pendiente. Todavía no se publica una JAR final.

## Componentes de ítems

Minecraft 26.3 elimina `swing_animation` y separa el comportamiento en `attack_animation` e `interact_animation`. El script `porting/apply_26_3.py`, aplicado durante la migración, modifica sólo `ItemProcessors` y `ComponentsProcessor`:

- Traduce configuraciones existentes de `swing_animation` a ambos componentes vanilla.
- Respeta valores modernos declarados explícitamente (no los sobrescribe con el legacy).
- Evita enviar el obsoleto `map_color` en 26.3.
- En 26.2 y anteriores no altera los componentes.
- Abarca tanto campos de ítem `data:` como el procesador explícito `components:`.
- No agrega un motor de animación: las animaciones de rigs siguen siendo propias de `vsrender_native`.

Ejemplo de formato moderno (dentro de `data:`):

```yaml
attack_animation:
  type: stab
  duration: 6
interact_animation:
  type: whack
  duration: 6
```

## Cambios de vanilla adicionales a considerar

- Resource pack 97.1 y datapack 121.0: VSRender original **ya contiene** el mapeo de recursos 97.1; no se duplica.
- `/posteffect` permite postprocesamiento desde shaders del cliente; utilizar cuando baste, sin reemplazar el renderer 3D.
- `cookingtime` ahora es obligatorio para recetas JSON de cocción. Smoker y blast furnace requieren revisar las duraciones.
- El compilador de shaders 26.3 usa `#include` y reglas nuevas; no migrar shaders complejos automáticamente.

Fuente: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3

**Pendiente de etapa 2:** ejecutar las pruebas JUnit y compilación de código Java, actualizar/validar NMS/Canvas/Folia 26.3 y efectuar pruebas reales de interacción. El empaquetado final (`shadowJar`) queda expresamente pospuesto.

## Avance de etapa 2: seguridad de regiones Folia/Canvas

**Corregido en la tanda 217:** `AbstractBukkitExecutor.runLater(Runnable, delay, core.World, x, z)` descartaba las coordenadas recibidas y enviaba la tarea a `(0, 0)`. Al delegar en `FoliaExecutor`, ese error podía ejecutar el callback en la región equivocada. Ahora reenvía `x, z` sin alterarlos, manteniendo el backend scheduler existente. La sobrecarga de `Location` ya calculaba las coordenadas de chunk mediante desplazamiento de 4 bits.

**Regresión añadida:** `porting/test_folia_region_26_3.py` verifica la transmisión de coordenadas y puede ejecutarse sin bibliotecas de compilación privadas. No equivale a un test de ejecución con múltiples regiones.

**Contratos 26.3 presentes y comprobados:** `porting/test_native_26_3_contracts.py` comprueba:
- `MinecraftVersion.V26_3`, `PackVersion(97, 1)` y el dev bundle de Paper `26.3.build.+`.
- Claves de componentes introducidas o utilizadas en 26.3, incluyendo `attack_animation`, `interact_animation`, `compostable`, `cooking_fuel` y `brewing_fuel`.
- Compatibilidad de `swing_animation` con el desdoblamiento moderno y supresión de `map_color`.

### Comprobaciones Java y NMS

El workflow `build.yml` conserva la tarea de compilación **sin** `shadowJar`. También dispone de una comprobación diagnóstica `:core:compileJava` cuando no está disponible `VSRENDER_SOURCE_TOKEN`. Esta comprobación puede advertir de dependencias ausentes; no se interpreta como validación completa de Java.

Los backends NMS del módulo `bukkit/native26` siguen identificados internamente como `v26_2`. Su compatibilidad binaria y de comportamiento con Paper/Canvas/Folia **26.3** todavía requiere compilación con todas las bibliotecas de terceros y pruebas en un servidor real; no deben renombrarse ni declararse compatibles de forma automática.

**Criterio de cierre de etapa 2:** compilación Java y pruebas JUnit sin errores, verificación explícita de los símbolos NMS en 26.3, pruebas de scheduling interregional, hitboxes, modelos, animaciones, resource packs e interacción en VSEngine/Canvas/Folia. Ninguna JAR final se genera hasta completar esas comprobaciones.

### Diagnóstico Java reproducible (tandas 221–229)

En la ejecución de `build.yml` del commit `d07b5de`, `:core:compileJava` falló con **95 errores de compilación**: faltaban `ca.spottedleaf.concurrentutil`, `ca.spottedleaf.common.util` y `cn.gtemc.reflection` en el classpath público de Core. Muchos errores posteriores (`cannot find symbol`, `@Override`) son cascadas por esas clases externas ausentes; **no atribuirlos todavía a cambios NMS 26.3**.

El código original de Core ya utilizaba clases LeafPile pero `core/build.gradle.kts` no declaraba el archivo binario en la configuración de compilación. En la tanda 225 se añadió `compileOnly(files(.../libs/leafpile-1.0.0.1.jar))`, con el nombre resuelto a través de `versionOf("leafpile")`, igual que en los demás módulos. No se agrega ni redistribuye ese JAR en Git público. El origen privado tiene la versión verificada; el workflow la recuperará únicamente si se configura `VSRENDER_SOURCE_TOKEN`.

La comprobación pública `:core:compileJava` es intencionalmente no bloqueante: una ejecución verde de GitHub Actions confirma auditoría y procesamiento de recursos, **no** una compilación Java correcta. Cuando estén disponibles las bibliotecas originales, `:core:test`, `:bukkit:test`, `:bukkit:compatibility:test`, `:bukkit:native26:compileJava` y `:bukkit:paper-loader:compileJava` deberán completar sin errores antes de cerrar la etapa 2. No se ejecuta `shadowJar`.

Registro de referencia de errores: https://github.com/JoseDaniel03/VSRender/actions/runs/39838064182

### Folia asíncrono: conversión de duraciones (tandas 231–238)

Se corrigió `FoliaExecutor` para evitar overflow de `long` al traducir ticks a milisegundos para `AsyncScheduler`: `ticksToMillis` satura en `Long.MAX_VALUE`, trata retrasos no positivos como cero, y limita a un tick los períodos de ejecución repetitiva no positivos. No se crean nuevas tareas ni se cambia la elección del scheduler de región/entidad/global.

`FoliaAsyncTimingTest` añade pruebas JUnit de conversión, límites y saturación; `porting/test_folia_region_26_3.py` añade una comprobación de fuentes ejecutable sin las siete JAR privadas. Las pruebas JUnit siguen pendientes de ejecutarse cuando el classpath Java esté completo.

En CI, la comprobación pública `:core:compileJava` se omite cuando existe el token privado: las bibliotecas deben restaurarse y comprobarse primero. El empaquetado `shadowJar` continúa excluido del workflow. Los tests Python de `porting/test_ci_native_26_3.py` vigilan estas condiciones para evitar regresiones.

### Recuperación de bibliotecas y compilación real de Core (tandas 240–244)

De la copia temporal `VSRENDER VERSION VIEJA/` cargada en GitHub se verificaron 3.177 archivos idénticos, ocho versiones antiguas de archivos ya portados y cinco JAR que faltaban en el árbol activo. Las JAR se trasladaron sin modificar su SHA Git a `libs/`; después se eliminó la carpeta duplicada sin sobrescribir fuentes del port.

El CI valida por `git hash-object` las bibliotecas disponibles, detecta las faltantes y utiliza las locales antes de acceder al repositorio privado. **Cinco JAR originales ya están en `main`**; solo faltan `bukkit/compatibility/legacy/libs/awsm-api.jar` y `bukkit/compatibility/legacy/libs/flow-nbt-2.0.2.jar`.

**Resultado verificado:** en https://github.com/JoseDaniel03/VSRender/actions/runs/39844448306, Gradle completó `:core:compileJava`, `:core:compileTestJava` y `:core:test` con `BUILD SUCCESSFUL`, y las 38 pruebas Python terminaron correctamente. Esto confirma **Core compilable y sus pruebas automatizadas ejecutables** usando las cinco JAR presentes. Todavía no demuestra que Bukkit/NMS 26.3 compile ni que VSRender funcione en un servidor VSEngine/Canvas/Folia 26.3.

Las dos JAR heredadas pueden añadirse en sus rutas exactas o restaurarse mediante el secreto de solo lectura `VSRENDER_SOURCE_TOKEN`; el código actual de GitHub Actions recupera únicamente las ausentes. El empaquetado del plugin sigue desactivado.


## Tandas 257–273: adaptación a Minecraft Java 26.3

**Versión oficial de referencia:** Minecraft Java 26.3, datapack `121.0`, resource pack `97.1`. No agregamos motores nuevos: aprovechamos lectores, serializadores, procesadores de ítems y generadores de modelos existentes.

### Recetas y datapacks

- Nuevo `VanillaRecipeReader26_3`: las recetas vanilla de `smelting`, `blasting`, `smoking` y `campfire_cooking` pasan por un lector que exige `cookingtime` explícito, entero y positivo. Versiones anteriores conservan sus reglas y su valor predeterminado.
- Las configuraciones propias de VSRender para `smoking` y `blasting` usan **160 ticks por defecto en 26.3** en lugar de 80, manteniendo el comportamiento previo con la nueva aceleración de combustible. Un `time` definido expresamente conserva su valor. Las recetas provenientes de datapacks mantienen exactamente el `cookingtime` que definió el autor.
- El motor **ya contaba** con `CustomBrewingRecipe`, `minecraft:brewing`, predicados `potion_contents` y la ruta de registro nativo activada en 26.3. No duplicar ese sistema.
- Los identificadores `cooking_fuel`, `brewing_fuel`, `block_transformer`, `compostable`, `attack_animation` e `interact_animation` ya existían en `DataComponentKeys`; los transformadores definidos mediante registry IDs los procesa la infraestructura actual. Esto no certifica todavía todas las interacciones vanilla sobre bloques y combustibles.

Ejemplo de receta 26.3 para datapack:

```json
{
  "type": "minecraft:smoking",
  "ingredient": "minecraft:beef",
  "result": "minecraft:cooked_beef",
  "cookingtime": 200,
  "experience": 0.35
}
```

### Modelos y NMS

- El exportador `BBModelConverter` conserva los cubos originalmente marcados `shade: false` con `shade_direction_override: "up"` en 26.3. Para versiones anteriores sigue exportando `shade: false`.
- El auditor `porting/audit_26_3.py` también señala modelos JSON existentes que aún contienen `shade: false`; **no los reescribe automáticamente**. Además, detecta `pot_decorations` con el formato antiguo de lista de cuatro IDs y el eliminado tag `#minecraft:brewing_fuel` de datapacks. En 26.3, las decoraciones de macetas usan un objeto con los lados `back/left/right/front`, y los combustibles de pociones se identifican mediante el componente `minecraft:brewing_fuel`.
- Se eliminó la llamada a `setInvulnerable(true)` ausente del NMS 26.3 en `Native26RigCollisionEntity`. Su lógica propia para impedir daño físico (`hurtServer`) sigue presente.

### Validación disponible

Las siete bibliotecas originales ya están en sus rutas de compilación y verificadas por SHA. **[GitHub Actions 39890053367](https://github.com/JoseDaniel03/VSRender/actions/runs/39890053367)** demostró una ejecución exitosa con `:core:test`, `:bukkit:test`, `:bukkit:compatibility:test`, `:bukkit:native26:compileJava` y `:bukkit:paper-loader:compileJava`. Las tandas posteriores amplían tests JUnit y controles estáticos; revisar los resultados correspondientes antes de afirmar su validación.

Esto demuestra compilación y pruebas automáticas, no funcionamiento completo de hitboxes, packs, NPC y rendimiento en producción. Las pruebas prácticas en Canvas/Folia siguen pendientes; no se ejecuta ni publica `:bukkit:paper-loader:shadowJar`.

### Próximos temas 26.3

Verificar mediante el servidor real las nuevas recetas de pociones, combustibles, `block_transformer`, nuevos registries y transacciones con resource packs; revisar shaders con `#moj_import` antes de migrarlos (el nuevo compilador usa `#include`). Aprovechar directamente comandos y datapacks vanilla cuando resuelvan el caso de uso, sin crear duplicados en VSRender.

Fuente principal: https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3

## Tandas 279–284: proveedores de contexto y auditoría de datapacks

Minecraft 26.3 divide el registro previo `number_provider` en `context_int_provider` (enteros) y `context_float_provider` (decimales). Sus nuevos tipos y referencias se resuelven **nativamente por Minecraft**: VSRender no necesita ni incorpora otro evaluador de expresiones.

Se extendió `porting/audit_26_3.py`, de solo lectura, para advertir sobre:

- Definiciones `data/<namespace>/number_provider/` y tags `tags/number_provider/` que necesitan migración al registro entero o decimal correspondiente.
- Tipos numéricos con nombres antiguos (`sum` → `add`, `product` → `mul`, `minimum` → `min`, `maximum` → `max`, `average` → `avg`) en proveedores de contexto, rolls de loot o los campos numéricos de combustible y compostaje.
- Proveedores uniformes inline sin `type` en esos contextos.
- Nombres anteriores de tipos `worldgen/block_state_provider`, como `weighted_state_provider` → `weighted` y `simple_state_provider` → `simple`.
- `pack.mcmeta` heredados, distinguiendo el formato **datapack 121.0** del **resource pack 97.1**; las declaraciones modernas `min_format`/`max_format` no se confunden con un viejo `pack_format`.

Los identificadores de registros nuevos (`demo:valor`) y los proveedores context-free de `worldgen` no se reinterpretan ni se reemplazan. **No hay conversión automática**, porque las referencias a números enteros y decimales deben revisarse según el contexto real. El auditor tampoco pretende evaluar expresiones vanilla.

### Uso en paquetes reales

Desde la raíz de VSRender:

```bash
python3 porting/audit_26_3.py
python3 porting/audit_26_3.py /ruta/a/datapack-descomprimido
python3 porting/audit_26_3.py /ruta/a/resource-pack-descomprimido
```

La herramienta busca `resources/`, `examples/` y `common-files/src/main/resources/` al ejecutarse sobre el repositorio, o escanea directamente `data/`/`assets/` del paquete descomprimido si se le proporciona su raíz. Emite avisos y **no edita archivos**, y no sustituye a `/reload` ni a pruebas dentro de Canvas/Folia 26.3.

Fuente primaria: [notas técnicas oficiales de Minecraft Java Edition 26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3).
