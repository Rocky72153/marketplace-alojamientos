# Evidencias de la entrega

Esta carpeta/guía enumera evidencias que deben recopilarse desde las herramientas reales del equipo. No adjuntar capturas recreadas ni marcar como ejecutada una actividad que no ocurrió.

## Uso de IA (requisito expreso de la guía)

Plantilla para registrar cada intercambio y enlazar capturas originales: [`registro-uso-IA.md`](registro-uso-IA.md).

La guía del curso exige un documento con capturas del uso de IA donde sean visibles la fecha y hora, los prompts completos de entrada y salida. El equipo debe capturar la interfaz real correspondiente a cada intercambio que utilice como soporte y guardar las capturas originales con su fecha. En el documento índice, relacionar cada captura con la sección/artefacto afectado y explicar qué se aceptó, modificó o descartó.

**No incluir** credenciales, tokens, datos personales innecesarios o información privada de otros integrantes. No reconstruir una captura retrospectiva como si fuera original.

## Evidencias para GitHub

- Enlace al repositorio y ramas `main`, `develop` y rama de trabajo.
- PR y checks auditados del repositorio: [revisión PR #1–#9](revision-pull-requests-2026-10.md). No consta aprobación independiente de otro integrante; esa revisión académica sigue pendiente.
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
| EV-03 | Documentación publicada en Confluence | Pendiente: la cuenta conectada no devuelve espacios ni páginas accesibles | 2026-10-14 | Equipo | Publicación/enlace pendiente; hace falta acceso a un espacio del curso o indicar uno accesible |
| EV-04 | Auditoría de PR de Entrega 1 | [Auditoría PR #1–#9](revision-pull-requests-2026-10.md) | 2026-10-14 | Equipo | PR #3–#9 integradas; el cambio propuesto en la PR #2 se retomó en la #9. La PR #1 permanece abierta y no integrada por fallas de compilación/pruebas. GitHub no registra aprobación independiente de otro integrante |
| EV-05 | CI verde en PR y main | [CI en main](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799) | 2026-10-14 | Equipo | Backend, Frontend y Documentation CI pasaron en PR #9 ([run 38678474687](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678474687)) y en main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907), [run 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799). Ver la auditoría enlazada para los checks anteriores |
| EV-06 | SonarCloud Quality Gate | [Quality Gate en main](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828) | 2026-10-14 | Equipo | Quality Gate aprobado en PR #8 ([run 38673786517](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517)), PR #9 ([run 38678474761](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678474761)) y main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907), [run 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828). La cobertura de código nuevo en PR #8 fue 0.0 % |
| EV-07 | Protección de ramas | Pendiente de configurar/verificar | Pendiente | Por asignar | Pendiente |
| EV-08 | Inspección de prototipo y feedback de usuarios | [Registro técnico](validacion-prototipo-y-pruebas-2026-10-14.md) | 2026-10-14 | Codex (inspección técnica) | Interacción técnica ejecutada; no es sesión de usuario. Feedback real con huéspedes/anfitriones/cliente sigue pendiente |
