# 13. Lista de verificación de Entrega 1

Esta lista se cruza con los productos esperados y las exigencias de la guía del proyecto. Marcar **Hecho** solo cuando exista evidencia real revisada; el contenido versionado inicial no equivale automáticamente a aceptación del equipo/docente.

| Ítem de la guía | Evidencia en el repositorio | Estado para revisión |
|---|---|---|
| Visión, alcance y objetivos del negocio | `01-vision-alcance.md` | Preparado; aprobación del equipo pendiente |
| Actores/interesados con descripción, relación, necesidades, preocupaciones, responsabilidades y afectación | `02-actores-interesados.md` | Preparado; validar y completar nombres/responsables |
| Procesos relevantes con límites, participantes, entradas/salidas, reglas, eventos/resultados y relación a requerimientos | `03-procesos-negocio.md` + `bpmn/*.bpmn` | Borrador; abrir/validar visualmente BPMN y acordar excepciones |
| Catálogo de requerimientos identificados y priorización MVP | `04-catalogo-requerimientos.md` | Preparado; revisión de cobertura y aprobación pendiente |
| Historias con ID, épica, enunciado, AC Dado/Cuando/Entonces, prioridad, estimación, responsable, estado, sprint y dependencias | `05-backlog-historias.md` | Preparado; cargar/actualizar en Jira con claves y responsables reales |
| Casos de uso con flujos principales y alternos | `06-casos-de-uso.md` | Preparado; walkthrough pendiente |
| Modelo de dominio | `07-modelo-dominio.md` | Preparado; revisar y versionar la versión aprobada en Confluence |
| Modelo lógico de datos y reglas de integridad | `08-modelo-datos.md` | Preparado; validar restricciones físicas y ownership de datos |
| Contexto, solución orientada a servicios, responsabilidades, interacciones y relación requisitos | `09-arquitectura-servicios.md` | Propuesta preparada; equipo debe aprobar si modular o distribuida |
| Contratos de API OpenAPI | `10-openapi.yaml` | Borrador; validar sintaxis, seguridad, códigos y relación con servicios |
| Prototipo | `frontend/` y `11-prototipo-validacion.md` | Prototipo inicial preparado; feedback real pendiente |
| Cronograma, WBS/EDT, riesgos y responsabilidades | `12-planificacion-riesgos.md` | Cronograma relativo; sustituir por fechas oficiales y asignar responsables |
| Matriz de trazabilidad requisito ↔ Jira ↔ AC ↔ branch/commit ↔ PR ↔ pruebas/evidencia ↔ release | `docs/TRACEABILITY_TEMPLATE.csv` | Plantilla disponible; rellenar claves/enlaces reales mientras se implemente |
| Jira: épicas/historias con prioridad, estimación, responsable, estado, sprint | Jira del equipo | No verificable desde este repositorio; debe ejecutarse en Jira |
| Confluence: visión, procesos, requisitos, diseño y manual despliegue | Espacio Confluence del equipo | No verificable desde este repositorio; replicar/enlazar artefactos y evidencias |
| GitHub: repositorio, ramas, commits coherentes, PR y releases | Repositorio y ramas actuales | CI configurado; reglas administrativas y PR de entrega deben validarse |
| GitHub Actions: compilar/construir, verificaciones y pruebas por PR | `.github/workflows/ci.yml` | Ejecuciones observadas exitosas; confirmar checks de la rama del PR de entrega |
| SonarCloud integrado, Quality Gate visible | `.github/workflows/sonarcloud.yml` y `sonar-project.properties` | Pendiente de variables y secreto; no marcar como aprobado todavía |
| Pull Request de integración con revisión de otro integrante | PR real en GitHub | Pendiente; la conexión no puede aportar aprobación auténtica de un compañero |
| Capturas de uso de IA con fecha/hora, prompts completos de entrada y salida | Evidencia externa del equipo | Pendiente; conservar evidencias auténticas de la interfaz, no reconstruirlas retrospectivamente |

## Antes de entregar

- [ ] El equipo revisó el contenido y reemplazó propuestas no aceptadas por acuerdos reales.
- [ ] Los nombres de actores, responsables, sprint y estimaciones corresponden a los datos verdaderos.
- [ ] Las preguntas abiertas de requisitos tienen decisión, responsable y evidencia.
- [ ] BPMN está visualmente legible y corresponde con los procesos, historias y reglas.
- [ ] La versión de dominio/datos/arquitectura y OpenAPI es consistente entre sí.
- [ ] Los artefactos están publicados/enlazados en Confluence como solicita la guía.
- [ ] Las historias existen en Jira y la matriz apunta a claves reales.
- [ ] El PR de entrega tiene revisión real de otro integrante y sus verificaciones obligatorias pasan.
- [ ] SonarCloud muestra un análisis real y estado de Quality Gate, o la limitación se resolvió antes de declarar terminado.
- [ ] Se revisó la visibilidad del repo y no hay secretos ni datos personales indebidos.
- [ ] Se actualizaron la matriz y el README del repositorio con enlaces reales.
- [ ] Se preparó sustentación: todos los integrantes pueden explicar y defender cada artefacto.
- [ ] Se guardó evidencia auténtica del uso de IA con fecha/hora, prompts completos y salida, como pide la guía.

**No afirmar que la entrega está 100% finalizada hasta cerrar los ítems que dependen de validación humana y herramientas externas.**
