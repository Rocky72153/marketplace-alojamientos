# 12. Planificación, WBS/EDT y riesgos

**Cautela:** no se proporcionó fecha oficial de la entrega 1, fecha de finalización, tamaño/horario del equipo ni estimaciones acordadas. Por eso se propone un plan relativo por semanas desde la aprobación de la planeación; reemplazar `Semana N` por las fechas reales del curso antes de entregar.

## 12.1 Enfoque ágil propuesto

Propuesta inicial: flujo Scrum ligero con iteraciones cortas, revisión de incremento y retrospectiva. El equipo debe confirmar la metodología vista en el curso y registrar iteraciones/reviews/retrospectivas en Jira. El enunciado exige seleccionar y aplicar una metodología ágil, pero no prescribe una específica.

- Reunión de planeación: confirmar objetivo del sprint, capacidad, historias Ready y tareas técnicas.
- Seguimiento breve: actualizar bloqueos y estado en Jira.
- Revisión: demostrar historias contra criterios de aceptación y registrar feedback.
- Retrospectiva: documentar acciones de mejora con responsable y fecha.
- Refinamiento: resolver preguntas abiertas, estimar historias y mantener prioridades.


## 12.1.1 Roles Scrum y responsabilidades

La guía del curso exige elegir y aplicar una metodología ágil, pero no asigna nombres concretos a los integrantes. Por ello, los roles se definen funcionalmente; los nombres reales deben ser confirmados por el equipo antes de asignarlos en Jira.

| Rol | Responsabilidades en este proyecto | Persona |
|---|---|---|
| Product Owner (PO) | Maximizar el valor del Marketplace; ordenar el backlog; aclarar objetivos, criterios de aceptación y decisiones de negocio; validar el incremento en la Review. | Por confirmar por el equipo; no se presume que el cliente esté disponible. |
| Scrum Master / facilitador | Facilitar ceremonias, detectar impedimentos, ayudar a que Scrum se aplique y fomentar mejora continua sin actuar como jefe del equipo. | Por confirmar por el equipo. |
| Developers / equipo de desarrollo | Diseñar, implementar, probar, revisar código, actualizar documentación y trazabilidad; estimar el trabajo y entregar un incremento “Done”. | Integrantes reales del grupo, por asignar a cada historia/subtarea en Jira. |
| Interesados / representante del negocio | Aportar contexto, revisar prototipos e incrementos y dar feedback verificable sobre necesidades. | Representante de Inversiones LR simulado para el curso; validar quién da feedback académico. |

**Nota:** una persona puede asumir más de una responsabilidad en un equipo pequeño si el curso lo permite, pero el acuerdo debe quedar explícito. No se asignan nombres ni se registra feedback del cliente sin confirmación real.

## 12.1.2 Duración y estructura de los sprints

**Propuesta operativa:** sprints de dos semanas (10 días hábiles), sujeto a aprobación del equipo y a las fechas oficiales del curso. La guía permite trabajo iterativo pero no fija duración ni fechas. En Jira se registran los sprints con fechas reales acordadas; no se inventan fechas de inicio/fin.

Cada sprint tendrá: objetivo de sprint, historias priorizadas y Ready, estimación de story points consensuada, responsable real por historia/subtarea, criterios de aceptación, pruebas previstas y capacidad acordada. Los story points que figuren en el backlog son estimaciones iniciales y deben revisarse en refinamiento/planning.

## 12.1.3 Ceremonias, cadencia y salidas

