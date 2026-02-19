# Changelog

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
