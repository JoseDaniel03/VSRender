# VSRender

Motor autónomo de contenido y renderizado nativo para Minecraft / **VSEngine 26.3** (Canvas, Paper y Folia).

## Estado de la migración — 22/10/2026

**Etapa 1 — migración de fuentes: 100 % completada.** El traslado desde `DaniDevz/VirgolandStudio/modules/vsrender-engine` conserva todas las fuentes y documentos originales de código propio. La referencia upstream permanece fijada en `88814227b8714cf30e9a82f8d756b18f9dfdd081`. El motor principal sigue siendo `vsrender_native`: **no se sustituye por CraftEngine, BetterModel o ModelEngine**.

Última auditoría documentada contra el árbol original:

| Categoría | Archivos |
| --- | ---: |
| Blobs del módulo original | 3.192 |
| Idénticos al upstream por hash Git | 3.179 |
| Modificados intencionalmente | 13 |
| Dependencias JAR originales recuperadas y verificadas | 7 |
| Dependencias JAR pendientes | 0 |
| **Archivos originales no binarios migrados** | **3.185 / 3.185 (100 %)** |

Los dos archivos de metadatos/notas que faltaban, `.gitmodules` y `tasks/todo.md`, fueron restaurados idénticos al origen. Se recuperaron las siete JAR originales y se verificaron por hash Git: cinco en `libs/` y dos en `bukkit/compatibility/legacy/libs/`. La carpeta temporal `VSRENDER VERSION VIEJA/` fue retirada sin sobrescribir código portado. Core, Common Files y los subsistemas principales de Bukkit/Proxy ya están presentes. El cierre de la migración de archivos no significa que todas sus funciones estén listas o probadas para 26.3. Se preservan y auditan las adaptaciones del repositorio independiente y del port 26.3, incluidas recetas de datapack, tiempos de hornos, modelos Blockbench, scheduler Folia y NMS, además del README, GPLv3 y atribuciones upstream.

**Core, Bukkit, compatibilidad, native26 y Paper Loader ya compilaron y pasaron sus pruebas automáticas en GitHub Actions**; aún no se probó el funcionamiento integral dentro de un servidor Canvas/Folia 26.3 ni se publicó una JAR final.

## Auditoría de migración y compilación opcional

El workflow [VSRender build checks](.github/workflows/build.yml) puede validar la integridad de fuentes, pruebas Python y recursos **sin token privado**. Las siete dependencias originales ya están disponibles y verificadas en `main`. GitHub Actions puede intentar directamente la compilación integral sin configurar el secreto opcional `VSRENDER_SOURCE_TOKEN`. **La generación de la JAR final permanece desactivada** hasta realizar las actualizaciones previstas para VSEngine 26.3.

**Las credenciales del repositorio privado ya no son necesarias para compilar:** las siete JAR se recuperaron con el consentimiento del propietario y sus hashes coinciden con el árbol original. Solo si se extrajeran nuevamente se necesitaría configurar el secreto opcional de GitHub Actions. Procedimiento alternativo:

1. En la cuenta que puede autorizar el acceso al repositorio privado `DaniDevz/VirgolandStudio`, crear un fine-grained personal access token limitado a ese repositorio con permiso **Contents: Read-only**, fecha de vencimiento acotada y sin permisos adicionales.
2. En [Settings → Secrets and variables → Actions](https://github.com/JoseDaniel03/VSRender/settings/secrets/actions), crear un *repository secret* llamado exactamente **`VSRENDER_SOURCE_TOKEN`** y pegar ahí el token. Nunca colocarlo en archivos, mensajes o logs.
3. En [Actions → VSRender build checks](https://github.com/JoseDaniel03/VSRender/actions/workflows/build.yml), volver a ejecutar el workflow de `main` con el dev bundle `26.3.build.+`.
4. Inspeccionar **Compile and test (no plugin JAR)**: ahí se podrán identificar los errores reales de Java/Gradle, si los hay, sin crear el plugin distribuible.

Las siete JAR restauradas forman parte de `main` y se verifican mediante sus hashes Git originales. El runner solo accede al repositorio privado si faltase alguna y existieran credenciales. No se publican artefactos de compilación del plugin. No se ensamblará ni publicará la JAR final hasta completar la migración, implementar las actualizaciones 26.3 y revisar licencias y funcionamiento.

## Comprobaciones de código previas al empaquetado

El workflow usa Gradle 9.7.1, Java 25 y el dev bundle de Paper 26.3. Con las siete dependencias restauradas ejecuta pruebas y compilación, sin invocar el empaquetado final `:bukkit:paper-loader:shadowJar` ni publicar archivos. Gradle puede generar archivos intermedios de sus submódulos como parte de las dependencias de prueba:

```bash
./gradlew :core:test :bukkit:test :bukkit:compatibility:test :bukkit:native26:compileJava :bukkit:paper-loader:compileJava "-PvsrenderPaperDevBundle=26.3.build.+" --no-daemon --max-workers=2
```

**Política de desarrollo:** la migración de archivos (etapa 1) está cerrada. El siguiente paso es actualizar y probar VSEngine/Canvas/Folia 26.3 (etapa 2). Recién después se autorizará el empaquetado de la JAR. Las pruebas de carga con resource packs, modelos, animaciones, bloques, entidades, GUIs y scripts seguirán siendo imprescindibles.

## Avances de Minecraft 26.3

- Lectura estricta de recetas de datapacks: `cookingtime` obligatorio y entero positivo para cocción, con tests que preservan el formato anterior.
- `smoking` y `blasting`: valor predeterminado de 160 ticks en 26.3, con valores declarados explícitamente intactos.
- Modelos exportados desde Blockbench: conversión del antiguo `shade: false` al campo `shade_direction_override: "up"`.
- Backend NMS native26: sustituida una llamada eliminada por la API 26.3 sin alterar la política de daño de las hitboxes.
- Auditor de recursos/datapacks: avisa sobre modelos con shading antiguo, recetas incompletas, `pot_decorations` legado, tag eliminado de combustible de pociones, shaders antiguos y otros formatos incompatibles. No reescribe archivos de terceros.

**Comprobación automática:** [ejecución exitosa 39890487735](https://github.com/JoseDaniel03/VSRender/actions/runs/39890487735), con compilación Java completa en 26.3 y controles de origen sin diferencias inesperadas. Los cambios posteriores tienen sus propias ejecuciones de CI.

Consulta [la documentación técnica 26.3](docs/MINECRAFT-26.3.md) para detalles, cambios vanilla que ya cubre el motor y verificaciones pendientes en servidores reales.

## Capacidades que se preservan

- Modelos Blockbench `.bbmodel`, modelos 3D, rigs, animaciones, hitboxes y attachments.
- Ítems, bloques, muebles, rotaciones, conexiones y multibloques.
- Configuración YAML, scripts, interfaces, fuentes y textos.
- Compilación y generación de resource packs.
- Integración Paper / Canvas / Folia sujeta a la verificación final en 26.3.

## Documentación y licencia

**[Migración cerrada y auditoría](MIGRATION.md)** · **[Motor original](README-VSRENDER.md)** · **[Importación nativa de modelos](VSR-NATIVE-CONTENT.md)** · **[Compatibilidad 26.3](docs/MINECRAFT-26.3.md)**

VSRender deriva del código abierto [CraftEngine](https://github.com/Xiao-MoMi/craft-engine) y preserva la licencia **GPLv3** ([LICENSE](LICENSE)), los avisos upstream ([VSR-UPSTREAM.md](VSR-UPSTREAM.md)) y las atribuciones originales.