| Ceremonia | Cadencia / duración propuesta | Participantes | Agenda y salida obligatoria |
|---|---|---|---|
| Sprint Planning | Al inicio de cada sprint; 60–90 min para sprint de 2 semanas. | PO, facilitador, Developers. | Objetivo del sprint, historias seleccionadas según capacidad, dependencias, subtareas, estimaciones y Definition of Done. |
| Daily Scrum | Cada día hábil; 15 min. | Developers; facilitador ayuda a eliminar impedimentos. | Ajuste del plan diario y bloqueos con responsable/acción en Jira. Si el equipo no puede reunirse a diario, registrar una alternativa acordada y no afirmar que se hizo Daily. |
| Backlog Refinement | Una vez por semana; 30–60 min. | PO y Developers; facilitador si hace falta. | Historias entendibles, criterios verificables, dependencias, estimaciones revisadas y elementos Ready para la próxima planificación. |
| Sprint Review | Al cierre de cada sprint; 45–60 min. | Equipo e interesados/representante de negocio. | Demostración real del incremento contra criterios, feedback registrado, historias aceptadas o devueltas al backlog. |
| Sprint Retrospective | Después de la Review; 30–45 min. | Equipo Scrum. | Qué funcionó, qué mejorar y 1–3 acciones con responsable y fecha para el siguiente sprint. |
| Release / revisión de integración | Cuando exista un incremento candidato; 30 min según necesidad. | Developers y revisor independiente. | CI verde, pruebas, Quality Gate cuando esté configurado, PR aprobado por otro integrante y release/trazabilidad actualizados. |

Estas duraciones son una propuesta de trabajo para el equipo, no evidencia de reuniones ya celebradas. Deben calendarizarse y registrarse realmente.

## 12.1.4 Propuesta de organización del backlog por sprint

La asignación es una **propuesta inicial para discusión**, no un compromiso de capacidad ni fechas. Sprint 1 ya existe en Jira como sprint futuro; los demás deben crearse en el tablero MARKET si el equipo aprueba esta distribución.

| Sprint propuesto | Objetivo | Historias sugeridas | Dependencias / salida |
|---|---|---|---|
| Sprint 1 — Fundamentos y oferta | Identidad, perfil, publicación y disponibilidad inicial. | HU-01, HU-02, HU-03, HU-04, HU-05, HU-06. | Permisos, modelo de usuario, estados de publicación y calendario coherentes. |
| Sprint 2 — Descubrimiento y reserva | Permitir descubrir, cotizar y reservar con consistencia. | HU-07, HU-08, HU-09, HU-10, HU-11. | Búsqueda solo sobre publicaciones activas, tarifa definida, control de solapamiento e idempotencia. |
| Sprint 3 — Ciclo posterior a reserva | Cancelación, notificaciones y reputación. | HU-12, HU-13, HU-14, HU-15. | Política de cancelación y definición de estancia completada aprobadas. |
| Sprint 4 — Administración y calidad | Moderación, métricas, exportación y observabilidad. | HU-16, HU-17, HU-18, HU-19. | Fórmulas de indicadores, auditoría, privacidad y exportación verificables. |

Antes de iniciar cada sprint, el equipo debe validar los puntos totales frente a la capacidad real. HU-10 está estimada inicialmente en 13 puntos; si el equipo considera que es demasiado grande, dividir trabajo técnico en subtareas no sustituye la posible división de la propia historia cuando no pueda completarse en un sprint.

## 12.2 WBS/EDT — Entrega 1

