# NegativeUtils — guía de uso

Mod de utilidades para **Minecraft 1.20.1 y Minecraft Forge**. Añade herramientas de juego, bloques programables con redstone, waypoints, hermandades, una enciclopedia de descubrimientos, una guía visual, un contador de servidor y emotes de Discord.

## Requisitos e instalación

- Minecraft **1.20.1**.
- **Minecraft Forge 47 o posterior** para Minecraft 1.20.1.
- **Java 17**.
- Instala el mismo JAR de NegativeUtils en el cliente y en el servidor Forge al jugar en multijugador. En un mundo individual basta con instalarlo en el cliente.

Para compilar desde el código fuente en Windows:

```powershell
.\gradlew.bat build
```

El JAR se genera en `build\libs\`. Copia ese archivo a la carpeta `mods` del cliente y, para multijugador, también a `mods` del servidor. Reinicia Minecraft después de instalarlo.

Los objetos y bloques del mod están en la pestaña del modo creativo **NegativeUtils**.

## Controles

Las teclas se pueden cambiar desde **Opciones → Controles → NegativeUtils**:

| Tecla predeterminada | Acción |
| --- | --- |
| `G` | Abrir la enciclopedia |
| `K` | Abrir la interfaz de hermandades |
| `H` | Mostrar u ocultar la guía de ruta |

Los comandos de operador se ejecutan en el chat o en la consola del servidor. Los comandos indicados como **operador** requieren nivel de permiso 2.

## Objetos y bloques

### Varita de waypoints

1. Sostén la **Varita de waypoints** y haz clic derecho en un bloque para abrir la interfaz de creación.
2. Escribe un nombre opcional, selecciona un color en la rueda cromática y pulsa **Listo**.
3. El waypoint se guarda en el servidor y se sincroniza con los jugadores. Aparece como marcador con nombre y distancia, en su dimensión correspondiente.
4. Para eliminar un waypoint, apunta hacia su marcador y haz clic izquierdo con la varita. Los jugadores pueden borrar los suyos; los operadores pueden borrar cualquiera.

Un operador también puede eliminar todos los waypoints con `/negativeutils waypoints clear`.

### Varita de ruta

La **Varita de ruta** permite a los operadores marcar una ruta persistente:

- **Clic derecho en un bloque:** añade un punto a la ruta.
- **Clic izquierdo en un bloque:** elimina el punto más cercano de esa dimensión.
- Pulsa `H` para mostrar u ocultar el trazado.
- Usa `/trail` para abrir la interfaz de color y opacidad.

La ruta se comparte desde el servidor. La opacidad de la interfaz va de 0 % (invisible) a 100 % (opaca).

### PALITOMAGICO

Haz clic derecho sobre un jugador con **PALITOMAGICO** para teletransportarlo a su punto de reaparición. Si no tiene uno disponible, se usa el punto de aparición del mundo. Se muestra humo negro en el origen y en el destino.

### Bloque de tiempo

Coloca el **Bloque de tiempo**, haz clic derecho para abrir su interfaz e indica la demora en segundos (de 1 a 86 400). Al recibir un nuevo pulso de redstone, comienza la cuenta y después emite una señal de redstone de nivel 15 durante un breve pulso.

### Bloque de secuencias

Coloca el **Bloque de secuencias** y haz clic derecho para abrir el editor multilínea. Introduce comandos normales de Minecraft, uno por línea, y pulsa **Guardar**. Al recibir una nueva señal de redstone, ejecuta las líneas hasta terminar o hasta encontrar una pausa.

Escribe `wait <segundos>` en una línea para esperar antes de continuar. Por ejemplo:

```text
say "Hola"
wait 2
say "Han pasado dos segundos"
```

Las líneas vacías se ignoran. Se admiten pausas de hasta 86 400 segundos. Las líneas consecutivas sin `wait` se ejecutan seguidas, sin pausa. La secuencia se ejecuta con permisos de comando equivalentes a nivel 2. Editar el bloque requiere ser operador.

## Interfaces y mecánicas

### Enciclopedia (`G`)

La enciclopedia organiza sus entradas en las categorías **Item**, **Bloque** y **Mob**. Las entradas se desbloquean al obtener el objeto correspondiente en el inventario o al matar el mob correspondiente. Las entradas aún no descubiertas aparecen ocultas; al descubrir una entrada configurada, el jugador recibe su notificación.

Los operadores pueden activar el modo de administración dentro de la interfaz para crear o eliminar entradas. Cada entrada define una categoría, un identificador de registro como `minecraft:zombie`, una descripción y el texto de notificación.

### Hermandades (`K`)

La interfaz permite crear una hermandad con nombre y color, buscar hermandades existentes y solicitar unirse. También muestra las invitaciones recibidas, que se pueden aceptar o rechazar.

Dentro de una hermandad hay pestañas para:

- **Chat:** enviar mensajes a los miembros.
- **Tablón:** consultar anuncios; los líderes y oficiales pueden publicar.
- **Ubicaciones:** guardar ubicaciones del servidor; los líderes y oficiales pueden añadirlas.
- **Estandarte:** guardar el estandarte que se lleva en la mano; disponible para líderes y oficiales.
- **Miembros:** consultar miembros y abandonar o borrar la hermandad según el rango.
- **Jugadores online:** buscar jugadores e invitar a los que no pertenecen a una hermandad.

Los líderes pueden gestionar las solicitudes para unirse. El comando `/guild motd <texto>` cambia el MOTD de la hermandad; solo pueden usarlo el líder y los oficiales de una hermandad.

### Panel de administración y contador

`/adminpanel` abre el panel de operador. Permite fijar una fecha y hora futuras, personalizar el texto, el color (campo hexadecimal o rueda cromática) y la posición del contador. El contador se sincroniza con los jugadores; **Quitar contador** lo elimina para todos.

### Emotes de Discord en el chat

El chat incluye el botón **Emotes**, que abre un selector paginado. Al elegir un emote, se inserta en el borrador del mensaje; también se puede escribir el nombre como `:nombre:`. Los emotes personalizados requieren que el servidor los sincronice y que los clientes tengan NegativeUtils.

Para conectar un bot de Discord, un operador ejecuta `/negativeutils discord token` y pega el token en la pantalla enmascarada. El token se envía al servidor y se guarda en su directorio `config`; **no lo compartas en el chat**. El bot debe estar añadido a los servidores de Discord cuyos emotes se quieran usar.

## Comandos

| Comando | Permiso | Función |
| --- | --- | --- |
| `/yo` | Cualquier jugador | Teletransporta al jugador a la ubicación configurada con `/momento`. |
| `/momento set ubicacion <x> <y> <z>` | Operador | Guarda la ubicación de `/yo` en la dimensión actual. |
| `/trail` | Operador | Abre los ajustes visuales de la ruta. |
| `/trail color <rojo> <verde> <azul> <opacidad>` | Operador | Ajusta el color RGB y la opacidad; cada valor va de 0 a 255. |
| `/adminpanel` | Operador | Abre el panel de administración del contador. |
| `/guild motd <texto>` | Líder u oficial de una hermandad | Cambia el MOTD de la hermandad. |
| `/negativeutils waypoints clear` | Operador | Elimina todos los waypoints del servidor. |
| `/negativeutils discord token` | Operador | Abre la pantalla segura para configurar el token del bot. |
| `/negativeutils discord sync` | Operador | Solicita sincronizar los emotes. |
| `/negativeutils discord status` | Operador | Muestra el estado de la conexión y la cantidad de emotes sincronizados. |
| `/negativeutils activar hermandades` | Operador | Activa la función de hermandades para todos. |
| `/negativeutils desactivar hermandades` | Operador | Desactiva la función de hermandades para todos. |
| `/negativeutils activar enciclopedia` | Operador | Activa la enciclopedia para todos. |
| `/negativeutils desactivar enciclopedia` | Operador | Desactiva la enciclopedia para todos. |

## Persistencia y multijugador

Los waypoints, las rutas, las hermandades, las entradas de enciclopedia, los descubrimientos, las opciones de funciones y el contador se guardan en los datos del mundo del servidor. Los elementos de interfaz y renderizado se muestran en el cliente; las acciones compartidas se procesan o sincronizan con el servidor.
