# VSRender — cierre de migración de fuentes

**Etapa 1: completada en `main` (22/10/2026).**
**Etapa 2: actualizaciones y compatibilidad VSEngine / Canvas / Folia 26.3 pendientes.**
**No generar ni publicar la JAR final en esta etapa.**

## Procedencia y referencia congelada

- Origen privado, solo lectura: `DaniDevz/VirgolandStudio`.
- Subdirectorio original: `modules/vsrender-engine/`.
- Commit original: `88814227b8714cf30e9a82f8d756b18f9dfdd081`.
- Destino independiente: `JoseDaniel03/VSRender`, rama `main`.
- Motor preservado: `vsrender_native`. No sustituirlo por otro motor.
- Mantener `LICENSE` (GPLv3), `VSR-UPSTREAM.md` y avisos de terceros.

## Auditoría de cierre

La comparación con el árbol original consta de **3.192 archivos**:

| Clasificación | Archivos | Explicación |
| --- | ---: | --- |
| Idénticos al origen (Git blob SHA) | **3.179** | Incluye `.gitmodules` y `tasks/todo.md` recuperados sin cambios. |
| Adaptados deliberadamente | **6** | El README, el `.gitignore` reforzado y cuatro archivos del port 26.3. |
| Excluidos legalmente del repositorio público | **7** | Bibliotecas JAR de terceros, disponibles solo como entradas externas de compilación. |
| **Total inventariado** | **3.192** | No faltan clases, recursos ni documentos originales que deban publicarse. |

**Cobertura de archivos originales no binarios de terceros: 3.185 de 3.185 (100 %).**
Esta cifra significa *migración de archivos*, no que las funciones estén listas para VSEngine 26.3 ni que se haya compilado el plugin.

La auditoría automática basada en los manifiestos `porting/upstream-manifest/` cubre **3.185 entradas** (sin el README independiente, `.gitmodules` y cinco de las JAR privadas). Con todas las fuentes presentes, el resultado esperado es **3.178 idénticas**, **5 cambios permitidos**, **2 JAR privadas ausentes** y **0 otras diferencias**. `.gitmodules` se coteja adicionalmente con su hash original `4facef73050191ebedcf6139fda688694f42b9c0`; `README.md` se exige como documento independiente.

Los cambios intencionales conservados son:
- `README.md` (documentación del repositorio independiente).
- `.gitignore` (protección explícita contra el commit accidental de JAR privadas).
- `bukkit/src/main/java/net/momirealms/craftengine/bukkit/plugin/scheduler/impl/AbstractBukkitExecutor.java` (preserva coordenadas de región al programar tareas diferidas en Folia).
- `bukkit/native26/build.gradle.kts` (bundle seleccionable 26.3).
- `core/src/main/java/net/momirealms/craftengine/core/item/processor/ComponentsProcessor.java`.
- `core/src/main/java/net/momirealms/craftengine/core/item/processor/ItemProcessors.java`.

Los dos últimos preservan la transición de componentes de ítems para 26.3 y no deben sobrescribirse con el archivo upstream.

### Siete dependencias de terceros no redistribuidas

Estas rutas siguen **fuera de Git público**, por motivos de licencias y de separación de dependencias, sin contar como código fuente pendiente:

- `libs/AxiomPaperPlugin-5.0.4.jar`
- `libs/denizen-1.3.3.jar`
- `libs/jni-internal-lookup-1.9.jar`
- `libs/leafpile-1.0.0.1.jar`
- `libs/worldguard-bukkit-7.0.14-dist.jar`
- `bukkit/compatibility/legacy/libs/awsm-api.jar`
- `bukkit/compatibility/legacy/libs/flow-nbt-2.0.2.jar`

Sus hashes de contenido están fijados en `.github/workflows/build.yml`. Si se requiere compilar Java, el runner puede recuperarlas temporalmente del repositorio privado con el secreto `VSRENDER_SOURCE_TOKEN` (`Contents: Read-only`); **no** se publican ni se añaden a commits. No incluirlas por la fuerza para obtener un «100 %» artificial.

### Referencias de submódulos y trabajo histórico

El archivo `.gitmodules` es idéntico al original y enumera tres proyectos externos upstream (`wiki`, `client-mod`, `proxy`). Su contenido externo no forma parte de los 3.192 archivos propios auditados ni se ha incorporado al motor autónomo. El archivo `tasks/todo.md` se conserva literalmente como **registro histórico**, con tareas que pueden ser trabajo técnico de la etapa 2: sus casillas no certifican funciones terminadas.

## Cómo repetir la auditoría, sin JAR

```bash
python3 -m py_compile porting/verify_original.py porting/audit_26_3.py porting/apply_26_3.py
python3 -m unittest discover -s porting -p 'test_*.py'
python3 porting/verify_original.py --strict-source
python3 porting/audit_26_3.py
```

`--strict-source` exige que estén presentes todos los archivos no binarios originales y que los hashes de fuentes, recursos y metadatos coincidan con la revisión fijada, excepto las cinco adaptaciones documentadas incluidas en el manifiesto. `--strict` es un modo exhaustivo que **fallará deliberadamente** mientras las dos JAR del manifiesto estén excluidas del árbol público.

El workflow `.github/workflows/import-vsrender.yml` fue **retirado como importador** y conservado como auditoría manual de solo lectura. No volver a ejecutar una copia indiscriminada del original: podría revertir código portado o introducir binarios de terceros.

El workflow `.github/workflows/build.yml` audita fuentes y recursos **sin token**. Si se proporciona el secreto, habilita también tests y compilación de código Java, sin tarea `shadowJar` ni JAR final.

## Etapa 2 — trabajo expresamente pendiente

La cobertura completa de fuentes no implica compatibilidad del runtime. Se deberá revisar y actualizar:

- APIs Paper y NMS para Minecraft / VSEngine 26.3.
- Scheduler y acceso seguro al mundo/entidades en Folia y Canvas.
- Formatos de resource pack y datapack, recetas, shaders y componentes de ítems.
- Rigs, animaciones, hitboxes, attachments y renderizado `vsrender_native`.
- Ítems, bloques, furniture, colisiones y GUI.
- Scripts, configuración, persistencia y servicios de red.
- Tests de comportamiento, validación en servidor y revisión de licencias.

**La JAR final se pospone** hasta completar estas actualizaciones y su validación. La migración de archivos (etapa 1) queda cerrada de forma independiente.
