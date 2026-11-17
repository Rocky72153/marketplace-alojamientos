# Documentación del Marketplace de Alojamientos

Este índice reúne la implementación vigente, sus evidencias y los antecedentes de diseño. Para ejecutar la aplicación, empieza por la [guía local](LOCAL_SETUP.md); para preparar la presentación, por el [informe de Entrega 2](INFORME-FINAL-ENTREGA-2.md). La portada del proyecto está en [README](../README.md).

## Documentación vigente

| Tema | Documento |
|---|---|
| Estado real y preparación de la entrega | [Informe de Entrega 2](INFORME-FINAL-ENTREGA-2.md), [acta de cierre y guion de sustentación](ACTA-CIERRE.md). |
| Arranque y parada del entorno local | [LOCAL_SETUP.md](LOCAL_SETUP.md). |
| Demostración en AWS | [Despliegue EC2](DESPLIEGUE-EC2.md), [plantillas de despliegue](../deploy/ec2/), [migración PostgreSQL](../backend/deploy/postgresql/README.md). |
| Rutas, esquemas, permisos y operación de la API | [Contrato OpenAPI vigente](openapi.yaml), [API implementada](API-IMPLEMENTADA.md). |
| Pago, eventos firmados y reembolsos de prueba | [Integración de Stripe](STRIPE-PRUEBAS.md). |
| Cotización, reserva y cancelación | [Reglas EP-05 y EP-06](EP05-EP06-reservas-cancelaciones.md), [evidencia de implementación](EP05-EP06-EVIDENCE.md). |
| Notificaciones y reseñas | [EP-07 y EP-08](EP07-EP08-EVIDENCE.md). |
| Administración, indicadores y reportes | [EP-09](EP09-administracion-reportes.md). |
| Correlación, auditoría y registros | [EP-10: observabilidad](EP10-observabilidad.md). |
| Experiencia de la interfaz | [Mejoras y capturas](FRONTEND-MEJORAS.md), [verificación de navegación y Stripe](FRONTEND-VERIFICACION.md). |
| Rendimiento | [Procedimiento reproducible](PRUEBA-RENDIMIENTO.md). |
| Requerimientos, tareas y versión entregada | [Matriz de trazabilidad](TRACEABILITY_TEMPLATE.csv), [decisiones](DECISIONS.md), [release v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0). |
| Colaboración y calidad | [Guía para contribuir](../CONTRIBUTING.md), [GitHub y SonarCloud](GITHUB_SETUP.md), [workflows](../.github/workflows/). |
| Historial de cambios de identificadores | [Nota sobre el historial](HISTORIAL-REESCRITO.md), [mapa de commits](historial-commit-map.csv). |

### Diagramas de la implementación

| Vista | Documento y archivos |
|---|---|
| Clases del backend | [Guía de lectura](DIAGRAMA-CLASES.md), [XML editable completo](diagramas/diagrama-clases-marketplace.xml). |
| Contexto C4, contenedores, componentes y despliegue AWS | [Arquitectura de servicios](entrega-1/09-arquitectura-servicios.md). |
| Modelo físico PostgreSQL | [Modelo de datos](entrega-1/08-modelo-datos.md), [imagen del modelo físico](entrega-1/diagramas/08-modelo-fisico-er.png). |
| Pago externo y otras interacciones | [Secuencias](entrega-1/14-secuencias.md), [secuencia Stripe](entrega-1/diagramas/14-sd04-pago-stripe.png). |
| Paquetes, actividad de reserva, estados y pipeline CI/CD | [Diseño detallado](entrega-1/20-diseno-detallado.md). |
| Procesos y BPMN | [Procesos de negocio](entrega-1/03-procesos-negocio.md), [fuentes BPMN](entrega-1/bpmn/). |
| Inventario de imágenes y fuentes | [Diagramas renderizados](entrega-1/diagramas/README.md), [fuentes editables](entrega-1/diagramas/fuentes/). |

Las actualizaciones de diseño del 16 de noviembre de 2026 incorporan el modelo físico, C4, componentes, despliegue, moderación y reportes BPMN, secuencia Stripe, paquetes, actividad, estados y pipeline. Los documentos indican las fuentes y distinguen la implementación de las propuestas originales.

