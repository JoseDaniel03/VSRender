# GUI y comandos

GUI Studio visual sigue pendiente, pero ya podés crear una plantilla de GUI por comando.

## Crear con pack explícito
/vsr gui create cocina mesa_preparacion 3

El pack cocina debe existir y estar habilitado. El nombre no lleva espacios y debe utilizar caracteres simples. El último argumento es la cantidad de filas (1 a 6); si se omite, son 3 filas.

## Abrir dentro del juego
/vsr gui open cocina mesa_preparacion

## Forma abreviada
/vsr gui create prueba

La forma abreviada solo funciona cuando **hay exactamente un pack habilitado**. En caso contrario el comando mostrará un ejemplo y enumerará los packs disponibles. También acepta nombres en mayúsculas y los normaliza a minúsculas.

## Importante
La cantidad de filas no es el nombre de la GUI y no se usan comillas salvo que debas agrupar un argumento. No genera una cocina funcional ni una GUI de recetas dinámica; es una plantilla visual básica.
