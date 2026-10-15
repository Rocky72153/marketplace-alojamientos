# Entrega 1 — Análisis de requerimientos y diseño

**Proyecto:** Marketplace de alojamientos temporales para Inversiones LR  
**Curso:** Ingeniería de Software I · Ingeniería de Sistemas · Universidad El Bosque · 2026-2  
**Estado:** Juan Sebastián Morales y Julián Aguilera ratificaron el paquete documental en su estado actual el 14/10/2026; las validaciones externas/docentes y evidencias no realizadas siguen identificadas.

## Alcance según la guía oficial

La guía define Entrega 1 como **análisis de requerimientos y diseño** y Entrega 2 como **producto funcional, pruebas e integración continua** (p. 1). No exige un número fijo de casos de prueba ni ejecutar los 44 del diseño en Entrega 1. Sí pide que los tests automatizados de historias ya implementadas se ejecuten en CI (p. 5).

## Acta de constitución

- [Acta de constitución del proyecto en Word](acta-constitucion-marketplace.docx).
- [Acta de constitución del proyecto en Markdown](acta-constitucion-marketplace.md).

Las fechas oficiales del cronograma, la aprobación del acta y las firmas están pendientes.

## Navegación de artefactos

1. [Visión, objetivos y alcance](01-vision-alcance.md)
2. [Actores e interesados](02-actores-interesados.md)
3. [Procesos de negocio](03-procesos-negocio.md)
4. [Catálogo de requerimientos y reglas](04-catalogo-requerimientos.md)
5. [Épicas e historias de usuario](05-backlog-historias.md)
6. [Casos de uso](06-casos-de-uso.md)
7. [Modelo de dominio](07-modelo-dominio.md)
8. [Modelo lógico de datos](08-modelo-datos.md)
9. [Arquitectura orientada a servicios](09-arquitectura-servicios.md)
10. [Contrato REST OpenAPI](10-openapi.yaml)
11. [Prototipo y estrategia de validación](11-prototipo-validacion.md)
12. [Planificación, WBS, cronograma relativo y riesgos](12-planificacion-riesgos.md)
13. [Checklist contra la guía](13-checklist-entrega.md)
14. [Diagramas de secuencia](14-secuencias.md)
15. [Requisitos del negocio](15-requisitos-negocio.md)
16. [Ciclo de vida del software](16-ciclo-vida-software.md)
17. [Diseño de casos de prueba](17-casos-de-prueba.md)
18. [Desglose de subtareas y planificación Jira](18-plan-subtareas-jira.md)
19. [Metodología Scrum + Lean](19-metodologia-scrum-lean.md)

## Fuentes de diagramas y material complementario

- BPMN 2.0 de publicación: [bpmn/publicacion-alojamiento.bpmn](bpmn/publicacion-alojamiento.bpmn).
- BPMN 2.0 de búsqueda/reserva: [bpmn/busqueda-reserva-cancelacion.bpmn](bpmn/busqueda-reserva-cancelacion.bpmn).
- BPMN 2.0 de cancelación: [bpmn/cancelacion-reserva.bpmn](bpmn/cancelacion-reserva.bpmn).
- Matriz de trazabilidad inicial: [../TRACEABILITY_TEMPLATE.csv](../TRACEABILITY_TEMPLATE.csv).
- Registro de decisiones/preguntas abiertas: [../DECISIONS.md](../DECISIONS.md).
- Guía de ejecución local: [../LOCAL_SETUP.md](../LOCAL_SETUP.md).
- Evidencias que debe recopilar el equipo: [../evidencias/README.md](../evidencias/README.md).
- Registro de inspección y pruebas parciales: [validacion-prototipo-y-pruebas-2026-10-14.md](../evidencias/validacion-prototipo-y-pruebas-2026-10-14.md).

## Cómo interpretar este material

Los artefactos se construyeron a partir de la guía oficial del curso compartida en el proyecto. Las decisiones que el enunciado deja abiertas —por ejemplo, tecnologías, límites físicos de servicios, tarifas y política concreta de cancelación— se marcan como propuestas o pendientes de validación. Scrum + Lean, los roles operativos y la cadencia semanal quedaron registrados y fueron incluidos en la ratificación del paquete del 14/10/2026; esto no acredita que las ceremonias se hayan realizado.

Los identificadores se mantienen consistentes: ACT, INT, RBN, RN, RF, RNF, EP, HU, CU, CP y ADR. Las historias tienen criterios de aceptación en formato **Dado que / Cuando / Entonces** y dependencias identificadas. La matriz CSV enlaza Jira, commits, PR, casos y la evidencia parcial ejecutada; la trazabilidad de implementación completa y releases se ampliará cuando el MVP exista.

## Validación y evidencia pendiente

- Ratificación del paquete por Juan Sebastián Morales y Julián Aguilera registrada el 14/10/2026. La revisión/aprobación docente no consta.
- Jira contiene 10 épicas, 19 historias y 38 subtareas en ocho sprints semanales del 19 de octubre al 14 de diciembre de 2026. MARKET-68 está asignada a Julián y MARKET-69 a Juan, pero ambas siguen sin sprint; el equipo debe ratificar Story Points, capacidad y asignación temporal.
- Los 20 artefactos están publicados/enlazados en el espacio MARKET, desde el [índice de Entrega 1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo). La página de procesos incluye enlaces a las tres fuentes BPMN versionadas; la ratificación del equipo está registrada; falta la validación visual de los BPMN.
- Abrir los BPMN en un modelador compatible, validar reglas y exportar capturas/diagramas legibles para Confluence si se requiere.
- La auditoría [PR #1–#9](../evidencias/revision-pull-requests-2026-10.md) registra diffs, checks y decisión: PR #3–#9 integradas (la actualización de Java de la cerrada #2 se retomó en la #9); PR #1 sigue abierta y no se integra porque no compila sus pruebas con Spring Boot 4. No hay aprobación independiente registrada para los PR auditados; la ratificación documental no sustituye una revisión de código. CI y SonarCloud pasaron en main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907): [run 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799) y [run 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828).
- La inspección técnica y los resultados parciales están en [`docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md`](../evidencias/validacion-prototipo-y-pruebas-2026-10-14.md). No hay sesiones reales registradas; quedan pendientes el feedback de participantes y los casos de negocio que dependen de funciones/endpoints aún no implementados.
- Capturar evidencia auténtica del uso de IA con fecha/hora y prompts/respuestas completos, conforme a la guía (p. 1); la plantilla está en `docs/evidencias/README.md`.

No se incluyen capturas ficticias ni se declara una validación que todavía no ha ocurrido.