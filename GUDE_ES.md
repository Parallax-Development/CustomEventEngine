# Guía del Event Engine DSL (CustomEventEngine)

## 1) Introducción conceptual
El **Event Engine DSL** es un lenguaje basado en YAML que te permite definir eventos automatizados en tu servidor sin escribir código Java. Cada archivo YAML dentro de `plugins/CustomEventEngine/events/` describe **qué disparadores activan el evento**, **qué condiciones deben cumplirse**, y **qué acciones se ejecutan**.

### Propósito
- Automatizar comportamientos de servidor por intervalos o condiciones.
- Mantener una configuración clara y editable por admins no programadores.
- Permitir escalabilidad y modularidad del contenido.

### Arquitectura (visión general)
```
[YAML en /events]
        |
        v
EventLoader -> EventDefinition -> FlowCompiler -> ExecutionPlan
        |                                   |
        v                                   v
Trigger + TargetResolver              RuntimeScheduler
        |                                   |
        v                                   v
TerritorialTriggerDispatcher       EventRuntime.tick()
```

### Flujo conceptual (paso a paso)
```
Trigger (interval) -> selecciona chunk -> startEvent()
   -> valida condiciones -> bloquea chunks -> ejecuta flow (acciones/delay)
```

### Integración con el plugin
- El engine se inicializa al arrancar el plugin.
- Carga definiciones desde `events/`.
- Registra triggers y arranca los schedulers.
- Expande el área del evento si está habilitado.

Implementaciones clave:
- EventLoader: carga y parseo de YAML.
- EventEngine: orquestación y ejecución runtime.

## 2) Instalación y configuración inicial

### Requisitos
- **Paper/Spigot 1.21+**
- **Java 21**

### Instalación rápida
1. Compila el plugin.
2. Copia el jar a `plugins/`.
3. Inicia el servidor para generar `config.yml`, `messages.yml` y la carpeta `events/`.

### Configuración base (`config.yml`)
```yaml
scheduler:
  tick-interval: 1
persistence:
  enabled: true
  interval-seconds: 60
expansion:
  enabled: true
  interval-ticks: 20
debug:
  chunk-lookup: false
```

### Problemas comunes
- **No se cargan eventos**: asegúrate de que el YAML tenga la clave `event`.
- **Evento no encontrado**: revisa `event.id` o el nombre del archivo.
- **No se ejecuta**: usa `/cee reload` tras modificar archivos.
- **El comando dice "no hay evento" pero sí existe**: activá `debug.chunk-lookup: true` y ejecutá `/cee event status <mundo> <x> <z>` para ver el mapeo de coordenadas a chunk en consola.

## 3) Fundamentos del DSL

### Estructura mínima
```yaml
event:
  id: "mi_evento"
  trigger:
    type: "interval"
    every: "60s"
  conditions:
    cond1:
      type: "players_online"
      min: 3
  flow:
    nodes:
      - action: "spawn_lightning"
      - delay: "5s"
```

### Tipos de datos soportados
- **String**: ids, nombres y tipos (`type`).
- **int**: cantidades (`min`, `radius`).
- **boolean**: `expansion.enabled`.
- **duraciones**: `ms`, `s`, `m`, `h`, `t` (ticks).

Ejemplos de duración:
- `500ms` ≈ 10 ticks
- `5s` = 100 ticks
- `1m` = 1200 ticks

### Variables (event.variables)
Podés declarar variables dentro de cada evento. Se guardan en el contexto del evento y se pueden usar desde condiciones/acciones.

Estructura:
```yaml
event:
  variables:
    fase:
      type: string
      scope: local
      initial: "inicio"
      description: "Estado del evento"
```

Campos:
- `type`: `string`, `number`, `boolean`, `array`, `object`.
- `scope`: `local` (por runtime) o `global` (compartida entre runtimes del engine).
- `initial` (opcional): valor inicial.
- `description` (opcional): texto descriptivo.

Reglas:
- El nombre debe ser `A-Za-z_` seguido de `A-Za-z0-9_`.
- Si `initial` no existe, se usa un default por tipo (ej: `0`, `false`, `[]`, `{}`, `""`).
- Si una variable es `global`, el engine junta las definiciones de todos los eventos. Si dos eventos declaran la misma variable global con distinto `type`, se considera error.