| Código | Paquete de trabajo | Actividades | Resultado verificable |
|---|---|---|---|
| 1.0 | Inicio y planeación | Confirmar equipo, metodología, roles, repositorio, definición de Ready/Done y calendario. | Acuerdos, cronograma y organización Jira. |
| 1.1 | Visión y alcance | Entender contexto, objetivos de negocio, actores y límites del MVP. | Documento de visión/alcance revisado. |
| 1.2 | Actores e interesados | Identificar actores directos/interesados, objetivos, preocupaciones, responsabilidades y afectaciones. | Matriz de actores con justificación. |
| 1.3 | Procesos de negocio | Modelar publicación, búsqueda/reserva, cancelación, moderación/auditoría/reportes; revisar decisiones. | Modelos BPMN y relación con requisitos. |
| 1.4 | Ingeniería de requerimientos | Catalogar RF/RNF/RN, analizar ambigüedades, priorizar el MVP y validar historias. | Catálogo, backlog Jira y criterios. |
| 1.5 | Casos de uso y prototipos | Especificar flujos principales, alternos y prototipo inicial para validación. | Casos de uso y evidencia de feedback real. |
| 1.6 | Modelo de dominio/datos | Definir entidades, relaciones, estados, restricciones y titularidad de datos. | Modelo de dominio y modelo lógico de datos. |
| 1.7 | Diseño orientado a servicios | Delimitar responsabilidades, contratos, interacciones, propiedad de datos e integración de pago test. | Diagrama de contexto/solución, secuencias y OpenAPI. |
| 1.8 | Trazabilidad y validación | Conectar requisitos, historias, criterios, modelos, evidencias previstas y reglas de cambio. | Matriz de trazabilidad inicial aprobada. |
| 1.9 | Gestión de documentación y evidencias | Mantener Confluence, actualizar Jira y versionar fuentes/decisiones en GitHub. | Índice y evidencia navegable. |
| 1.10 | Revisión/sustentación entrega 1 | Inspeccionar cobertura de la guía, resolver inconsistencias, preparar defensa técnica. | Checklist completo y presentación/sustentación preparada. |

## 12.3 Cronograma relativo sugerido

| Periodo relativo | Enfoque | Dependencias / salida |
|---|---|---|
| Semana 1 | Kickoff, metodología, visión, alcance, actores/interesados y backlog inicial. | Confirmar objetivo, integrantes, criterio de éxito y calendario real. |
| Semana 2 | Procesos y reglas; RF/RNF; identificar contradicciones/preguntas abiertas. | Aprobación de actores/procesos por el equipo. |
| Semana 3 | Historias con AC, casos de uso, prototipo y primera matriz de trazabilidad. | Refinamiento y revisión de criterios. |
| Semana 4 | Dominio/datos, arquitectura de servicios, diagramas de secuencia y contratos OpenAPI. | Reglas y límites de servicios suficientemente estables. |
| Semana 5 | Revisión integral, corrección de inconsistencias, Confluence/evidencia, sustentación. | Checklist de entrega aprobado por el equipo y fechas ajustadas al cronograma docente. |

Este calendario es un ejemplo de planificación, no las fechas oficiales del curso. Si la entrega real vence antes, comprimir actividades manteniendo validación y trazabilidad.

## 12.4 Dependencias críticas

- RF-001/roles y permisos preceden a operaciones de perfil, publicación y administración.
- Publicación/detalle y servicios preceden a búsqueda; disponibilidad precede a cotización/reserva.
- Cotización y reserva requieren decisión sobre tarifas, idempotencia, solapamiento y estados de pago.
- Cancelación requiere política definida y regla clara de liberación de fechas.
- Reseñas requieren definición de estancia completada y vínculo con reserva.
- Dashboard/reportes dependen de reglas de cómputo y datos fiables/auditables.
- SonarCloud necesita proyecto, organización, clave y token; protección de branch requiere checks reales publicados.

## 12.5 Registro de riesgos

Escala inicial cualitativa (probabilidad/impacto): baja, media, alta. Reevaluar en cada revisión.

