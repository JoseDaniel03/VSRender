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
