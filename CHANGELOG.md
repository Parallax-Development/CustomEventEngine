# Changelog

## [1.0.9] - 2026-02-20

### Fixed
- Variables: la escritura a variables globales ahora requiere que la variable esté declarada como `scope: global` en el evento. Esto evita que eventos “ensucien” globales por accidente y que valores persistan entre ejecuciones.

## [1.0.8] - 2026-02-19

### Added
- Nodo `loop` en flow: repite una subsecuencia N veces.
- Nodo `condition_loop` en flow: repite una subsecuencia N veces mientras una expresión MVEL sea true en cada iteración.

## [1.0.7] - 2026-02-19

### Added
- Pack compatible inicial del DSL: nuevos triggers, conditions y actions orientados a eventos Bukkit y participantes.

## [1.0.6] - 2026-02-19

### Added
- Flag `--include-schedulers` para `/cee event purge*`: además desregistra schedulers `interval` de eventos purgados.


## [1.0.5] - 2026-02-19

### Added
- Comando `/cee event purge`: permite purgar runtimes activos por chunk/mundo/región, liberando locks y tasks.

## [1.0.4] - 2026-02-19

### Fixed
- Detección por chunk: `/cee event status|stop|inspect` ahora intenta resolver x/z como bloques y como chunks para evitar falsos negativos.

### Added
- Config `debug.chunk-lookup` para logs detallados de búsqueda por chunk.

## [1.0.3] - 2026-02-19

### Fixed
- `/cee event status`: ahora también muestra eventos pendientes por trigger `interval`.
- Trigger `interval`: el scheduling queda integrado al scheduler del runtime para mantener estado consistente.
- `/cee event stop`: ahora desregistra el scheduling por intervalo del evento detenido.

## [1.0.2] - 2026-02-19

### Added
- Variables por evento (`event.variables`) con scope local/global, tipos y valores iniciales (incluye referencias y expresiones MVEL).
- Interpolación de variables en mensajes de acciones (`{var}` / `${var}`).

### Changed
- Acción `set_time` ahora acepta `${var}` y `= expr` en runtime.
- Persistencia: el snapshot guarda solo variables locales (las globales se re-inicializan desde definiciones).

## [1.0.1] - 2026-02-19

### Added
- Trigger `command`: permite disparar un evento al ejecutar un comando (con defaults a `/cee event start <event>`).

### Changed
- Documentación: comandos `/cee event *` ahora describen el formato `[mundo x z]`.
