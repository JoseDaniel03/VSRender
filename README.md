# VSRender

Motor autónomo de contenido y renderizado nativo para Minecraft / **VSEngine 26.3** (Canvas, Paper y Folia).

## Estado de la migración — 22/10/2026

El traslado desde `DaniDevz/VirgolandStudio/modules/vsrender-engine` está prácticamente completo. La referencia upstream permanece fijada en `88814227b8714cf30e9a82f8d756b18f9dfdd081`. El motor principal sigue siendo `vsrender_native`: **no se sustituye por CraftEngine, BetterModel o ModelEngine**.

Última auditoría documentada contra el árbol original:

| Categoría | Archivos |
| --- | ---: |
| Blobs del módulo original | 3.192 |
| Idénticos al upstream por hash Git | 3.179 |
| Modificados intencionalmente | 4 |
| Ausentes por decisión de migración | 9 |

Los nueve ausentes son metadatos/notas obsoletos y siete JAR externas de terceros que **no deben publicarse en el repositorio** sin revisar sus licencias. Core, Common Files y los subsistemas principales de Bukkit/Proxy ya están presentes. Se conservan las modificaciones verificadas para 26.3, GPLv3 y atribuciones upstream.

**Todavía no existe una JAR de VSEngine 26.3 compilada y probada.** La cobertura de fuentes no demuestra que el motor cargue correctamente en Canvas/Folia.

## Bloqueo actual de CI

El workflow [VSRender build checks](.github/workflows/build.yml) ya ejecutó correctamente pruebas de scripts de migración, auditoría estricta de fuentes, configuración de Java 25 y procesamiento de recursos de Core/Common Files. La compilación Java completa aún **no se ejecutó** porque GitHub Actions no dispone del secreto `VSRENDER_SOURCE_TOKEN` requerido para leer las siete JAR de compilación del repositorio privado original. **La generación de la JAR final está desactivada intencionalmente** hasta completar el 100 % de la migración y realizar las actualizaciones previstas para VSEngine 26.3.

La integración GitHub de ChatGPT **no suministra automáticamente credenciales** a GitHub Actions. Para desbloquear el workflow:

1. En la cuenta que puede autorizar el acceso al repositorio privado `DaniDevz/VirgolandStudio`, crear un fine-grained personal access token limitado a ese repositorio con permiso **Contents: Read-only**, fecha de vencimiento acotada y sin permisos adicionales.
2. En [Settings → Secrets and variables → Actions](https://github.com/JoseDaniel03/VSRender/settings/secrets/actions), crear un *repository secret* llamado exactamente **`VSRENDER_SOURCE_TOKEN`** y pegar ahí el token. Nunca colocarlo en archivos, mensajes o logs.
3. En [Actions → VSRender build checks](https://github.com/JoseDaniel03/VSRender/actions/workflows/build.yml), volver a ejecutar el workflow de `main` con el dev bundle `26.3.build.+`.
4. Inspeccionar **Compile and test (no plugin JAR)**: ahí se podrán identificar los errores reales de Java/Gradle, si los hay, sin crear el plugin distribuible.

Las JAR privadas se obtienen **temporalmente en el runner**, se comprueban por hash Git frente al upstream fijado y no se añaden a Git ni se publican como artefactos. No se ensamblará ni publicará la JAR final hasta completar la migración, implementar las actualizaciones 26.3 y revisar licencias y funcionamiento.

## Comprobaciones de código previas al empaquetado

El workflow usa Gradle 9.7.1, Java 25 y el dev bundle de Paper 26.3. Una vez restauradas las dependencias privadas, ejecuta exclusivamente tareas de pruebas y compilación de fuentes (sin `shadowJar`):

```bash
./gradlew :core:test :bukkit:test :bukkit:compatibility:test :bukkit:native26:compileJava :bukkit:paper-loader:compileJava "-PvsrenderPaperDevBundle=26.3.build.+" --no-daemon --max-workers=2
```

**Política de desarrollo:** primero terminar de migrar y auditar el código, después aplicar las actualizaciones necesarias para VSEngine/Canvas/Folia 26.3, y recién entonces autorizar el empaquetado de la JAR. Las pruebas de carga con resource packs, modelos, animaciones, bloques, entidades, GUIs y scripts seguirán siendo imprescindibles.

## Capacidades que se preservan

- Modelos Blockbench `.bbmodel`, modelos 3D, rigs, animaciones, hitboxes y attachments.
- Ítems, bloques, muebles, rotaciones, conexiones y multibloques.
- Configuración YAML, scripts, interfaces, fuentes y textos.
- Compilación y generación de resource packs.
- Integración Paper / Canvas / Folia sujeta a la verificación final en 26.3.

## Documentación y licencia

**[Migración y origen](MIGRATION.md)** · **[Motor original](README-VSRENDER.md)** · **[Importación nativa de modelos](VSR-NATIVE-CONTENT.md)** · **[Compatibilidad 26.3](docs/MINECRAFT-26.3.md)**

VSRender deriva del código abierto [CraftEngine](https://github.com/Xiao-MoMi/craft-engine) y preserva la licencia **GPLv3** ([LICENSE](LICENSE)), los avisos upstream ([VSR-UPSTREAM.md](VSR-UPSTREAM.md)) y las atribuciones originales.
