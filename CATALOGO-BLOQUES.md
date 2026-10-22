# Catálogo de bloques personalizados VsRender

Los siete ejemplos actuales usan modelos personalizados sobre bloques nativos. Este listado distingue demos incluidas de propuestas: una entrada en el catálogo no significa que su modelo o comportamiento estén implementados.

## Demos incluidas

| Bloque | ID | Comportamiento |
| --- | --- | --- |
| Mesa | vsrender:demo_table | Modelo con patas, carrier que mantiene el suelo visible |
| Lámpara | vsrender:demo_lamp | Mano vacía alterna luz nativa 0/15 y modelo apagado/encendido |
| Silla | vsrender:demo_chair | Giro con Shift + clic derecho vacío |
| Sillón modular | vsrender:demo_sofa | Frente hacia quien lo coloca; unión lateral, brazos y esquinas |

| Estatua gigante | vsrender:demo_statue_giant | 3×3×5; 15 piezas nativas, retirada conjunta y drop único |

| Estantería | vsrender:demo_shelf | Cinco pisos; tres poses; intercambio y contenido persistente; requiere mod |
| Soporte invisible | vsrender:demo_invisible_stand | Un ítem visible; soporte nativo invisible; requiere mod |

## Próximos bloques propuestos

Las dimensiones son tamaño objetivo en bloques de mundo, no un límite fijo del motor. Para objetos grandes conviene repartir el modelo entre varias celdas: una estatua visual alta con un único bloque de colisión en el pedestal dejaría su parte superior sin interacción física.

| Bloque | Tamaño objetivo | Función a probar | Estado |
| --- | --- | --- | --- |
| Monumento / obelisco | 1 × 1 × 6 | Vertical, base y remate, colocación del conjunto | Propuesto |
| Fuente | 3 × 3 × 2 | Bordes conectables, centro y agua | Propuesto |
| Arco de entrada | 5 × 1 × 4 | Estructura grande con hueco transitable | Propuesto |
| Árbol decorativo | 3 × 3 × 5 | Tronco y copa, follaje sin oclusión del terreno | Propuesto |
| Poste de luz | 1 × 1 × 3 | Luz real, modelo vertical y rotación | Propuesto |
| Banco | 2 × 1 × 1 | Unión por extremos y respaldo | Propuesto |
| Barra / mostrador | 1 × 1 × 1 por módulo | Tramos rectos, extremos y esquinas | Propuesto |
| Armario | 2 × 1 × 2 | Puertas y almacenamiento | Propuesto |
| Cama personalizada | 2 × 1 × 1 | Cabeza/pies, validación de espacio y retirada conjunta | Propuesto |
| Mesa grande | 2 × 2 × 1 | Unión de tapas y patas sólo en bordes | Propuesto |
| Sillón individual | 1 × 1 × 1 | Variante independiente del modular | Propuesto |
| Maceta con planta | 1 × 1 × 1 | Modelo abierto y terreno visible | Propuesto |
| Barril | 1 × 1 × 1 | Rotación y almacenamiento | Propuesto |
| Cajones apilados | 1 × 1 × 1 | Variantes de altura y orientación | Propuesto |
| Cofre personalizado | 1 × 1 × 1 | Tapa e inventario | Propuesto |
| Cartel de pie | 1 × 1 × 2 | Dos caras, orientación y texto | Propuesto |
| Puerta personalizada | 1 × 1 × 2 | Dos mitades, apertura y colisión coherente | Propuesto |
| Portón doble | 3 × 1 × 3 | Apertura del conjunto y bloqueo | Propuesto |
| Valla personalizada | 1 × 1 × 1 por módulo | Conexiones y colisión de valla | Propuesto |
| Escalera personalizada | 1 × 1 × 1 | Esquinas y perfil físico de escalera | Propuesto |
| Tejado modular | 1 × 1 × 1 por módulo | Pendientes, cumbreras y esquinas | Propuesto |
| Chimenea | 1 × 1 × 3 | Partes verticales y partículas opcionales | Propuesto |

## Orden recomendado

1. Estatua gigante: demuestra tamaños libres mediante varias celdas y permite validar el ciclo completo de colocación/retirada.
2. Poste de luz y banco: amplían las demos actuales con altura y uniones.
3. Fuente y arco: prueban estructuras con huecos transitables.
4. Puertas, vallas y escaleras: requieren perfiles físicos específicos.
5. Almacenamiento: añadir inventarios a barriles, cofres y armarios.

## Estatua incluida

Valida espacio, altura y propiedad regional antes de colocar. Guarda orientación y número de pieza en estados nativos; no necesita entidades ni un registro de estructuras para persistir. La retirada conserva bloques ajenos y evita drops duplicados.

La orientación corregida del sillón se aplica a nuevas colocaciones. Los sillones existentes mantienen su estado; se pueden girar con Shift + clic derecho vacío.
