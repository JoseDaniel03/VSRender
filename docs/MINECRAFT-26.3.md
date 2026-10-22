# Port 26.3 — mejoras nativas

**Estado:** migración de archivos originales completada (3.185/3.185 fuentes y documentos propios). El port 26.3, sus tests Java y la validación sobre Canvas/Folia continúan pendientes. No implica que el servidor 26.3 esté validado ni que exista una JAR.

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
