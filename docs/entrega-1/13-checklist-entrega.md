# 13. Lista de verificación de Entrega 1

Esta lista se cruza con los productos esperados y las exigencias de la guía del proyecto. Marcar **Hecho** solo cuando exista evidencia real revisada; el contenido versionado inicial no equivale automáticamente a aceptación del equipo/docente.

| Ítem de la guía | Evidencia en el repositorio | Estado para revisión |
|---|---|---|
| Acta de constitución del proyecto | [Word](acta-constitucion-marketplace.docx) y [Markdown](acta-constitucion-marketplace.md) | Adaptada y alineada con Entrega 1; [PR #7](https://github.com/Rocky72153/marketplace-alojamientos/pull/7) abierto para revisión; fechas, aprobación y firmas pendientes |
| Visión, alcance y objetivos del negocio | `01-vision-alcance.md` | Preparado; aprobación del equipo pendiente |
| Actores/interesados con descripción, relación, necesidades, preocupaciones, responsabilidades y afectación | `02-actores-interesados.md` | Preparado; validar y completar nombres/responsables |
| Procesos relevantes con límites, participantes, entradas/salidas, reglas, eventos/resultados y relación a requerimientos | `03-procesos-negocio.md` + `bpmn/*.bpmn` | Borrador; validar visualmente BPMN y acordar excepciones |
| Catálogo de requerimientos identificados y priorización MVP | `04-catalogo-requerimientos.md` | Preparado; revisión de cobertura y aprobación pendiente |
| Historias con ID, épica, enunciado, AC Dado/Cuando/Entonces, prioridad, estimación, responsable, estado, sprint y dependencias | `05-backlog-historias.md` | Cargado en Jira MARKET con claves reales; faltan estimaciones acordadas, responsables y sprint/iteración |
| Casos de uso con flujos principales y alternos | `06-casos-de-uso.md` | Preparado; walkthrough pendiente |
| Modelo de dominio | `07-modelo-dominio.md` | Preparado; revisar y versionar la versión aprobada en Confluence |
| Modelo lógico de datos y reglas de integridad | `08-modelo-datos.md` | Preparado; validar restricciones físicas y propiedad de datos |
| Contexto, solución orientada a servicios, responsabilidades e interacciones | `09-arquitectura-servicios.md` y `14-secuencias.md` | Propuesta preparada; equipo debe aprobar límites modulares/distribuidos |
| Contratos de API OpenAPI | `10-openapi.yaml` | Borrador; validar sintaxis, seguridad, códigos y relación con servicios |
| Prototipo y plan de validación | `frontend/` y `11-prototipo-validacion.md` | Prototipo inicial; feedback real pendiente |
| Cronograma, WBS/EDT, riesgos y responsabilidades | `12-planificacion-riesgos.md` | Cronograma relativo; sustituir por fechas oficiales y asignar responsables |
| Manual de ejecución/despliegue | `docs/LOCAL_SETUP.md` | Guía local incluida; no equivale a despliegue de producción |
| Matriz de trazabilidad requisito ↔ Jira ↔ AC ↔ branch/commit ↔ PR ↔ pruebas/evidencia ↔ release | `docs/TRACEABILITY_TEMPLATE.csv` | Claves Jira y PR #4 enlazados; evidencia de implementación, pruebas y release sigue pendiente |
| Registro de decisiones y preguntas abiertas | `docs/DECISIONS.md` | Preguntas y propuestas registradas; decisiones requieren aprobación real |
| Jira: épicas/historias con prioridad, estimación, responsable, estado, sprint | Jira MARKET y [PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5) | Jira tiene 10 épicas, 19 historias, 38 subtareas y 2 tareas transversales; las 69 están asignadas y permanecen en To Do. PR #5 documenta 8 sprints y 106 SP iniciales; el equipo debe ratificar capacidad y planificación |
| Confluence: visión, procesos, requisitos, diseño y manual de despliegue | Espacio Confluence del equipo | No verificable desde este repositorio; replicar/enlazar artefactos y evidencias |
| GitHub: repositorio, ramas, commits coherentes, PR y releases | [PR #4](https://github.com/Rocky72153/marketplace-alojamientos/pull/4), [PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5) y [PR #7](https://github.com/Rocky72153/marketplace-alojamientos/pull/7) | PR #4 está fusionado; PR #5 y PR #7 están abiertos, sin revisiones registradas. Las reglas administrativas y el release siguen por verificar |
| GitHub Actions: compilar/construir, verificaciones y pruebas por PR | `.github/workflows/ci.yml`, [PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5) y [PR #7](https://github.com/Rocky72153/marketplace-alojamientos/pull/7) | CI pasó en el HEAD de PR #5 (`83da7ff`); CI del HEAD actual de PR #7 (`b436ae7`) está en ejecución. SonarCloud se omite mientras siga sin configuración |
| SonarCloud integrado, Quality Gate visible | `.github/workflows/sonarcloud.yml` y `sonar-project.properties` | Pendiente de variables y secreto; job se omite a propósito hasta configuración |
| Pull Request de integración con revisión de otro integrante | [PR #4](https://github.com/Rocky72153/marketplace-alojamientos/pull/4), [PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5) y [PR #7](https://github.com/Rocky72153/marketplace-alojamientos/pull/7) | PR #4 se fusionó; PR #5 y PR #7 siguen abiertos sin revisión independiente. No fusionar hasta que otro integrante revise |
| Capturas de uso de IA con fecha/hora, prompts completos de entrada y salida | `docs/evidencias/README.md` | Guía de evidencia preparada; capturas auténticas pendientes |

## Antes de entregar

- [ ] El equipo revisó el contenido y reemplazó propuestas no aceptadas por acuerdos reales.
- [ ] Nombres de actores, responsables, sprint y estimaciones corresponden a datos verdaderos.
- [ ] Las preguntas abiertas de requisitos tienen decisión, responsable y evidencia.
- [ ] Los BPMN son legibles en la herramienta acordada y corresponden con procesos, historias y reglas.
- [ ] Los modelos de dominio/datos/arquitectura, secuencias y OpenAPI son consistentes.
- [ ] Los artefactos están publicados/enlazados en Confluence como solicita la guía.
- [x] Las historias existen en Jira y la matriz apunta a claves reales.
- [ ] El PR de entrega tiene revisión real de otro integrante y sus verificaciones obligatorias pasan.
- [ ] SonarCloud muestra un análisis real y estado de Quality Gate.
- [ ] Se revisó la visibilidad del repo y no hay secretos ni datos personales indebidos.
- [ ] La matriz y el README incluyen enlaces reales a entregables y evidencia.
- [ ] Todos los integrantes pueden explicar y defender los artefactos durante la sustentación.
- [ ] Se guardó evidencia auténtica del uso de IA con fecha/hora, prompts completos y salida, como pide la guía.

**No afirmar que la entrega está 100% finalizada hasta cerrar los ítems que dependen de validación humana y herramientas externas.**
