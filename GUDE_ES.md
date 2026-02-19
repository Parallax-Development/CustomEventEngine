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
```

### Problemas comunes
- **No se cargan eventos**: asegúrate de que el YAML tenga la clave `event`.
- **Evento no encontrado**: revisa `event.id` o el nombre del archivo.
- **No se ejecuta**: usa `/cee reload` tras modificar archivos.

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

### Palabras clave reservadas
`event`, `id`, `trigger`, `conditions`, `flow`, `nodes`, `action`, `delay`, `scope`, `expansion`, `target`, `chunk_policy`, `chunk_unload_policy`

### Convenciones
- `event.id` en minúsculas con guiones o guiones bajos.
- Tipos como ids cortos: `spawn_lightning`, `players_online`.

## 4) Referencia de comandos y funciones

### Comandos del plugin
- `/cee help [comando]` (permiso `cee.help`)
- `/cee list [pagina]` y `/cee list all` (permiso `cee.view`)
- `/cee reload` y `/cee reload silent` (permiso `cee.admin`)
- `/cee event start <evento> [ubicacion]` (permiso `cee.admin`)
- `/cee event stop [ubicacion]` (permiso `cee.admin`)
- `/cee event status [ubicacion]` (permiso `cee.view`)
- `/cee event inspect [ubicacion]` (permiso `cee.admin`)
- `/cee player info <jugador>` (permiso `cee.view`)

### Elementos del DSL soportados actualmente
- `trigger.type = interval` con `every` (duración).
- `conditions.<key>.type = players_online` con `min`.
- `conditions.<key>.type = expression` con `expression`.
- `conditions.<key>.type = world_time` con `min` y `max`.
- `conditions.<key>.type = random_chance` con `chance`.
- `conditions.<key>.type = variable_equals` con `key` y `value`.
- `flow.nodes[].action = spawn_lightning`, `broadcast`, `clear_weather`, `set_time`, `send_participants`, `set_variable`.
- `flow.nodes[].delay = <duración>`.
- `flow.nodes[].config` para parámetros de la acción.
- `scope.type = chunk_radius` con `radius`.
- `target.strategy = random_loaded_chunk`.

### Limitaciones actuales
- `repeat` y `parallel` existen a nivel de modelo, pero no hay parsing YAML.
- `chunk_policy` y `chunk_unload_policy` existen, pero no se usan aún en runtime.

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
