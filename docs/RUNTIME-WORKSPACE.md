# Carpetas de trabajo de VSRender 26.3

Al arrancar el plugin, dentro de `plugins/VsRender/` se asegura la estructura de
carpetas de la versión anterior, **sin borrar archivos**. La operación es
idempotente. Se ejecuta **después** de construir el gestor de packs para no
impedir que extraiga el contenido predeterminado en `resources/`.

```text
plugins/VsRender/
├── generated/
├── import/
├── library/
├── native-assets/
├── extensions/
├── content-data/
├── cache/
├── export/
├── libs/
├── translations/
└── resources/
    └── <pack>/
        ├── pack.yml
        ├── configuration/
        ├── resourcepack/
        └── bibliotecas/
            └── <categoria>/
                └── items/
                    └── <objeto>/
                        └── gui/
```

**Rutas ya utilizadas por el motor:**
- `generated/`: carpeta de salida configurada para el resource pack;
  el ZIP aparecerá cuando se genere el pack, no necesariamente en el primer arranque.
- `native-assets/`: modelos y assets nativos.
- `cache/`: optimización y otros datos cacheados.
- `translations/`: archivos de idioma.
- `resources/`: packs de contenido y sus configuraciones.
- `resources/<pack>/bibliotecas/.../gui/`: fuentes PNG/YAML por objeto
  para `/vsr gui export` y `/vsr gui import`.

**Carpetas de compatibilidad reservadas:**
- `import/`, `export/`, `library/`, `extensions/`, `content-data/`
  y `libs/` se crean para recuperar el esquema de trabajo y futuras herramientas.
  **Que existan no significa que un importador/exportador general ya esté
  conectado a esas rutas**. El importador de GUI ya implementado usa la carpeta
  del objeto dentro de `resources`. Las librerías Java de compilación
  pertenecen al repositorio; no es necesario copiarlas a `plugins/VsRender/libs`.

La estructura se crea al **arrancar** el servidor con la nueva JAR.
Actualizar solo una carpeta de contenido o ejecutar `/vsr reload` con una JAR
antigua no puede crear estos directorios. Si una carpeta falla por permisos de
escritura, VSRender informa la ruta en el registro del servidor.
