# 13. Lista de verificación de Entrega 1

Esta lista se cruza con los productos esperados y las exigencias de la guía del proyecto. Marcar **Hecho** solo cuando exista evidencia real revisada; el contenido versionado inicial no equivale automáticamente a aceptación del equipo/docente.

| Ítem de la guía | Evidencia en el repositorio | Estado para revisión |
|---|---|---|
| Acta de constitución del proyecto | [Word](acta-constitucion-marketplace.docx) y [Markdown](acta-constitucion-marketplace.md) | Adaptada y agregada; fechas oficiales, aprobación y firmas pendientes |
| Visión, alcance y objetivos del negocio | `01-vision-alcance.md` | Revisado y ratificado como parte del paquete documental por Juan Sebastián Morales y Julián Aguilera el 14/10/2026; aprobación docente no acreditada |
| Actores/interesados con descripción, relación, necesidades, preocupaciones, responsabilidades y afectación | `02-actores-interesados.md` | Preparado; validar y completar nombres/responsables |
| Procesos relevantes con límites, participantes, entradas/salidas, reglas, eventos/resultados y relación a requerimientos | `03-procesos-negocio.md` + `bpmn/*.bpmn` | Borrador; validar visualmente BPMN y acordar excepciones |
| Catálogo de requerimientos identificados y priorización MVP | `04-catalogo-requerimientos.md` | Preparado; revisión de cobertura y aprobación pendiente |
| Historias con ID, épica, enunciado, AC iniciados con «El sistema debe» y escenario Dado/Cuando/Entonces, prioridad MoSCoW justificada, estimación, responsable, estado, sprint y dependencias | `05-backlog-historias.md` y Jira MARKET | 19 historias cargadas; paquete documental ratificado por el equipo el 14/10/2026; fechas del curso y capacidad real no constan |
| Casos de uso con flujos principales y alternos y diagrama | `06-casos-de-uso.md` | Ocho casos descritos y diagrama Mermaid agregado; walkthrough pendiente |
| Modelo de dominio | `07-modelo-dominio.md` | Preparado; revisar y versionar la versión aprobada en Confluence |
| Modelo lógico de datos y reglas de integridad | `08-modelo-datos.md` | Preparado; validar restricciones físicas y propiedad de datos |
| Contexto, solución orientada a servicios, responsabilidades, interacciones y despliegue | `09-arquitectura-servicios.md` y `14-secuencias.md` | Propuesta y diagrama de despliegue lógico agregados; proveedor/URL de hosting por definir y límites modulares por aprobar |
| Contratos de API OpenAPI | `10-openapi.yaml` | Borrador; validar sintaxis, seguridad, códigos y relación con servicios |
| Prototipo y plan de validación | `frontend/` y `11-prototipo-validacion.md` | Inspección técnica parcial registrada; feedback de participantes reales pendiente |
| Cronograma, WBS/EDT, riesgos y responsabilidades | `12-planificacion-riesgos.md` y `19-metodologia-scrum-lean.md` | Scrum + Lean, roles operativos y ocho sprints semanales documentados; paquete ratificado el 14/10/2026; capacidad real no consta |
| Manual de ejecución/despliegue | `docs/LOCAL_SETUP.md` | Guía local incluida; no equivale a despliegue de producción |
| Metodología ágil y mejora de flujo | `19-metodologia-scrum-lean.md`, Jira MARKET | Scrum + Lean y roles operativos documentados; paquete ratificado el 14/10/2026; ceremonias reales y métricas de ejecución no acreditadas |
| Matriz de trazabilidad requisito ↔ Jira ↔ AC ↔ branch/commit ↔ PR ↔ pruebas/evidencia ↔ release | `docs/TRACEABILITY_TEMPLATE.csv` y `docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md` | Enlaces a Jira/PR/casos y resultados reales parciales de UI; el resto se mantiene pendiente/no implementado |
| Registro de decisiones y preguntas abiertas | `docs/DECISIONS.md` | Preguntas y propuestas registradas; decisiones requieren aprobación real |
| Requisitos del negocio explícitos derivados de objetivos del enunciado | `15-requisitos-negocio.md` | Catálogo RBN-001 a RBN-009 preparado; validar indicadores y trazabilidad con el equipo |
| Ciclo de vida del software | `16-ciclo-vida-software.md` | Modelo iterativo-incremental, fases y criterios documentados; paquete ratificado el 14/10/2026; aprobación docente no acreditada |
| Casos de prueba con ID, historia, precondiciones, pasos, datos y resultado esperado | `17-casos-de-prueba.md` y `docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md` | El repositorio documenta 44 casos, con prioridades derivadas de las historias; la guía oficial no fija un número ni exige ejecutarlos todos en Entrega 1. Hay evidencia parcial de UI y CI. Los tests automatizados de historias ya implementadas deben ejecutarse en CI; los casos de negocio que dependen de funciones/endpoints inexistentes quedan pendientes para Entrega 2 |
| Subtareas por cada historia | Jira `MARKET-30` a `MARKET-67` | 38 subtareas creadas y asignadas, dos por historia (implementación al responsable de la historia y prueba al otro integrante); confirmar la distribución con el equipo |
| Jira: épicas/historias con prioridad, estimación, responsable, estado, sprint | Jira del equipo | Verificado: 10 épicas, 19 historias, 38 subtareas y MARKET-68/69. Jira asigna MARKET-68 a Julián y MARKET-69 a Juan; ambas siguen sin sprint. Capacidad no validada |
| Confluence: visión, procesos, requisitos y diseño | Espacio MARKET del proyecto | Publicados: índice de Entrega 1 con 20 páginas enlazadas, incluida el acta; la página de procesos enlaza las tres fuentes BPMN. El equipo ratificó el paquete el 14/10/2026; la validación visual BPMN sigue pendiente. [Abrir índice](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo) |
| GitHub: repositorio, ramas, commits coherentes, PR y releases | Repositorio y ramas actuales | [Auditoría PR #1–#9](../evidencias/revision-pull-requests-2026-10.md): PR #3–#9 integradas; la #2 se retomó mediante #9; #1 sigue abierta y fallida. No hay aprobación independiente; protección de ramas y release pendientes |
| GitHub Actions: compilar/construir, verificaciones y pruebas por PR | `.github/workflows/ci.yml` | Backend, Frontend y Documentation CI pasaron en PR #9 ([run 38678474687](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678474687)) y en main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907), [run 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799) |
| SonarCloud integrado, Quality Gate visible | `.github/workflows/sonarcloud.yml` y `sonar-project.properties` | Quality Gate aprobado en PR #8 ([run 38673786517](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517)), PR #9 ([run 38678474761](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678474761)) y main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907), [run 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828); cobertura de código nuevo en PR #8: 0.0 % |
| Pull Request de integración con revisión de otro integrante | PR real en GitHub | Cambios integrados después de revisión técnica; no existe aprobación independiente registrada de otro integrante. La revisión entre pares de la entrega sigue pendiente; no reconstruir una aprobación del PR #4 |
| Capturas de uso de IA con fecha/hora, prompts completos de entrada y salida | `docs/evidencias/README.md` | Guía de evidencia preparada; capturas auténticas pendientes |

## Antes de entregar

- [x] Juan Sebastián Morales y Julián Aguilera ratificaron el paquete documental en su estado actual el 14/10/2026; la constancia está en [Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo). Las decisiones aún abiertas se mantienen identificadas.
- [ ] Nombres de actores, responsables, sprint y estimaciones corresponden a datos verdaderos.
- [ ] Las preguntas abiertas de requisitos tienen decisión, responsable y evidencia.
- [ ] Los BPMN son legibles en la herramienta acordada y corresponden con procesos, historias y reglas.
- [ ] Los modelos de dominio/datos/arquitectura, secuencias y OpenAPI son consistentes.
- [x] Los artefactos están publicados/enlazados en Confluence; revisar visualmente los diagramas en la herramienta acordada antes de la sustentación.
- [x] Las historias existen en Jira y la matriz apunta a claves reales.
- [ ] El PR de entrega tiene revisión real de otro integrante y sus verificaciones obligatorias pasan.
- [x] SonarCloud muestra análisis real y Quality Gate aprobado en PR #8 y en main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907); cobertura de código nuevo reportada en PR #8: 0.0 %.
- [ ] Se revisó la visibilidad del repo y no hay secretos ni datos personales indebidos.
- [ ] La matriz y el README incluyen enlaces reales a entregables y evidencia.
- [ ] Todos los integrantes pueden explicar y defender los artefactos durante la sustentación.
- [ ] Se guardó evidencia auténtica del uso de IA con fecha/hora, prompts completos y salida, como pide la guía.

**La guía oficial divide Entrega 1 (análisis de requerimientos y diseño) de Entrega 2 (producto funcional, pruebas e integración continua); no define una cantidad fija de casos de prueba para Entrega 1. La guía sí pide ejecutar en CI los tests automatizados asociados a historias ya implementadas.**

**No afirmar que la entrega está 100% finalizada hasta cerrar los ítems que dependen de validación humana y herramientas externas.**
