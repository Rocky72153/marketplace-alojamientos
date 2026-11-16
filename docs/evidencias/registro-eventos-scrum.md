# Registro de eventos Scrum

**Proyecto:** Marketplace de Alojamientos (Inversiones LR)
**Equipo:** Juan Sebastián Morales Forero (Product Owner y Developer) y Julián David Aguilera Párraga (Scrum Master y Developer)
**Registro elaborado:** 16/11/2026, por el Scrum Master. Tarea en Jira: MARKET-74.

## Cómo se construyó este registro

Durante los sprints 1 a 4 no se guardaron actas de las reuniones en el momento en que ocurrieron. Este documento registra cada sprint con datos que se pueden comprobar: objetivo, fechas, alcance y resultado salen del [tablero de Jira](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2) y del historial de [Pull Requests](https://github.com/Rocky72153/marketplace-alojamientos/pulls?q=is%3Apr). No se inventan asistentes, duraciones ni comentarios de reuniones.

La retrospectiva acumulada de la sección final analiza esos datos y fija acciones de mejora con responsable y fecha para los sprints 5 y 6. La falta de actas a tiempo queda registrada como hallazgo del proceso.

## Resumen de sprints

| Sprint | Fechas (lunes 09:00, Bogotá) | Objetivo registrado en Jira | Puntos terminados | Incremento integrado |
|---|---|---|---|---|
| Sprint 1 | 19/10 a 26/10/2026 | Identidad, publicaciones, disponibilidad y búsqueda (HU-01 a HU-07) | 37 | PR #13 en develop el 22/10 |
| Sprint 2 | 26/10 a 02/11/2026 | Detalle de alojamiento (HU-08) y desarrollo de cotización, reserva, historial y cancelación (HU-09 a HU-12) | 3 | PR #14 en develop el 27/10 |
| Sprint 3 | 02/11 a 09/11/2026 | Terminar e integrar HU-09 a HU-12 | 0 | Ninguno: el PR #15 quedó esperando revisión |
| Sprint 4 | 09/11 a 16/11/2026 | Integrar en main HU-09 a HU-19 | 66 | PR #15, #26, #29 y #31 (y sus integraciones #27, #30, #32 a #34) |
| Sprint 5 | 16/11 a 23/11/2026 | Revisión de los PR #35 a #37, diagramas que faltan, demostración estable y registro de eventos Scrum | En curso | PR #35 a #38 y diagramas de diseño en main (commit 01440c9) |
| Sprint 6 | 23/11 a 26/11/2026 | Release v1.0, demostración final, sustentación y acta de cierre | Futuro | |

Total del Product Backlog: 19 historias y 106 puntos terminados. Velocidad media de los sprints 1 a 4: 26,5 puntos.

## Sprint 1

- **Planning (19/10):** se comprometieron HU-01 a HU-07 (MARKET-5 a MARKET-11) con sus subtareas.
- **Review (26/10):** las siete historias quedaron integradas en `develop` con el PR #13 el 22/10, con Backend CI y Frontend CI aprobados. 37 puntos terminados.

## Sprint 2

- **Planning (26/10):** HU-08 y el inicio de HU-09 a HU-12.
- **Review (02/11):** HU-08 integrada con el PR #14 el 27/10 (3 puntos). HU-09 a HU-12 siguieron en desarrollo en el PR #15 y pasaron al Sprint 3.

## Sprint 3

- **Planning (02/11):** terminar e integrar HU-09 a HU-12.
- **Review (09/11):** no se integró código nuevo; el PR #15 quedó esperando revisión. 0 puntos terminados. El 08/11 se reorganizó el plan a seis sprints (ADR registrada en `docs/DECISIONS.md`).

## Sprint 4

- **Planning (09/11):** integrar en `main` HU-09 a HU-19, más MARKET-68 y MARKET-69.
- **Review (16/11):** HU-09 a HU-19 quedaron en `main` el 12/11 (PR #15, #26, #29 y #31). La demostración se publicó en [moralesforero.lol](http://moralesforero.lol/) con Stripe en modo de prueba (PR #35 a #37, 16/11). 66 puntos terminados; el sprint se cerró en Jira el 16/11.

## Sprint 5: Planning (16/11/2026)

- **Objetivo:** revisión de los PR #35 a #37, diagramas que faltan, demostración estable y registro de eventos Scrum.
- **Alcance:** MARKET-70 a MARKET-79 y MARKET-82.
- **Responsables:** MARKET-73 (demostración en AWS hasta el 26/11) a cargo de Juan Sebastián Morales Forero; revisión, diagramas y registro Scrum a cargo de Julián David Aguilera Párraga.
- **Avance al 16/11:** PR #35 a #38 fusionados; los once diagramas de diseño quedaron en `main` (MARKET-75 a MARKET-79 y MARKET-82); revisión cerrada (MARKET-72).

## Retrospectiva acumulada (sprints 1 a 4)

### Qué funcionó

- Todo el código entró por Pull Request con CI (Backend, Frontend y Documentation) y SonarCloud en verde.
- El backlog completo (19 historias, 106 puntos) quedó terminado antes de la entrega final.
- Jira, GitHub y Confluence se mantuvieron enlazados: cada historia apunta a su PR.

### Qué no funcionó

- El ritmo fue irregular: 37, 3, 0 y 66 puntos. El Sprint 3 no entregó nada porque el PR #15 esperó revisión una semana.
- Ningún PR tiene aprobación registrada del otro integrante en GitHub.
- Las ramas `main` y `develop` no tienen protección activada.
- No se guardaron actas de Planning, Review ni Retrospective en el momento.

### Acciones de mejora

| Acción | Responsable | Fecha límite |
|---|---|---|
| Registrar Planning, Review y Retrospective de los sprints 5 y 6 el mismo día del evento, en Confluence 5.3 | Julián David Aguilera Párraga (Scrum Master) | 23/11 y 26/11/2026 |
| No dejar un PR más de un día sin revisión; si se bloquea, dividirlo | Ambos | Desde el 16/11/2026 |
| Activar la protección de `main` y `develop` con `scripts/configure-github-branch-protection.sh` (requiere permisos de administrador del repositorio) | Juan Sebastián Morales Forero (propietario del repositorio) | 23/11/2026 |
| Publicar el release v1.0 y llenar la columna Release de la matriz de trazabilidad | Julián David Aguilera Párraga | 16/11/2026 (hecho, MARKET-80) |
