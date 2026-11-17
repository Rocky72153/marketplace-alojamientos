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
| Uso de IA y soportes originales por recopilar | [Registro de uso](evidencias/uso-ia-2026-10.md), [índice de capturas](evidencias/registro-uso-IA.md). |
| Índice general de soportes | [Carpeta de evidencias](evidencias/README.md). |

Cada medición o captura acredita únicamente la versión, el ambiente y el alcance descritos. Las pruebas automáticas no sustituyen las firmas, la aceptación docente ni la validación con participantes.

## Herramientas del equipo

[Jira MARKET](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2) · [Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview) · [GitHub Actions](https://github.com/Rocky72153/marketplace-alojamientos/actions) · [SonarCloud](https://sonarcloud.io/dashboard?id=Rocky72153_marketplace-alojamientos).