Inicialización avanzada:
- Referencia: `"${otra_variable}"` copia el valor de otra variable.
- Expresión: `"= <expr>"` evalúa una expresión MVEL al iniciar el runtime.

Ejemplo (dependencias + expresión):
```yaml
event:
  variables:
    base:
      type: number
      scope: global
      initial: 10
    bonus:
      type: number
      scope: local
      initial: "= base + 5"
    mensaje:
      type: string
      scope: local
      initial: "Listo"
```

### Interpolación en strings
En strings (por ejemplo `broadcast.message` o `send_participants.message`) podés inyectar variables:
- `{variable}`
- `${variable}`

Ejemplo:
```yaml
- action: "broadcast"
  message: "&aFase: {fase} | Bonus: ${bonus}"
```

### Palabras clave reservadas
`event`, `id`, `trigger`, `conditions`, `variables`, `flow`, `nodes`, `action`, `delay`, `scope`, `expansion`, `target`, `chunk_load_rules`, `chunk_unload_rules`

### Convenciones
- `event.id` en minúsculas con guiones o guiones bajos.
- Tipos como ids cortos: `spawn_lightning`, `players_online`.

## 4) Referencia de comandos y funciones

### Comandos del plugin
- `/cee help [comando]` (permiso `cee.help`)
- `/cee list [pagina]` y `/cee list all` (permiso `cee.view`)
- `/cee reload` y `/cee reload silent` (permiso `cee.admin`)
- `/cee event start <evento> [mundo x z]` (permiso `cee.admin`)
- `/cee event stop [mundo x z]` (permiso `cee.admin`)
- `/cee event status [mundo x z]` (permiso `cee.view`) (incluye eventos pendientes por intervalos)
- `/cee event inspect [mundo x z]` (permiso `cee.admin`)
- `/cee event purge chunk [mundo x z]` (permiso `cee.admin`)
- `/cee event purge world <mundo>` (permiso `cee.admin`)
- `/cee event purge region <mundo> <x1> <z1> <x2> <z2>` (permiso `cee.admin`)
- `/cee player info <jugador>` (permiso `cee.view`)

#### Nota: `/cee event status`
- Si hay un runtime bloqueando el chunk objetivo, se muestra como evento activo.
- Además, lista los eventos con trigger `interval` registrados y el tiempo restante (ticks) para el próximo disparo.
- `/cee event stop` desregistra el scheduling del evento detenido si estaba configurado con trigger `interval`.

#### Nota: `/cee event purge`
- Purga = cancela runtimes activos, elimina sus tasks runtime y libera los chunks bloqueados para reutilización.
- `purge chunk` acepta `<mundo x z>` y, si no encuentra runtime, intenta resolver `x/z` como coordenadas de bloque y como coordenadas de chunk.
- `purge region` toma un rectángulo y purga todo runtime que tenga algún chunk dentro. Interpreta `x/z` como coordenadas de bloque (y si no encuentra nada, reintenta como coordenadas de chunk).
- Flag `--include-schedulers`: además desregistra schedulers de trigger `interval` de los eventos purgados para que no vuelvan a dispararse automáticamente.

Ejemplos:
- `/cee event purge chunk world 100 200`
- `/cee event purge world world`
- `/cee event purge region world 0 0 512 512`
- `/cee event purge world world --include-schedulers`

### Elementos del DSL soportados actualmente
- `trigger.type = interval` con `every` (duración).
- `trigger.type = command` con `command` (opcional), `permission` (opcional) y `cancel` (opcional).
- `conditions.<key>.type = players_online` con `min`.
- `conditions.<key>.type = expression` con `expression`.
- `conditions.<key>.type = world_time` con `min` y `max`.
- `conditions.<key>.type = random_chance` con `chance`.
- `conditions.<key>.type = variable_equals` con `key` y `value`.
- `event.variables` para variables `local`/`global` con inicialización.
- `flow.nodes[].action = spawn_lightning`, `broadcast`, `clear_weather`, `set_time`, `send_participants`, `set_variable`.
- `flow.nodes[].delay = <duración>`.
- `flow.nodes[].config` para parámetros de la acción.
- `scope.type = chunk_radius` con `radius`.
- `target.strategy = random_loaded_chunk`.