## Entrega 1 histórica

El [índice de Entrega 1](entrega-1/README.md) conserva visión, actores, requisitos, historias, casos de uso, planificación, modelos y criterios de revisión. También contiene actualizaciones de diseño posteriores; la tabla anterior señala cuáles describen la implementación vigente.

- [Acta de constitución](entrega-1/acta-constitucion-marketplace.md) y [Word](entrega-1/acta-constitucion-marketplace.docx).
- [Visión y alcance](entrega-1/01-vision-alcance.md), [actores](entrega-1/02-actores-interesados.md) y [catálogo de requisitos](entrega-1/04-catalogo-requerimientos.md).
- [Backlog e historias](entrega-1/05-backlog-historias.md) y [casos de uso](entrega-1/06-casos-de-uso.md).
- [Modelo conceptual original](entrega-1/07-modelo-dominio.md) y [OpenAPI inicial](entrega-1/10-openapi.yaml). El contrato inicial no equivale por sí solo al backend actual.
- [Prototipo y validación](entrega-1/11-prototipo-validacion.md), [casos de prueba diseñados](entrega-1/17-casos-de-prueba.md) y [checklist de la entrega](entrega-1/13-checklist-entrega.md).
- [Planificación y riesgos](entrega-1/12-planificacion-riesgos.md), [ciclo de vida](entrega-1/16-ciclo-vida-software.md) y [metodología Scrum](entrega-1/19-metodologia-scrum.md).

Las anotaciones históricas sobre tareas pendientes corresponden a su fecha de corte. Para conocer el estado posterior, consulta el informe actual y la evidencia asociada; no conviertas una aprobación pendiente en una aprobación realizada.

## Evidencias

