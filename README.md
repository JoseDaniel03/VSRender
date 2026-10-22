# VSRender

Motor autónomo de contenido y renderizado nativo para Minecraft / VSEngine.

## Estado: migración en curso

Repositorio nuevo creado el 21/10/2026 para separar `modules/vsrender-engine` de `DaniDevz/VirgolandStudio`. Ya incluye **155 archivos del renderer y 51 procesadores de ítems**, junto con las clases de versiones/ítems, pruebas de compatibilidad, Gradle, licencia y workflows. Los 206 archivos de esas dos áreas están presentes; tres conservan modificaciones controladas de Minecraft 26.3. El traslado del resto del código completo desde el repositorio privado necesita ejecutar ese workflow con un token de lectura autorizado. **No existe todavía una compilación validada para Minecraft 26.3 en este repositorio**.

**[Cómo completar el traslado](MIGRATION.md)** · **[Motor original](README-VSRENDER.md)** · **[Importación nativa de modelos](VSR-NATIVE-CONTENT.md)** · **[Compatibilidad 26.3](docs/MINECRAFT-26.3.md)**

### Capacidad prevista

- Importación y compilación de modelos Blockbench `.bbmodel`.
- Renderer nativo independiente `vsrender_native`, animaciones, rigs y hitboxes.
- Ítems, bloques, muebles, estructuras multibloque y resource packs.
- Compatibilidad con Canvas/Paper/Folia, a verificar específicamente para 26.3.

El motor no necesita CraftEngine, BetterModel o ModelEngine instalados como motores externos. Algunas integraciones opcionales históricas podrán revisarse después.

### Licencia

Derivado del proyecto abierto [CraftEngine](https://github.com/Xiao-MoMi/craft-engine); conservar GPLv3 (`LICENSE`) y avisos upstream (`VSR-UPSTREAM.md`). Se conservarán también las atribuciones de terceros del código original.

### Validación continua

Con el núcleo de render ya migrado, GitHub Actions verifica los scripts de compatibilidad para 26.3 mediante pruebas Python. **Una ejecución verde en esta fase no significa que el plugin Java haya compilado**: el build Gradle se ejecuta solamente cuando estén presentes las fuentes completas. El importador privado aplica el script de animaciones nativas y audita shaders y recetas antes de confirmar el commit.
