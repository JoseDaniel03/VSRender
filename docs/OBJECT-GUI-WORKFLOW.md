# GUI asociada a cada objeto — VSRender 26.3

La nueva carpeta `bibliotecas` organiza los **archivos fuente editables** del objeto. No hay un editor integrado: el PNG se modifica con un programa externo y se importa al pack de VSRender.

## Ejemplo: mochila

Primero crea/habilita un pack normal con las herramientas de VSRender. Luego:

```text
/vsr gui export demo aventura items mochila_aventurero 3
```

Se generan estos archivos (sin pisar archivos existentes):

```text
plugins/VsRender/resources/demo/bibliotecas/aventura/items/mochila_aventurero/
└── gui/
    ├── gui.yml
    ├── layout.yml
    ├── background.png
    ├── overlay_player_inventory.png
    └── guia_casillas.png
```

Tipos admitidos: `items`, `blocks`, `furniture`. Todos los nombres se limitan a letras minúsculas, dígitos, guiones, guion bajo y punto.

**Medidas del PNG:** ancho 176 píxeles; alto `114 + 18 × filas`. Para 3 filas, `176 × 168`. Tanto `background.png` como `overlay_player_inventory.png` tienen el tamaño completo; el overlay es opcional/transparente y se superpone al fondo. `guia_casillas.png` solo sirve de referencia para editar. El área inferior incluye las casillas de inventario y barra rápida del jugador, **sin moverlas ni alterar su funcionamiento vanilla**.

### Importación

Tras editar los PNG con Aseprite, GIMP, Krita u otro programa:

```text
/vsr gui import demo aventura items mochila_aventurero
```

El comando valida los archivos, combina el overlay y coloca los recursos renderizables en:

```text
resources/demo/resourcepack/assets/<namespace>/textures/font/gui/aventura/items/mochila_aventurero.png
resources/demo/configuration/gui/objects/aventura/items/mochila_aventurero.yml
resources/demo/configuration/gui/obj_aventura_items_mochila_aventurero.yml
```

La primera configuración registra la imagen en el motor original; la segunda permite verla con `BasicGui`. **Después de importar, recarga la configuración y vuelve a generar y distribuir el resource pack** usando el workflow que ya tenga configurado VSRender. El comando no fuerza ni reinicia la distribución de packs por sí solo.

Vista previa (casillas decorativas y botones `none/close` de `gui.yml`):

```text
/vsr gui open demo obj_aventura_items_mochila_aventurero
```

### Cofres y muebles con almacenamiento

En el comportamiento `simple_storage_block` (cofre), o `simple_storage` del mueble, añade la propiedad opcional `gui` con la ubicación de su biblioteca:

```yaml
behaviors:
  - type: simple_storage_block
    rows: 3
    gui: "demo:construccion/blocks/cofre_roble"
```

El importador no crea almacenamiento nuevo: el comportamiento nativo existente sigue siendo responsable de guardar objetos, comprobar permisos, emitir sonidos y administrar los viewers. Si existe una importación para esa referencia, el título de ventana usará el PNG; si no existe, se conserva el título original. En el ejemplo, el PNG debe ser de 3 filas igual que el comportamiento de almacenamiento. Requiere recargar las configuraciones del bloque/mueble para vincular una nueva referencia.

### Una mochila no es automáticamente un contenedor

El importador prepara **su interfaz visual**, pero la lógica de mochila portátil (abrir al usar el ítem, persistir objetos dentro del ítem, impedir duplicaciones) corresponde al comportamiento propio de ese ítem. No debe simularse mediante el preview de `BasicGui`, que cancela clics para proteger elementos de demostración.

### Limitaciones

- El PNG puede decorar la zona del inventario del jugador, pero no mueve físicamente sus casillas.
- `gui.yml` admite `rows`, `title` y `buttons` de la vista previa; para preview los botones solo admiten acciones `none` y `close`.
- `layout.yml` admite `player_inventory.mode: vanilla` y `decorate: true/false`; no es un editor de slots arbitrarios.
- No modifica `internal`, no crea un motor paralelo de contenedores y no genera la JAR final del plugin.
