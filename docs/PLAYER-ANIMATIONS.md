# Animaciones del jugador vinculadas a ítems

VsRender 26.9.2-vsrender.6 incorpora un adaptador para BetterModel 3.5.0. BetterModel sigue siendo un plugin opcional independiente: no se incluye un fork ni se embebe su motor dentro de VsRender.

## Configuración

Instalar BetterModel 3.5.0 Paper junto con VsRender. Mantener habilitado `module.player-animation` en la configuración de BetterModel. Crear las animaciones en los modelos de `plugins/BetterModel/players`, usando sus etiquetas de huesos de jugador y de ítems. Recargar BetterModel después de editar los modelos.

En la definición de un ítem existente, añadir este fragmento de comportamiento bajo el propio ítem. Usa el modelo y la animación incluidos en BetterModel; no crea un ítem de demo:

```yaml
events:
  - on: right_click
    type: vsrender:play_player_animation
    limb: steve
    animation: roll
    loop: false
```

Para detener la animación desde otra interacción o desde lógica:

```yaml
type: vsrender:stop_player_animation
target: self
```

Ambas funciones admiten las condiciones y selectores de jugador del runtime de funciones actual. `target` omitido equivale a `self`; un selector inválido se rechaza. Las animaciones en bucle requieren una acción de parada o una interrupción.

Se detiene la representación propia cuando el jugador cambia de slot, intercambia manos, suelta un ítem, interactúa con el inventario, cambia de mundo, muere o sale. Las tareas pendientes se invalidan para que una animación del ítem anterior no empiece después de cambiarlo. No se borran trackers pertenecientes a otras integraciones: si otro sistema representa al mismo jugador con BetterModel, se devuelve `CONFLICT`.

## Resource pack

Generar primero el pack de BetterModel y después el de VsRender. La configuración base de VsRender ya incluye `BetterModel/build.zip` en `resource-pack.merge-external-zip-files`. En una instalación con configuración personalizada, conservar o añadir esa entrada en la sección correspondiente. Entregar el pack combinado al cliente y recargarlo. El mod Virgoland Client de la entrega anterior no cambia en esta versión.

## API para la lógica de VirgolandStudio

El servicio público `net.momirealms.craftengine.bukkit.api.PlayerAnimationService` se registra en Bukkit ServicesManager solamente si se instala la integración BetterModel. Otro módulo puede obtenerlo con `Bukkit.getServicesManager().load(PlayerAnimationService.class)` y llamar:

- `play(Player, String limb, String animation, boolean loop)`.
- `stop(Player)`.

Devuelve un `CompletionStage<Result>`: `STARTED`, `STOPPED`, `NOT_RUNNING`, `CANCELLED`, `UNAVAILABLE`, `INVALID_RESOURCE`, `CONFLICT` o `FAILED`. El resultado de reproducción corresponde al intento real en el EntityScheduler, no solamente al envío de una tarea. Los consumidores deben volver a su scheduler propietario antes de modificar el mundo desde una continuación.

El lifecycle del bridge es owner de esos `CompletionStage`: al deshabilitar VsRender o BetterModel, todo resultado todavía pendiente termina como `UNAVAILABLE`, incluso si Canvas/Folia ya no ejecuta callbacks del `EntityScheduler`. Las solicitudes nuevas después del cierre también se rechazan como `UNAVAILABLE`; un callback tardío no puede reabrir ni sobrescribir ese estado.

La función visual no aplica daño, recarga ni consumo. Tampoco programa efectos sincronizados con keyframes: eso requiere un puente de marcadores de animación. Los módulos de lógica anteriores pueden consumir esta API; su editor de nodos y su catálogo de acciones todavía no fueron modificados en esta entrega.

## Alcance

Esta integración controla representaciones del jugador en BetterModel. No incorpora animaciones propias de manos/cámara en primera persona al mod, ni animaciones de ítems dentro de slots del inventario. La capa común completa de proveedores, el editor de bloques, categorías y presets siguen siendo trabajo separado.

## Validación

Consultar `validacion/bettermodel/INFORME.md` en el ZIP para los resultados y límites de esta entrega. Los tests del núcleo comprueban propiedad y limpieza de sesiones, callbacks tardíos y cancelación de solicitudes pendientes. El probe de servidor usa un jugador controlado y no certifica un render visual con un cliente conectado.
