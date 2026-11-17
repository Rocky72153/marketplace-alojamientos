# Acta de cierre del proyecto

## Marketplace de Alojamientos

**Fecha de elaboración:** 16/11/2026
**Estado:** Lista para firmar en la sustentación del 26/11/2026. Las firmas y la aceptación del docente se registran ese día; este documento no las da por hechas.
**Tarea en Jira:** MARKET-81

## Información del proyecto

| Dato | Descripción |
|---|---|
| Organización | Universidad El Bosque, Programa de Ingeniería de Sistemas, periodo 2026-2 |
| Curso | Ingeniería de Software I |
| Cliente | Inversiones LR |
| Patrocinador | Ing. David Esteban Rodríguez Gómez, docente |
| Product Owner y Developer | Juan Sebastián Morales Forero |
| Scrum Master, gerente del proyecto y Developer | Julián David Aguilera Párraga |
| Inicio | 19/10/2026 (Sprint 1) |
| Entrega final | 26/11/2026 |
| Versión entregada | [v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0) |

## Alcance entregado

Las 19 historias del Product Backlog (106 puntos) están terminadas en Jira e integradas en `main`:

| Épica | Historias | Pull Requests |
|---|---|---|
| Identidad y perfiles | HU-01 a HU-03 | #13, #33, #34 |
| Publicaciones y disponibilidad | HU-04 a HU-06 | #13 |
| Búsqueda y detalle | HU-07 y HU-08 | #13, #14 |
| Cotización, reserva, historial y cancelación | HU-09 a HU-12 | #15 |
| Notificaciones y reseñas | HU-13 a HU-15 | #26, #27 |
| Administración, dashboard y reportes | HU-16 a HU-18 | #29, #30 |
| Trazas y logs estructurados | HU-19 | #31, #32 |
| Pagos con Stripe en modo de prueba, interfaz y diagrama de clases | (mejoras del Sprint 5) | #35 a #37 |

Además de lo que pide la guía, se integró Stripe en modo de prueba (sandbox) con confirmación por evento firmado y reembolsos.

## Entregables

| Entregable | Ubicación |
|---|---|
| Código fuente, documentación y diagramas | [Repositorio en GitHub](https://github.com/Rocky72153/marketplace-alojamientos), rama `main` |
| Versión entregada | [Release v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0) |
| Demostración publicada | [moralesforero.lol](http://moralesforero.lol/) (AWS EC2, datos ficticios, Stripe en modo de prueba) |
| Backlog, sprints y trazabilidad de historias | [Tablero de Jira MARKET](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2) |
| Documentación por fases | [Espacio MARKET en Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview) |
| Diagramas (BPMN, casos de uso, dominio, C4, componentes, paquetes, actividad, estados, secuencia, MER, despliegue y CI/CD) | `docs/entrega-1/` y [inventario en Confluence Fase 3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9142274) |
| Matriz de trazabilidad | [TRACEABILITY_TEMPLATE.csv](TRACEABILITY_TEMPLATE.csv) |
| Registro de eventos Scrum | [registro-eventos-scrum.md](evidencias/registro-eventos-scrum.md) |
| Evidencia de pruebas | [EVIDENCIA-ENTREGA-FINAL.md](EVIDENCIA-ENTREGA-FINAL.md) |
| Uso de IA | [uso-ia-2026-10.md](evidencias/uso-ia-2026-10.md) y [registro-uso-IA.md](evidencias/registro-uso-IA.md) |

## Calidad

- Integración continua en GitHub Actions: Backend CI (Maven, JUnit y JaCoCo), Frontend CI (lint, pruebas y build) y Documentation CI, en verde en `main`.
- SonarCloud con Quality Gate aprobado en `main`.

## Limitaciones conocidas al cierre

- La demostración usa HTTP sin cifrado y solo admite datos ficticios.
- La recuperación de contraseña es simulada (no hay proveedor de correo).
- La protección de las ramas `main` y `develop` no está activada.
- No hubo sesiones de validación con usuarios reales (sección 11.5 del prototipo).
- La cuenta de AWS de la demostración vence el 23/11/2026. El [plan alterno local](DESPLIEGUE-EC2.md#plan-alterno-para-la-entrega-del-26112026--market-73), a cargo de Juan Sebastián Morales Forero, cubre el criterio documental de MARKET-73 sin asegurar una disponibilidad futura que aún no se ha comprobado.

## Lecciones aprendidas

Detalle en la retrospectiva acumulada de [registro-eventos-scrum.md](evidencias/registro-eventos-scrum.md):

- La revisión rápida de los PR evita sprints sin entrega, como el Sprint 3.
- Las actas de los eventos Scrum deben registrarse el mismo día.
- Tener Jira, GitHub y Confluence enlazados desde el inicio facilitó la trazabilidad.

## Guion de la sustentación (26/11/2026)

| Parte | Contenido | Presenta |
|---|---|---|
| 1 | Problema de Inversiones LR, alcance y backlog priorizado (MoSCoW) | Product Owner |
| 2 | Metodología Scrum: sprints, velocidad, eventos y trazabilidad Jira, GitHub y Confluence | Scrum Master |
| 3 | Diseño: BPMN, casos de uso, C4, componentes, modelo de datos y despliegue en AWS | Scrum Master |
| 4 | Demostración en vivo con el guion de [6.4 Release y demostración final](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9273364): publicar, buscar, consultar disponibilidad, cotizar, reservar y pagar con Stripe de prueba, cancelar y administrar | Product Owner |
| 5 | Calidad: CI, SonarCloud, pruebas y pipeline CI/CD | Scrum Master |
| 6 | Limitaciones, lecciones aprendidas y preguntas | Ambos |

Plan de respaldo: si la demostración en AWS no está disponible, ejecutar el sistema en local con [LOCAL_SETUP.md](LOCAL_SETUP.md) y mostrar las capturas de [5.4 Interfaz publicada y evidencias](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/12255234).

## Aceptación

| Rol | Nombre | Firma | Fecha |
|---|---|---|---|
| Product Owner | Juan Sebastián Morales Forero | | |
| Scrum Master | Julián David Aguilera Párraga | | |
| Docente y patrocinador | Ing. David Esteban Rodríguez Gómez | | |