| ID | Riesgo | Prob. | Impacto | Respuesta preventiva | Señal de alerta | Propietario propuesto |
|---|---|---|---|---|---|---|
| R-01 | Requerimientos ambiguos de tarifas, cancelación o pago provocan retrabajo. | Alta | Alta | Mantener preguntas abiertas y conseguir decisión explícita antes de implementar. | Historias cambian por reglas no documentadas. | Líder de requerimientos por asignar |
| R-02 | Dos solicitudes concurrentes confirman el mismo intervalo. | Media | Alta | Definir restricción/transacción, clave idempotente y pruebas concurrentes. | Prueba logra dos reservas confirmadas solapadas. | Responsable backend por asignar |
| R-03 | Arquitectura llamada “servicios” sin límites claros genera acoplamiento. | Media | Alta | Documentar ownership de datos, contratos e interacciones; justificar si es modular o distribuida. | Un servicio edita tablas de otro sin contrato. | Arquitectura por asignar |
| R-04 | Falta de integración real con Jira/Confluence impide demostrar trazabilidad. | Media | Alta | Crear backlog y documentación en herramientas obligatorias; enlazar claves reales desde matriz. | CSV tiene links vacíos o solo historias no creadas en Jira. | Scrum Master/gestor backlog por asignar |
| R-05 | Pipeline o reglas de branch impiden integrar por checks mal nombrados. | Media | Media | Ejecutar CI, confirmar nombres reales de check y solo luego exigirlos. | Check requerido no aparece en PR. | Maintainer GitHub por asignar |
| R-06 | SonarCloud falla por token/organización/proyecto sin configurar. | Alta hasta configuración | Media | Añadir variables y secreto de forma segura; verificar scan antes de exigir el check. | Workflow omitido o scanner no autorizado. | Maintainer GitHub por asignar |
| R-07 | Integración de pago test queda fuera del flujo funcional. | Media | Alta | Definir contrato sandbox, escenarios éxito/fallo/reintento y no usar credenciales reales. | Reserva se confirma sin registrar estado de simulación. | Responsable pagos por asignar |
| R-08 | Se sobreestima el alcance y faltan capacidades obligatorias para demo. | Media | Alta | Priorizar MVP obligatorio; opcionales solo tras funcionalidades/pruebas/sustentación completas. | Trabajo en mapas/chat antes de búsqueda-reserva estable. | Product owner por asignar |
| R-09 | La documentación no refleja el código o estado real. | Media | Alta | Revisar trazabilidad por PR; documentar decisiones de cambio y versiones. | OpenAPI/diagramas no concuerdan con endpoints/campos implementados. | Equipo |
| R-10 | El repositorio queda visible públicamente sin decisión consciente. | Media | Media | Acordar visibilidad con el equipo/docente; revisar configuración y secretos antes de publicar. | Repositorio expone información interna o credenciales. | Propietario repositorio |
| R-11 | Ausencia de feedback real de usuarios/interesados invalida supuestos del prototipo. | Alta | Media | Registrar quién revisó qué y observaciones reales; no fabricar evidencia. | Se afirma validación sin registro de revisión. | Responsable UX/requerimientos por asignar |
| R-12 | Fechas académicas reales no encajan con cronograma relativo. | Media | Alta | Sustituir semanas relativas por fechas oficiales y replanificar semanalmente. | Hitos vencidos sin entregables revisados. | Equipo |

## 12.6 Recursos y responsabilidades (propuesta RACI inicial)

Roles RACI deben mapearse a nombres reales en Jira/Confluence; no se asignan nombres ficticios.

| Actividad | Responsable de ejecución (R) | Aprobador (A) | Consultados (C) | Informados (I) |
|---|---|---|---|---|
| Requerimientos / historias | Analista por asignar | Product Owner/equipo por acordar | Integrantes, docente si aplica | Equipo |
| Procesos/arquitectura/modelos | Diseñador por asignar | Equipo | Backend/frontend, interesados | Equipo |
| Repo/CI/seguridad | Maintainer por asignar | Propietario repo | Todos los desarrolladores | Equipo |
| Pruebas de aceptación | QA por asignar | Responsable de historia | Autor de historia, usuario/actor representado | Equipo |
| Control de cambios/trazabilidad | Gestor backlog por asignar | Equipo | Todos los responsables | Docente en sustentación |

## 12.7 Reuniones y evidencias que guardar

- Planificación: objetivo, historias seleccionadas, estimación y capacidad.
- Seguimiento: bloqueos y acuerdos/acciones.
- Revisión: historia mostrada, criterios pasados/fallidos, observaciones.
- Retrospectiva: qué mantener/mejorar y acciones asignadas.
- Validación de artefactos: participantes, fecha, versión revisada y observaciones reales.

Guardar enlaces/capturas auténticas en las herramientas de curso. No reemplazar evidencia real por ejemplos de este documento.
