# Port 26.3 — mejoras nativas

**Estado:** script y 7 pruebas de regresión preparados; la ejecución espera la importación completa del módulo original. No implica que el servidor Canvas 26.3 esté validado.

## Componentes de ítems

Minecraft 26.3 elimina `swing_animation` y separa el comportamiento en `attack_animation` e `interact_animation`. El script `porting/apply_26_3.py`, ejecutado tras la importación del motor, modifica sólo `ItemProcessors` y `ComponentsProcessor`:

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

**Pendiente:** importar motor, ejecutar JUnit y Gradle, validar NMS/Canvas/Folia 26.3 y pruebas reales de interacción.
