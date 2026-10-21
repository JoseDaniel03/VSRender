# Migración de VSRender

## Origen verificado
- Repositorio privado: `DaniDevz/VirgolandStudio`.
- Subdirectorio: `modules/vsrender-engine/`.
- Commit de referencia: `88814227b8714cf30e9a82f8d756b18f9dfdd081`.
- El módulo tiene 3.192 blobs (árbol recursivo auditado el 21/10/2026).
- La última build documentada es `26.9.2-vsrender.6`, probada sobre Paper 26.2, Java 25.

## Importar el código exacto sin reescribirlo

La acción manual `Import VSRender source` copia el **subdirectorio** del repositorio privado a la raíz de este repositorio y genera un commit en `main`. Incluye fuentes Java, recursos propios, wrapper Gradle, ejemplos, archivos de licencia y pruebas. Los JAR de terceros de `libs/` no se vuelven a publicar automáticamente. Se conservan los avisos GPLv3 y upstream.

Debido a que GitHub no permite a un workflow de un repositorio leer automáticamente otro repositorio privado que pertenece a otra cuenta, es necesario un token con **solo permiso de lectura** en el repositorio fuente:

1. En la cuenta GitHub `DaniDevz`, crear un fine-grained personal access token para el repositorio `VirgolandStudio` con `Contents: Read-only`.
2. En `JoseDaniel03/VSRender`, abrir **Settings → Secrets and variables → Actions → New repository secret**; registrar el token como `VSRENDER_SOURCE_TOKEN`.
3. Abrir **Actions → Import VSRender source → Run workflow** en la rama `main`.
4. Verificar que el workflow haya confirmado el commit y que el directorio `core/` y el archivo `gradle/wrapper/gradle-wrapper.jar` estén presentes.

El token no debe agregarse al código, archivos de configuración ni logs. Se utiliza únicamente durante el checkout temporal en el runner. El workflow no programa importaciones periódicas.

## Después de importar

- Compilar el código original con Java 25 y Gradle desde `./gradlew`.
- Pasar los tests de core, Bukkit, compatibilidad y empaquetado del paper-loader.
- Adaptar cuidadosamente el backend NMS 26.2 y los schedulers a VSEngine/Canvas 26.3.
- Verificar los demos con jugador real y resource pack. No confundir smoke en Paper 26.2 con compatibilidad 26.3.
- No instalar CraftEngine, BetterModel ni ModelEngine como runtime obligatorio: el provider nativo se llama `vsrender_native`.
- Respetar las condiciones de distribución de los binarios terceros incluidos en `libs/` y conservar los avisos de licencia de dependencias.

**Importante:** este repositorio no debe anunciar una build para 26.3 hasta pasar sus pruebas reales.


## Dependencias externas en la migración pública

El módulo original contiene JAR de terceros en `libs/`. El workflow de importación mantiene los archivos Java, modelos, texturas y licencias, pero **excluye `libs/*.jar` del commit público** hasta auditar sus derechos de redistribución. El workflow de compilación obtiene temporalmente esas bibliotecas del repositorio original privado usando `VSRENDER_SOURCE_TOKEN` y no las guarda en `main`. Tampoco publica automáticamente el JAR generado: antes se verificará que la distribución del binario ensamblado respete todas las licencias y que el motor arranque en VSEngine 26.3. Se deberá reemplazar cualquier dependencia privada no redistribuible por una alternativa legal o fuente autorizada.
