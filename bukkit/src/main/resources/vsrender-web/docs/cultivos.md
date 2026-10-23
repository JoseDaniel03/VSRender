# Cultivos y semillas

La pestaña **Plantillas → Verdura cultivable** genera un producto comestible, su semilla y una planta desde una única configuración YAML. No desarrolla motores nuevos: reutiliza `crop_block`, `bush_block` y `block_item`.

## Resultado
- `cocina:tomate`: alimento, textura y estadísticas de consumo.
- `cocina:tomate_semillas`: ítem de semilla que planta el cultivo.
- `cocina:tomate_cultivo`: bloque con edades, bonemeal, cosecha y botín.

## Parámetros
Elegí de 2 a 8 etapas y velocidad de crecimiento entre 0.01 y 1. También podés asociar texturas PNG existentes del pack para el producto y las semillas.

Las apariencias del cultivo utilizan **trigo vanilla como marcador visual**. Para que se vea realmente como un tomate, reemplazá las apariencias con modelos y texturas propias. La plantilla no dibuja imágenes.

La tabla de botín depende de `default:loot_table/seed_crop` del pack de recursos predeterminado. Verificá que el pack esté habilitado antes de probarlo en juego.

## Aplicación
Guardá, aplicá la configuración y actualizá el resource pack si cambiaste apariencias. Primero probá en tu servidor de desarrollo.