### Tabla de actions
Los parámetros de las acciones se pueden definir de dos formas equivalentes:
- Como claves al mismo nivel del nodo (`message: ...`, `time: ...`, etc.)
- Dentro de `config:`

| Action type | Utilidad | Parámetros disponibles | Ejemplo mínimo |
|---|---|---|---|
| `broadcast` | Enviar mensaje a todos los jugadores del servidor. | `message` (string) | `- action: "broadcast"\n  message: "&aHola"` |
| `clear_weather` | Quitar lluvia/tormenta y reiniciar duraciones climáticas del mundo. | (sin parámetros) | `- action: "clear_weather"` |
| `send_participants` | Enviar mensaje solo a los participantes del evento. | `message` (string) | `- action: "send_participants"\n  message: "&eEvento activo"` |
| `set_time` | Ajustar el tiempo del mundo. | `time` (ticks/duración o `${variable}` o `= expr`) | `- action: "set_time"\n  time: "13000t"` |
| `set_variable` | Guardar una variable en el contexto del evento. | `key` (string), `value` (any, soporta `${var}` y `= expr`) | `- action: "set_variable"\n  key: "fase"\n  value: "inicio"` |
| `spawn_lightning` | Spawnear un rayo en la ubicación del evento. | (sin parámetros) | `- action: "spawn_lightning"` |

### Tabla de conditions
| Condition type | Utilidad | Parámetros disponibles | Ejemplo mínimo |
|---|---|---|---|
| `expression` | Evaluar una expresión MVEL para decidir si el evento puede ejecutarse. | `expression` (string) | `type: "expression"\nexpression: "playersOnline >= 5"` |
| `players_online` | Requerir un mínimo de jugadores conectados. | `min` (int) | `type: "players_online"\nmin: 5` |
| `random_chance` | Permitir ejecución con una probabilidad aleatoria. | `chance` (number). Acepta `0.0–1.0` o porcentaje (`25` = 25%). | `type: "random_chance"\nchance: 25` |
| `variable_equals` | Comparar una variable del contexto con un valor esperado. | `key` (string), `value` (string) | `type: "variable_equals"\nkey: "fase"\nvalue: "inicio"` |
| `world_time` | Validar que el tiempo del mundo esté dentro de un rango (ticks 0–23999). Soporta rangos que cruzan 24000. | `min` (ticks), `max` (ticks) | `type: "world_time"\nmin: "12000t"\nmax: "23000t"` |

### Triggers

#### Trigger `interval`
```yaml
trigger:
  type: "interval"
  every: "60s"
```

#### Trigger `command`
Activa el evento cuando un jugador ejecuta un comando.

- `command` (opcional): comando a escuchar. Si se omite, se usa `/cee event start <event>`.
- `permission` (opcional): permiso requerido para disparar el evento. Default: `cee.admin`.
- `cancel` (opcional): si `true`, cancela la ejecución del comando original. Default: `true`.

Ejemplo (comando personalizado):
```yaml
trigger:
  type: "command"
  command: "/tormenta"
  permission: "cee.admin"
  cancel: true
```

Ejemplo (default del plugin para este evento):
```yaml
trigger:
  type: "command"
```

### Limitaciones actuales
- `chunk_load_rules` y `chunk_unload_rules` se parsean, pero no se aplican aún en runtime.

## 5) Tutorial paso a paso

### Paso 1: evento mínimo
```yaml
event:
  id: "primer_evento"
  trigger:
    type: "interval"
    every: "30s" # cada 30 segundos
  flow:
    nodes:
      - delay: "5s" # espera antes de ejecutar la acción
      - action: "spawn_lightning"
```

### Paso 2: añadir condición
```yaml
event:
  id: "tormenta_con_gente"
  trigger:
    type: "interval"
    every: "1m"
  conditions:
    online:
      type: "players_online"
      min: 5
    probabilidad:
      type: "random_chance"
      chance: 25
  flow:
    nodes:
      - action: "spawn_lightning"
```

### Paso 3: scope + expansión
```yaml
event:
  id: "tormenta_expansiva"
  trigger:
    type: "interval"
    every: "2m"
  scope:
    type: "chunk_radius"
    radius: 0
  expansion:
    enabled: true
    max_radius: 2
    step: 1
    interval: "20t"
  flow:
    nodes:
      - action: "send_participants"
        message: "&e¡Evento activo!"
      - delay: "5s"
      - action: "clear_weather"
```

