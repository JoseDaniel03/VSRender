# Plantillas de ítems

## Ingrediente
Material base, nombre, textura opcional, descripción y apilamiento. No es consumible por defecto.

## Alimento
Permite definir nutrición, saturación, tiempo de consumo, animación, si se puede comer con hambre llena y un efecto de estado con nivel, duración y probabilidad.

## Bebida
Las mismas opciones, con animación de beber y recipiente de retorno (opcional). El material base predeterminado es honey_bottle. Probá los componentes finales dentro de Minecraft antes de usar ítems valiosos.

## Plato genérico
Usá un solo ID por plato como guiso o ensalada. El formulario crea un ítem comestible. Para acumular ingredientes variables de diferentes recetas sigue haciendo falta la estación de cocina.

## Texturas
El campo textura acepta namespace:item/mi_comida, cuya PNG debe estar en:
resources/<pack>/resourcepack/assets/<namespace>/textures/item/mi_comida.png

La vista previa muestra PNG propios que existen dentro del resource pack. Si el item no tiene PNG, indica el material base en lugar de mostrar una imagen rota. No renderiza modelos 3D.

## Detalles técnicos
VSRender usa data.food y el componente minecraft:consumable. El formulario genera ambas partes para permitir consumir el ítem con el tiempo, la animación y el efecto indicados. Usa una lista limitada de efectos vanilla; para una configuración más compleja se puede editar YAML avanzado.

## Cultivos
Una semilla con bloque crop_block, etapas de crecimiento, texturas y loot requiere archivos adicionales. El formulario web todavía no genera un cultivo completo automáticamente.
