# Mochila portátil: compatibilidad CanvasMC/Paper 26.3

## Problema observado
Al abrir una mochila con clic derecho en CanvasMC/Baguette 26.3, el plugin emitía:
`NoSuchMethodError: Bukkit.createInventory(InventoryHolder, int, net.momirealms.craftengine.libraries.adventure.text.Component)`.

**Causa:** VSRender reubica Adventure (shading) bajo `net.momirealms.craftengine.libraries.adventure`,
pero la API del servidor espera `net.kyori.adventure.text.Component`.
El bytecode de VSRender intentaba buscar una sobrecarga que no existe en el servidor.

## Solución
En `PortableShulkerItemBehavior.Session`, deserializar el título MiniMessage
con `AdventureHelper` **dentro del plugin** y transformarlo a `String`
formateado por `AdventureHelper.getLegacy().serialize(...)`.
Luego crear el inventario con `Bukkit.createInventory(this, 27, legacyTitle)`.

La sobrecarga `String` está marcada como deprecated en Paper, pero es apta
como puente binario para este plugin con Adventure reubicado y existe
en Bukkit/Paper 26.x. Se mantiene el título coloreado; no se altera la
persistencia de los 27 slots.

El bloque de apertura también elimina `OPEN_IDENTITIES` ante `LinkageError`
para evitar bloquear permanentemente la mochila tras un fallo de API.

## Reproducción y verificación
1. Compilar el plugin con GitHub Actions.
2. Inspeccionar `PortableShulkerItemBehavior$Session.class` con `javap -c -p`:
   la llamada final debe tener descriptor
   `org/bukkit/Bukkit.createInventory:(Lorg/bukkit/inventory/InventoryHolder;ILjava/lang/String;)Lorg/bukkit/inventory/Inventory;`.
3. Reemplazar la JAR antigua, hacer arranque **completo** del servidor y
   obtener `mochilas:mochila_basica`.
4. Probar abrir con clic derecho, guardar ítems, cerrar, reabrir,
   colocar con Shift+clic derecho y recuperar tras romper.
5. Confirmar que no aparece `NoSuchMethodError` en consola.

Esta corrección es limitada a **la mochila**. La compilación o pruebas unitarias
no sustituyen la validación en un servidor CanvasMC/Baguette real.

Referencia API: https://purpurmc.org/javadoc/org/bukkit/Bukkit.html