### Paso 4: probar y depurar
- `/cee reload`
- `/cee list`
- `/cee event start tormenta_expansiva`
- `/cee event inspect`

## 6) Casos de uso reales

### Evento manual en chunk específico
```yaml
event:
  id: "lluvia_manual"
  trigger:
    type: "interval"
    every: "10m" # trigger no se usará si lo inicias manualmente
  flow:
    nodes:
      - action: "spawn_lightning"
```

### Evento periódico con condiciones
```yaml
event:
  id: "tormenta_global_controlada"
  trigger:
    type: "interval"
    every: "5m"
  conditions:
    online:
      type: "players_online"
      min: 10
    horario:
      type: "world_time"
      min: "12000t"
      max: "23000t"
  flow:
    nodes:
      - delay: "2s"
      - action: "spawn_lightning"
```

## 7) Mejores prácticas y patrones
- Usar intervalos razonables para no saturar el scheduler.
- Combinar triggers con conditions para reducir cargas.
- Empezar con `scope.radius = 0` y escalar si es necesario.
- Evitar `expansion.max_radius` altos en servidores concurridos.

## 8) Solución de problemas
- **Evento no encontrado**: revisar `event.id`.
- **Chunk ocupado**: detener evento con `/cee event stop`.
- **Condiciones fallidas**: verificar `conditions` y `min`.
- **No hay runtime activo**: el evento no está en ese chunk.

## 9) Integración con otros sistemas
El engine expone una API para registrar acciones/condiciones/triggers personalizados.
Ejemplo conceptual de acción:
```java
public final class SaveMetricAction implements Action {
    private final DataSource dataSource;

    public SaveMetricAction(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(EventContext context) {
        // Guardar métricas en BD
    }
}
```

## 10) Testing y debugging
- Crear evento simple.
- Usar `/cee event start <id>`.
- Usar `/cee event status` y `/cee event inspect`.
- Revisar logs y `runtime.json` si está habilitada la persistencia.

## 11) Rendimiento y optimización
- Evitar triggers de alta frecuencia sin necesidad.
- Reducir expansión y scope en eventos intensivos.
- Monitorear eventos activos para evitar saturación.

## 12) Glosario y referencias rápidas
- **Event**: definición de comportamiento en YAML.
- **Trigger**: sistema que dispara el evento.
- **Condition**: filtro previo a la ejecución.
- **Flow**: secuencia de nodos (`action`, `delay`).
- **Scope**: chunks bloqueados por el evento.
- **Expansion**: expansión gradual del área.
- **Chunk target**: estrategia para seleccionar chunk.
- **Variables de contexto**: datos guardados en el runtime del evento.

Acciones nuevas:
- `clear_weather`: limpia lluvia y tormenta.
- `set_time`: ajusta el tiempo del mundo.
- `send_participants`: envía mensaje a participantes.
- `set_variable`: guarda una variable en el contexto.

Condiciones nuevas:
- `world_time`: valida rango de tiempo del mundo.
- `random_chance`: valida probabilidad aleatoria.
- `variable_equals`: compara una variable con un valor.

Duraciones comunes:
- `10t` = 10 ticks
- `500ms` ≈ 10 ticks
- `5s` = 100 ticks
- `1m` = 1200 ticks

## 13) Apéndices
- Compatibilidad: Paper/Spigot 1.21+, Java 21.
- No hay migraciones documentadas en el repositorio actual.
- Librerías: MVEL para condiciones `expression`.

## 14) Ejemplos rápidos (nuevos types)
```yaml
flow:
  nodes:
    - action: "set_variable"
      key: "fase"
      value: "inicio"
    - action: "send_participants"
      message: "&aFase: {fase}"
    - action: "set_time"
      time: "13000t"
    - action: "clear_weather"
```

```yaml
conditions:
  prob:
    type: "random_chance"
    chance: 0.2
  noche:
    type: "world_time"
    min: "12000t"
    max: "23000t"
  fase:
    type: "variable_equals"
    key: "fase"
    value: "inicio"
```
