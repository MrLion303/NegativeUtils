# NegativeUtils

**NegativeUtils** es un mod de utilidades para **Minecraft 1.20.1 con Forge**, pensado principalmente para servidores, eventos, mapas personalizados y proyectos de **Negative Studios**.

El mod reúne herramientas de administración, creación de recorridos, puntos de referencia, automatización mediante redstone, cuentas regresivas y funciones relacionadas con Discord.

## Características

### Waypoints

Permite crear puntos de referencia personalizados dentro del mundo.

Con la **Varita de Waypoints**:

- Clic derecho sobre un bloque: crea un waypoint y abre su configuración.
- Puedes asignarle nombre y color.
- Clic izquierdo sobre un waypoint: lo elimina.
- Los waypoints se guardan en el servidor y se sincronizan con los jugadores.
- Cada waypoint puede mostrarse u ocultarse individualmente.

Comandos:

```text
/negativeutils waypoints lista
/negativeutils waypoints mostrar "<nombre>"
/negativeutils waypoints ocultar "<nombre>"
/negativeutils waypoints clear
```

### Senderos

El sistema de senderos permite crear recorridos formados por puntos consecutivos, siguiendo el funcionamiento de una polilínea.

Primero puedes crear un sendero:

```text
/negativeutils senderos crear "<nombre>"
```

Después utiliza la **Varita de Senderos**:

- Clic izquierdo: marca o reinicia el punto A.
- Clic derecho: añade B, C, D y los siguientes puntos.
- Si no hay un sendero seleccionado, el primer clic izquierdo crea automáticamente uno con un nombre disponible.

Los senderos son independientes entre sí y pueden mostrarse u ocultarse individualmente.

Comandos principales:

```text
/negativeutils senderos crear "<nombre>"
/negativeutils senderos select "<nombre>"
/negativeutils senderos mostrar "<nombre>"
/negativeutils senderos ocultar "<nombre>"
/negativeutils senderos eliminar "<nombre>"
/negativeutils senderos lista
/negativeutils senderos deseleccionar
```

El comando `select` selecciona el sendero y abre su pantalla de configuración.

Desde la pantalla puedes personalizar:

- Color.
- Opacidad.

Cada sendero conserva su propia configuración y sus propios puntos.

### Cuenta regresiva

El mod incorpora un sistema de cuenta regresiva para eventos.

El panel se abre con:

```text
/negativeutils contador
```

Solo los operadores pueden utilizarlo.

Permite establecer:

- Fecha final.
- Hora exacta.
- Texto.
- Color.
- Posición.

La cuenta puede mostrarse como:

- Bossbar.
- Actionbar.
- Scoreboard.
- Title.

Puedes controlar por comando si una cuenta regresiva aparece en pantalla sin pausarla ni reiniciar su tiempo. Usa el nombre con comillas si contiene espacios:

```text
/negativeutils contador mostrar "Nombre del contador"
/negativeutils contador ocultar "Nombre del contador"
```

También están disponibles los alias `activar` y `desactivar`:

```text
/negativeutils contador activar "Nombre del contador"
/negativeutils contador desactivar "Nombre del contador"
```

- **Mostrar / activar:** vuelve a mostrar esa cuenta regresiva a los jugadores.
- **Ocultar / desactivar:** deja de mostrarla, pero el tiempo sigue avanzando normalmente.
- Estos comandos requieren permisos de operador y no eliminan ni modifican la configuración del contador.

La cuenta utiliza una **fecha y hora final absoluta**, por lo que el tiempo continúa transcurriendo aunque el servidor permanezca apagado. Al volver a encenderlo, el mod calcula el tiempo restante usando la hora actual.

### Contadores rápidos: `contadorminus`

Este sistema crea contadores independientes sin abrir una interfaz. Solo se muestran en la bossbar y sus IDs no llevan espacios.

```text
/negativeutils contadorminus crear <id> <minutos>
/negativeutils contadorminus mostrar <id>
/negativeutils contadorminus ocultar <id>
/negativeutils contadorminus borrar <id>
```

Ejemplo: `/negativeutils contadorminus crear inicio 2` crea el contador `inicio` con una duración de 2 minutos, inicialmente oculto.

- **Mostrar:** inicia o reinicia el contador desde la duración completa.
- **Ocultar:** lo oculta y reinicia su duración completa.
- **Al llegar a cero:** se oculta automáticamente y queda reiniciado para la siguiente activación.
- Los contadores rápidos no abren el panel y no permiten configurar otra posición que no sea la bossbar.
- El ID debe ser único entre los contadores existentes. El tiempo permitido es de 1 a 525600 minutos.

### Bloque de tiempo

Permite retrasar una señal de redstone.

Configuración:

- Coloca el bloque.
- Haz clic derecho.
- Define el tiempo de espera.
- Conecta una señal de redstone.

Cuando recibe una activación, espera el tiempo configurado y genera el pulso de salida.

El rango permite establecer desde 1 segundo hasta 24 horas.

### Bloque de secuencias

Permite ejecutar una secuencia de comandos mediante redstone.

Ejemplo:

```text
say Comienza el evento
wait 5
say Han pasado cinco segundos
```

Las líneas de comandos se ejecutan en orden y `wait` permite introducir pausas entre ellas.

Esto resulta útil para:

- Eventos.
- Cinemáticas.
- Mapas personalizados.
- Mensajes automáticos.
- Teletransportes.
- Activación de mecanismos.

### Emotes de Discord

NegativeUtils incluye un sistema para utilizar emotes de Discord dentro del chat de Minecraft.

Comandos:

```text
/negativeutils discord token
/negativeutils discord sync
/negativeutils discord status
```

### Varita de Respawn

La **Varita de Respawn** permite enviar a un jugador a su punto de reaparición.

- Haz clic derecho sobre otro jugador.
- El mod busca su punto de reaparición.
- Si existe, lo transporta allí.
- Si no existe, utiliza el spawn principal del Overworld.
- Se genera un efecto de humo durante el transporte.

Visualmente utiliza la textura original del **palo de Minecraft**, pero mantiene un brillo de objeto encantado.

### `/yo`

El comando se mantiene como acceso rápido a una ubicación guardada.

```text
/yo
```

La ubicación se configura mediante:

```text
/momento set ubicacion <coordenadas>
```

La posición y dimensión se guardan en los datos persistentes del servidor.

### Persistencia y multijugador

Los sistemas importantes utilizan datos guardados del mundo y sincronización cliente-servidor.

Esto permite que:

- Los waypoints sobrevivan a reinicios.
- Los senderos sobrevivan a reinicios.
- La cuenta regresiva conserve su fecha final.
- Los jugadores que entren posteriormente reciban los datos actuales.
- Cada jugador pueda trabajar con su propia selección de sendero sin afectar la selección de los demás.

El mod está diseñado para funcionar como mod de **cliente y servidor** en servidores Forge.

## Compatibilidad

- **Minecraft:** 1.20.1
- **Forge:** 47.4.23
- **Java:** 17

## Desarrollo

NegativeUtils forma parte de los proyectos de **Negative Studios**.

La versión actual está en desarrollo y las funciones pueden seguir ampliándose o modificándose.

**Versión:** 1.0.3
