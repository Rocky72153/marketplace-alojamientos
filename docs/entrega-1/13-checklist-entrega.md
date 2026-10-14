# 13. Lista de verificación de Entrega 1

Esta lista se cruza con los productos esperados y las exigencias de la guía del proyecto. Marcar **Hecho** solo cuando exista evidencia real revisada; el contenido versionado inicial no equivale automáticamente a aceptación del equipo/docente.

| Ítem de la guía | Evidencia en el repositorio | Estado para revisión |
|---|---|---|
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
| Requisitos del negocio explícitos derivados de objetivos del enunciado | `15-requisitos-negocio.md` | Catálogo RBN-001 a RBN-009 preparado; validar indicadores y trazabilidad con el equipo |
| Ciclo de vida del software | `16-ciclo-vida-software.md` | Modelo iterativo-incremental propuesto, fases y criterios de salida documentados; aprobación del equipo pendiente |
| Casos de prueba con ID, historia, precondiciones, pasos, datos y resultado esperado | `17-casos-de-prueba.md` | Casos CP-HU01 a CP-HU19 y transversales diseñados; ejecución/evidencia real pendiente |
| Subtareas por cada historia | Jira `MARKET-30` a `MARKET-67` | 38 subtareas creadas y asignadas, dos por historia (implementación al responsable de la historia y prueba al otro integrante); confirmar la distribución con el equipo |
| Jira: épicas/historias con prioridad, estimación, responsable, estado, sprint | Jira del equipo | Verificado: 10 épicas, 19 historias, 38 subtareas; story points iniciales aplicados, responsables asignados y 4 sprints con distribución propuesta. Fechas/capacidad aún por acordar |
| Confluence: visión, procesos, requisitos, diseño y manual de despliegue | Espacio Confluence del equipo | No verificable desde este repositorio; replicar/enlazar artefactos y evidencias |
| GitHub: repositorio, ramas, commits coherentes, PR y releases | Repositorio y ramas actuales | PR #4 está fusionado en `main` sin aprobación independiente registrada; PR #5 está abierto con documentación de brechas y CI verde. Protección de ramas y SonarCloud aún pendientes |
| GitHub Actions: compilar/construir, verificaciones y pruebas por PR | `.github/workflows/ci.yml` | Backend, frontend y documentación pasaron en el commit sincronizado; conservar el enlace de la ejecución como evidencia |
| SonarCloud integrado, Quality Gate visible | `.github/workflows/sonarcloud.yml` y `sonar-project.properties` | Pendiente de variables y secreto; job se omite a propósito hasta configuración |
| Pull Request de integración con revisión de otro integrante | PR real en GitHub | PR #4 está fusionado sin aprobación independiente registrada; PR #5 (cierre de brechas documentales) está abierto y requiere revisión humana antes del merge. No se puede reconstruir retroactivamente una aprobación auténtica del PR #4 |
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