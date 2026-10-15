# Evidencias de la entrega

Esta carpeta/guía enumera evidencias que deben recopilarse desde las herramientas reales del equipo. No adjuntar capturas recreadas ni marcar como ejecutada una actividad que no ocurrió.

## Uso de IA (requisito expreso de la guía)

Plantilla para registrar cada intercambio y enlazar capturas originales: [`registro-uso-IA.md`](registro-uso-IA.md).

La guía del curso exige un documento con capturas del uso de IA donde sean visibles la fecha y hora, los prompts completos de entrada y salida. El equipo debe capturar la interfaz real correspondiente a cada intercambio que utilice como soporte y guardar las capturas originales con su fecha. En el documento índice, relacionar cada captura con la sección/artefacto afectado y explicar qué se aceptó, modificó o descartó.

**No incluir** credenciales, tokens, datos personales innecesarios o información privada de otros integrantes. No reconstruir una captura retrospectiva como si fuera original.

## Evidencias para GitHub

- Enlace al repositorio y ramas `main`, `develop` y rama de trabajo.
- Pull Request real de la documentación de entrega 1, descripción completa, revisión de otro integrante y checks CI visibles.
- Ejecución de Backend CI y Frontend CI en GitHub Actions con resultado final.
- Reglas de protección activas para `main` y `develop`, si el plan/permisos permiten, y checks seleccionados.
- Proyecto SonarCloud importado, variables/secreto configurados sin exponer sus valores, análisis real y Quality Gate visible.
- Release/tag real cuando se establezca una versión entregable, sin inventar un release.

## Evidencias académicas

- Captura real de épicas, 19 historias y sus criterios en Jira. Las estimaciones iniciales, los responsables y la distribución propuesta en cuatro sprints ya están aplicados; el equipo debe ratificar los puntos y acordar fechas/capacidad.
- Enlace/captura a páginas Confluence con visión, actores, BPMN, requerimientos, historias, casos de uso, modelos, arquitectura, API y manual de despliegue.
- Registro de feedback real del prototipo: fecha, rol participante, tarea, hallazgo, decisión y cambio asociado.
- Validación del catálogo y de preguntas abiertas por el equipo; acuerdos/observaciones y responsable.
- Matriz de trazabilidad con claves/enlaces reales a Jira, commit, PR, prueba/evidencia y release.

## Registro

| ID | Evidencia | Enlace/ruta real | Fecha | Responsable | Estado |
|---|---|---|---|---|---|
| EV-01 | Uso de IA con prompts completos, entrada/salida y fecha/hora visibles | Pendiente de recopilar | Pendiente | Por asignar | Pendiente |
| EV-02 | Backlog en Jira | https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog | 2026-10-14 | Por asignar | 10 épicas, 19 historias y 38 subtareas; puntos/responsables/sprints aplicados; revisión de estimaciones y fechas/capacidad pendiente; captura pendiente |
| EV-03 | Documentación publicada en Confluence | Pendiente de publicar/enlazar | Pendiente | Por asignar | Pendiente |
| EV-04 | PR de entrega 1 con revisión real | https://github.com/Rocky72153/marketplace-alojamientos/pull/5 | 2026-10-14 | Julián David Aguilera Párraga | PR #5 abierto y pendiente de revisión independiente; PR #4 anterior está fusionado sin aprobación independiente registrada. No simular ni reconstruir una aprobación |
| EV-05 | CI verde en PR | https://github.com/Rocky72153/marketplace-alojamientos/actions | 2026-10-14 | Por asignar | La ejecución `38600164927` pasó Backend CI, Frontend CI y Documentation CI para el HEAD `82371a3`; las ejecuciones posteriores deben comprobarse contra el HEAD final del PR. SonarCloud (`38600164944`) terminó omitido y no constituye un Quality Gate aprobado |
| EV-06 | SonarCloud Quality Gate | Pendiente de importar/configurar | Pendiente | Por asignar | Pendiente |
| EV-07 | Protección de ramas | Pendiente de configurar/verificar | Pendiente | Por asignar | Pendiente |
| EV-08 | Revisión real de prototipo | Pendiente de realizar | Pendiente | Por asignar | Pendiente |
