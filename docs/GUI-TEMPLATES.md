# GUIs editables en VSRender 26.3

## Donde esta el ejemplo original

- `common-files/src/main/resources/resources/internal/configuration/gui.yml`: iconos, imagenes y plantillas.
- `common-files/src/main/resources/resources/internal/resourcepack/assets/minecraft/textures/font/gui/custom/`: fondos PNG del navegador y las recetas.
- `core/src/main/java/net/momirealms/craftengine/core/plugin/gui/`: sistema existente de `GuiLayout`, `BasicGui` y `GuiElement`.
- `core/src/main/java/net/momirealms/craftengine/core/plugin/gui/category/ItemBrowserManagerImpl.java`: disposicion y navegacion de la demo.

Es mejor dejar `internal` sin modificar y trabajar en un pack propio.

## Crear tu propio menu

Primero crea y habilita un pack con el comando de recursos integrado (si todavia no tenes uno). Luego, como administrador:

```text
/vsr gui create demo menu_principal 3
/vsr gui open demo menu_principal
```

Tambien admiten el prefijo `/vsrender`. Ambos comandos requieren `ce.command.admin.gui`.
Crear genera **sin sobrescribir**: `plugins/VsRender/resources/demo/configuration/gui/menu_principal.yml`. Editalo y usa `gui open` otra vez para ver los cambios; la vista previa lee el archivo al abrir.

```yaml
title: "<white>Menu Principal"
rows: 3
buttons:
  13:
    item: minecraft:emerald
    action: none
  26:
    item: minecraft:barrier
    action: close
```

Los slots son numeros empezando en **0**, de izquierda a derecha, 9 por fila. Las filas admitidas son 1–6. La vista previa solo tiene las acciones `none` (cancelar el clic) y `close` (cerrar). No ejecuta comandos arbitrarios ni agrega otro motor GUI; usa `BasicGui` y `GuiElement` ya existentes.

## Fondo PNG propio

Registra la imagen en **otro** archivo YAML del mismo pack (por ejemplo `configuration/imagenes.yml`):

```yaml
images:
  demo:mi_fondo:
    height: 140
    ascent: 18
    font: minecraft:gui
    file: demo:font/gui/mi_fondo.png
```

El PNG se guarda en `plugins/VsRender/resources/demo/resourcepack/assets/demo/textures/font/gui/mi_fondo.png`.

Luego configura el titulo de `menu_principal.yml`:

```yaml
title: "<white><shift:-11><image:demo:mi_fondo>"
```

Para ver cambios en el PNG debes regenerar/enviar el resource pack con el workflow existente de VSRender. El fondo es visual; los botones reales siguen las casillas del inventario.

No modifiques la GUI de demostracion interna ni generes la JAR final sin autorizacion.
