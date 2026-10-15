# Evidencias de la entrega

Esta carpeta/guía enumera evidencias que deben recopilarse desde las herramientas reales del equipo. No adjuntar capturas recreadas ni marcar como ejecutada una actividad que no ocurrió.

## Uso de IA (requisito expreso de la guía)

Plantilla para registrar cada intercambio y enlazar capturas originales: [`registro-uso-IA.md`](registro-uso-IA.md).

La guía del curso exige un documento con capturas del uso de IA donde sean visibles la fecha y hora, los prompts completos de entrada y salida. El equipo debe capturar la interfaz real correspondiente a cada intercambio que utilice como soporte y guardar las capturas originales con su fecha. En el documento índice, relacionar cada captura con la sección/artefacto afectado y explicar qué se aceptó, modificó o descartó.

**No incluir** credenciales, tokens, datos personales innecesarios o información privada de otros integrantes. No reconstruir una captura retrospectiva como si fuera original.

## Evidencias para GitHub

- Enlace al repositorio y ramas `main`, `develop` y rama de trabajo.
- PR reales de Entrega 1 con descripción y checks visibles. Las PR #5, #7 y #8 se revisaron técnicamente; GitHub no registra una aprobación independiente de otro integrante, que sigue pendiente como evidencia académica.
- Ejecución de Backend CI y Frontend CI en GitHub Actions con resultado final.
- Reglas de protección activas para `main` y `develop`, si el plan/permisos permiten, y checks seleccionados.
- Proyecto SonarCloud importado, variables/secreto configurados sin exponer sus valores, análisis real y Quality Gate visible.
- Release/tag real cuando se establezca una versión entregable, sin inventar un release.

## Evidencias académicas

- Captura real de épicas, 19 historias y sus criterios en Jira. Las estimaciones iniciales, responsables y distribución en ocho sprints semanales (19 oct.–14 dic. 2026) están registradas. Jira muestra MARKET-68 asignada a Julián David Aguilera Párraga y MARKET-69 a Juan Sebastián Morales Forero; ambas siguen sin sprint. El equipo debe ratificar puntos, capacidad y asignación temporal.
- Enlace/captura a páginas Confluence con visión, actores, BPMN, requerimientos, historias, casos de uso, modelos, arquitectura, API y manual de despliegue.
- Registro de inspección técnica: [validacion-prototipo-y-pruebas-2026-10-14.md](validacion-prototipo-y-pruebas-2026-10-14.md). La retroalimentación de huéspedes, anfitriones, administradores o cliente sigue pendiente.
- Validación del catálogo y de preguntas abiertas por el equipo; acuerdos/observaciones y responsable.
- Matriz de trazabilidad con claves/enlaces reales a Jira, commit, PR, prueba/evidencia y release.

## Registro

| ID | Evidencia | Enlace/ruta real | Fecha | Responsable | Estado |
|---|---|---|---|---|---|
| EV-01 | Uso de IA con prompts completos, entrada/salida y fecha/hora visibles | Pendiente de recopilar | Pendiente | Por asignar | Pendiente |
| EV-02 | Backlog en Jira | https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog | 2026-10-14 | Por asignar | 10 épicas, 19 historias, 38 subtareas y dos tareas transversales; MARKET-68 asignada a Julián y MARKET-69 a Juan, ambas sin sprint. Ratificar estimaciones/capacidad y conservar captura actualizada |
| EV-03 | Documentación publicada en Confluence | Pendiente de publicar/enlazar | Pendiente | Por asignar | Pendiente |
| EV-04 | PR de entrega 1 con revisión real | https://github.com/Rocky72153/marketplace-alojamientos/pull/5 | 2026-10-14 | Equipo | Consultar [estado de la PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5); PR #7 y PR #8 están integradas. La revisión técnica y los checks se verificaron; GitHub no registra aprobación independiente de otro integrante, por lo que la revisión académica entre pares sigue pendiente |
| EV-05 | CI verde en PR | https://github.com/Rocky72153/marketplace-alojamientos/actions | 2026-10-14 | Equipo | [PR #5 checks](https://github.com/Rocky72153/marketplace-alojamientos/pull/5/checks): run `38671271700` (HEAD `83da7ff`) pasó Backend, Frontend y Documentation CI. El run `38673169814` (PR #7, HEAD `165d2db`) pasó Backend, Frontend y Documentation CI. El run `38673786497` (PR #8, HEAD `b275941`) pasó CI; consultar los checks enlazados para el estado del HEAD actual |
| EV-06 | SonarCloud Quality Gate | https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517 | 2026-10-14 | Equipo | Quality Gate aprobado en PR #8; 0 incidencias nuevas, 0 hotspots nuevos y 0.0 % de cobertura de código nuevo ([run](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517)). Verificar el análisis del último commit de `main` |
| EV-07 | Protección de ramas | Pendiente de configurar/verificar | Pendiente | Por asignar | Pendiente |
| EV-08 | Inspección de prototipo y feedback de usuarios | [Registro técnico](validacion-prototipo-y-pruebas-2026-10-14.md) | 2026-10-14 | Codex (inspección técnica) | Interacción técnica ejecutada; no es sesión de usuario. Feedback real con huéspedes/anfitriones/cliente sigue pendiente |