| Evidencia | Registro |
|---|---|
| Ejecución desde clon limpio y pruebas locales | [Validación de entrega del 12 de noviembre](EVIDENCIA-ENTREGA-FINAL.md). |
| Rendimiento local con 10.000 alojamientos | [Resultado de MARKET-69](evidencias/medicion-market-69-final.json). |
| Rendimiento en EC2 | [Medición del 14 de noviembre](evidencias/medicion-aws-t3-small-2026-11-14.json). |
| Pago y reembolso con Stripe sandbox | [Resultado estructurado del 14 de noviembre](evidencias/stripe-sandbox-2026-11-14.json). |
| Navegación, sesión y retorno de Stripe | [Verificación del 15 de noviembre](evidencias/frontend-stripe-2026-11-15.json). |
| Capturas del frontend | [Portada, móvil, cotización y administración](FRONTEND-MEJORAS.md#capturas). |
| Trabajo Scrum y retrospectiva acumulada | [Registro de eventos](evidencias/registro-eventos-scrum.md). |
| Uso de IA y soportes originales por recopilar | [Revisión actual: registro textual sin captura](evidencias/uso-ia-2026-11-17.md), [registro histórico](evidencias/uso-ia-2026-10.md), [índice de registros y capturas pendientes](evidencias/registro-uso-IA.md). |
| Índice general de soportes | [Carpeta de evidencias](evidencias/README.md). |

Cada medición o captura acredita únicamente la versión, el ambiente y el alcance descritos. Las pruebas automáticas no sustituyen las firmas, la aceptación docente ni la validación con participantes.

## Herramientas del equipo

[Jira MARKET](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2) · [Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview) · [GitHub Actions](https://github.com/Rocky72153/marketplace-alojamientos/actions) · [SonarCloud](https://sonarcloud.io/dashboard?id=Rocky72153_marketplace-alojamientos).

## Mapa por fases de Confluence

Este índice ordena los documentos del repositorio igual que el [espacio MARKET en Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview): seis fases y, dentro de cada una, las mismas páginas con el mismo número. Cada fila enlaza la página de Confluence y el archivo versionado en GitHub que la respalda. Los archivos no se movieron de carpeta para no romper los enlaces existentes en Jira, Confluence, la matriz de trazabilidad y la validación del CI.

## Fase 1. Inicio

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 1.1 | Documento del proyecto (acta de constitución) | [1.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1015848) | [acta-constitucion-marketplace.md](entrega-1/acta-constitucion-marketplace.md) y [versión Word](entrega-1/acta-constitucion-marketplace.docx) |
| 1.2 | Metodología Scrum y roles | [1.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/655380) | [19-metodologia-scrum.md](entrega-1/19-metodologia-scrum.md) |
| 1.3 | Ciclo de vida del software | [1.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/688361) | [16-ciclo-vida-software.md](entrega-1/16-ciclo-vida-software.md) |
| 1.4 | Herramientas del proyecto (Jira, Confluence, GitHub) | [1.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9044023) | [CONTRIBUTING.md](../CONTRIBUTING.md), [GITHUB_SETUP.md](GITHUB_SETUP.md) y [LOCAL_SETUP.md](LOCAL_SETUP.md) |

## Fase 2. Análisis

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 2.1 | Visión, objetivos y alcance | [2.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1015809) | [01-vision-alcance.md](entrega-1/01-vision-alcance.md) |
| 2.2 | Requisitos del negocio | [2.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/655362) | [15-requisitos-negocio.md](entrega-1/15-requisitos-negocio.md) |
| 2.3 | Actores e interesados | [2.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/688324) | [02-actores-interesados.md](entrega-1/02-actores-interesados.md) |
| 2.4 | Procesos de negocio (BPMN) | [2.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/884744) | [03-procesos-negocio.md](entrega-1/03-procesos-negocio.md) y archivos `.bpmn` en [bpmn](entrega-1/bpmn/) |
| 2.5 | Requerimientos clasificados (RF, RNF, RN) | [2.5](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1048577) | [04-catalogo-requerimientos.md](entrega-1/04-catalogo-requerimientos.md) |
| 2.6 | Priorización MoSCoW | [2.6](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9142308) | [05-backlog-historias.md](entrega-1/05-backlog-historias.md) |
| 2.7 | Épicas | [2.7](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9142329) | [05-backlog-historias.md](entrega-1/05-backlog-historias.md) |
| 2.8 | Historias de usuario y criterios de validación | [2.8](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/917511) | [05-backlog-historias.md](entrega-1/05-backlog-historias.md) |
| 2.9 | Casos de uso | [2.9](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/983050) | [06-casos-de-uso.md](entrega-1/06-casos-de-uso.md) |
| 2.10 | Casos de prueba | [2.10](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1081364) | [17-casos-de-prueba.md](entrega-1/17-casos-de-prueba.md) |

## Fase 3. Diseño

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 3.1 | Modelo de dominio | [3.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/786434) | [07-modelo-dominio.md](entrega-1/07-modelo-dominio.md) |
| 3.2 | Modelo de datos | [3.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753694) | [08-modelo-datos.md](entrega-1/08-modelo-datos.md), secciones 8.1 y 8.2 |
| 3.3 | Arquitectura de servicios | [3.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1081345) | [09-arquitectura-servicios.md](entrega-1/09-arquitectura-servicios.md) |
| 3.4 | Diagramas de secuencia | [3.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/589826) | [14-secuencias.md](entrega-1/14-secuencias.md) |
| 3.5 | Contrato de API (OpenAPI) | [3.5](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/688342) y [API implementada](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/11829249) | [10-openapi.yaml](entrega-1/10-openapi.yaml) (contrato de Entrega 1) y [API-IMPLEMENTADA.md](API-IMPLEMENTADA.md) |
| 3.6 | Prototipo y validación | [3.6](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1015828) | [11-prototipo-validacion.md](entrega-1/11-prototipo-validacion.md) y [validacion-prototipo-y-pruebas-2026-10-14.md](evidencias/validacion-prototipo-y-pruebas-2026-10-14.md) |
| 3.7 | Matriz de trazabilidad | [3.7](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9175059) | [TRACEABILITY_TEMPLATE.csv](TRACEABILITY_TEMPLATE.csv) |
| 3.8 | Diagrama de clases del backend (XML completo) | [3.8](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/12451842) | [DIAGRAMA-CLASES.md](DIAGRAMA-CLASES.md) y [diagrama-clases-marketplace.xml](diagramas/diagrama-clases-marketplace.xml) |
| 3.9 | Diseño detallado: paquetes, actividad, estados y pipeline CI/CD | [3.9](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/13959169) | [20-diseno-detallado.md](entrega-1/20-diseno-detallado.md) |
| 3.10 | Normalización hasta tercera forma normal | [3.10](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/14450690) | [08-modelo-datos.md](entrega-1/08-modelo-datos.md), sección 8.3 |

Las imágenes de todos los diagramas están en [entrega-1/diagramas](entrega-1/diagramas/) y sus fuentes editables en [entrega-1/diagramas/fuentes](entrega-1/diagramas/fuentes/).

## Fase 4. Planeación Scrum

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 4.1 | Product Backlog y estimación | [4.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9175082) | [05-backlog-historias.md](entrega-1/05-backlog-historias.md) |
| 4.2 | Tareas (subtareas por historia) | [4.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/688380) | [18-plan-subtareas-jira.md](entrega-1/18-plan-subtareas-jira.md) |
| 4.3 | Sprints | [4.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9011202) | [12-planificacion-riesgos.md](entrega-1/12-planificacion-riesgos.md) |
| 4.4 | Planificación, WBS/EDT y riesgos | [4.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/1114113) | [12-planificacion-riesgos.md](entrega-1/12-planificacion-riesgos.md) y [DECISIONS.md](DECISIONS.md) |

## Fase 5. Construcción

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 5.1 | Estado de implementación por historia | [5.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/8945666) | [EP05-EP06-reservas-cancelaciones.md](EP05-EP06-reservas-cancelaciones.md), [EP05-EP06-EVIDENCE.md](EP05-EP06-EVIDENCE.md), [EP07-EP08-EVIDENCE.md](EP07-EP08-EVIDENCE.md), [EP09-administracion-reportes.md](EP09-administracion-reportes.md) y [EP10-observabilidad.md](EP10-observabilidad.md) |
| 5.2 | Integración continua y calidad (GitHub) | [5.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9273345) | [EVIDENCIA-ENTREGA-FINAL.md](EVIDENCIA-ENTREGA-FINAL.md), [PRUEBA-RENDIMIENTO.md](PRUEBA-RENDIMIENTO.md) y los flujos de [.github/workflows](../.github/workflows/) |
| 5.3 | Registro de eventos Scrum | [5.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9175104) | [registro-eventos-scrum.md](evidencias/registro-eventos-scrum.md) y [revision-pull-requests-2026-10.md](evidencias/revision-pull-requests-2026-10.md) |
| 5.4 | Interfaz publicada y evidencias | [5.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/12255234) | [FRONTEND-MEJORAS.md](FRONTEND-MEJORAS.md), [FRONTEND-VERIFICACION.md](FRONTEND-VERIFICACION.md), [STRIPE-PRUEBAS.md](STRIPE-PRUEBAS.md) y el índice de [evidencias](evidencias/README.md) |

## Fase 6. Cierre

| # | Página | Confluence | Archivo en GitHub |
|---|---|---|---|
| 6.1 | Checklist de entrega | [6.1](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/819409) | [13-checklist-entrega.md](entrega-1/13-checklist-entrega.md) |
| 6.2 | Estado de la Entrega 1 | [6.2](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665) | [entrega-1/README.md](entrega-1/README.md) |
| 6.3 | Evidencia del uso de IA | [6.3](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9306113) | [Registro textual actual, sin captura de la revisión](evidencias/uso-ia-2026-11-17.md), [prompt elaborado](evidencias/prompt-revision-diagramas-2026-11-17.md) y [registro histórico](evidencias/registro-uso-IA.md) |
| 6.4 | Release y demostración final | [6.4](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9273364) | [Release v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0) y [DESPLIEGUE-EC2.md](DESPLIEGUE-EC2.md) |
| 6.5 | Informe final y acta de cierre | [6.5](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/9011225) | [INFORME-FINAL-ENTREGA-2.md](INFORME-FINAL-ENTREGA-2.md) y [ACTA-CIERRE.md](ACTA-CIERRE.md) |

## Otros documentos del repositorio

| Archivo | Contenido |
|---|---|
| [HISTORIAL-REESCRITO.md](HISTORIAL-REESCRITO.md) y [historial-commit-map.csv](historial-commit-map.csv) | Equivalencias de commits tras la reescritura del historial del 16/11/2026 |
| [evidencias](evidencias/README.md) | Capturas, mediciones y registros que respaldan las páginas de Confluence |
