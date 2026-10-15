# 12. Planificación, WBS/EDT y riesgos

**Cautela:** no se ha aportado la fecha oficial de entrega académica. La planificación del producto sí está distribuida en ocho sprints semanales en Jira; los Story Points siguen siendo iniciales y la capacidad debe validarse con el equipo antes de cada compromiso.

## 12.1 Enfoque ágil adoptado: Scrum + Lean

El proyecto se organiza con **Scrum como marco de trabajo** y **Lean como principios de flujo y mejora continua**. Scrum estructura backlog, objetivos semanales, seguimiento, review y retrospectiva; Lean orienta a priorizar valor, limitar el trabajo en curso, reducir esperas/retrabajo y asegurar calidad desde el inicio. La descripción normativa está en [19. Metodología Scrum + Lean](19-metodologia-scrum-lean.md).

## 12.1.1 Roles operativos

| Rol | Responsable | Responsabilidad principal |
|---|---|---|
| Product Owner (PO) | Juan Sebastián Morales Forero | Priorizar backlog, aclarar reglas de negocio y criterios de aceptación, proponer objetivos y revisar el incremento contra los criterios. |
| Scrum Master / facilitador | Julián David Aguilera Párraga | Facilitar el proceso, hacer visibles impedimentos y promover la mejora continua sin actuar como jefe del equipo. |
| Developers | Juan Sebastián Morales Forero y Julián David Aguilera Párraga | Diseño, implementación, pruebas, revisión de código, documentación y trazabilidad. Ambos siguen participando en desarrollo y validación cruzada. |
| Interesado de negocio | Representante de Inversiones LR simulado para el curso | Aportar o revisar necesidades cuando esté disponible. No se presume feedback ni aprobación sin registro real. |

Esta asignación refleja la decisión operativa de planificación registrada para el proyecto. Debe quedar ratificada explícitamente por ambos integrantes y ajustarse si el curso o el representante de negocio define restricciones adicionales.

## 12.1.2 Sprints semanales configurados en Jira

Jira contiene ocho sprints semanales desde el 19 de octubre hasta el 14 de diciembre de 2026. Sus límites están configurados para iniciar y terminar los lunes a las 09:00, hora de Bogotá.

| Sprint | Fechas locales | Objetivo resumido | Historias | Puntos iniciales |
|---|---|---|---|---:|
| MARKET Sprint 1 | 19–26 oct 2026 | Identidad, recuperación y perfil | HU-01–HU-03 | 11 |
| MARKET Sprint 2 | 26 oct–2 nov 2026 | Publicaciones y moderación básica | HU-04–HU-05 | 13 |
| MARKET Sprint 3 | 2–9 nov 2026 | Disponibilidad y búsqueda | HU-06–HU-07 | 13 |
| MARKET Sprint 4 | 9–16 nov 2026 | Detalle y cotización | HU-08–HU-09 | 8 |
| MARKET Sprint 5 | 16–23 nov 2026 | Creación e historial de reservas | HU-10–HU-11 | 16 |
| MARKET Sprint 6 | 23–30 nov 2026 | Cancelación, notificaciones y reseñas | HU-12–HU-14 | 16 |
| MARKET Sprint 7 | 30 nov–7 dic 2026 | Moderación, administración y dashboard | HU-15–HU-17 | 16 |
| MARKET Sprint 8 | 7–14 dic 2026 | Reportes y trazabilidad técnica | HU-18–HU-19 | 13 |
| **Total** | **8 semanas** | **Alcance completo planificado** | **19 historias** | **106** |

La disponibilidad estimada es de 4–6 horas por semana por integrante (8–12 horas combinadas). La velocidad real todavía no se conoce y los Story Points son estimaciones iniciales; por tanto, 106 puntos en ocho semanas no se considera capacidad validada. En la planificación se revisarán horas disponibles, dependencias y tamaño, y al terminar el primer sprint se recalibrará el plan usando trabajo realmente terminado. Los sprints con 16 puntos requieren especial revisión y HU-10 (13 puntos) debe refinarse para confirmar que sea terminable en una semana.

## 12.1.3 Cadencia, calidad y mejora Lean

Las ceremonias y prácticas se detallan en [19. Metodología Scrum + Lean](19-metodologia-scrum-lean.md). Como mínimo, cada semana se debe registrar planificación y objetivo, seguimiento/bloqueos, review con criterios realmente demostrados y retrospectiva con una o dos acciones de mejora con responsable y fecha. Para reducir desperdicio, se limitará el WIP del equipo a un máximo inicial de dos historias activas de implementación, se harán visibles los tiempos de espera de revisión y se priorizará terminar/revisar trabajo en curso antes de empezar más trabajo.

## 12.2 WBS/EDT — Entrega 1

| Código | Paquete de trabajo | Actividades | Resultado verificable |
|---|---|---|---|
| 1.0 | Inicio y planeación | Formalizar Scrum + Lean, roles, Definition of Ready/Done, repositorio y calendario de sprints. | Acuerdos, cronograma y organización Jira. |
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
| R-04 | Falta de integración real con Jira/Confluence impide demostrar trazabilidad. | Media | Alta | Crear backlog y documentación en herramientas obligatorias; enlazar claves reales desde matriz. | CSV tiene links vacíos o solo historias no creadas en Jira. | Julián (facilitación) / Juan (priorización del backlog) |
| R-05 | Pipeline o reglas de branch impiden integrar por checks mal nombrados. | Media | Media | Ejecutar CI, confirmar nombres reales de check y solo luego exigirlos. | Check requerido no aparece en PR. | Maintainer GitHub por asignar |
| R-06 | SonarCloud falla por token/organización/proyecto sin configurar. | Alta hasta configuración | Media | Añadir variables y secreto de forma segura; verificar scan antes de exigir el check. | Workflow omitido o scanner no autorizado. | Maintainer GitHub por asignar |
| R-07 | Integración de pago test queda fuera del flujo funcional. | Media | Alta | Definir contrato sandbox, escenarios éxito/fallo/reintento y no usar credenciales reales. | Reserva se confirma sin registrar estado de simulación. | Responsable pagos por asignar |
| R-08 | Se sobreestima el alcance y faltan capacidades obligatorias para demo. | Media | Alta | Priorizar MVP obligatorio; opcionales solo tras funcionalidades/pruebas/sustentación completas. | Trabajo en mapas/chat antes de búsqueda-reserva estable. | Juan Sebastián Morales Forero (PO) / equipo |
| R-09 | La documentación no refleja el código o estado real. | Media | Alta | Revisar trazabilidad por PR; documentar decisiones de cambio y versiones. | OpenAPI/diagramas no concuerdan con endpoints/campos implementados. | Equipo |
| R-10 | El repositorio queda visible públicamente sin decisión consciente. | Media | Media | Acordar visibilidad con el equipo/docente; revisar configuración y secretos antes de publicar. | Repositorio expone información interna o credenciales. | Propietario repositorio |
| R-11 | Ausencia de feedback real de usuarios/interesados invalida supuestos del prototipo. | Alta | Media | Registrar quién revisó qué y observaciones reales; no fabricar evidencia. | Se afirma validación sin registro de revisión. | Responsable UX/requerimientos por asignar |
| R-12 | Fechas académicas reales no encajan con cronograma relativo. | Media | Alta | Sustituir semanas relativas por fechas oficiales y replanificar semanalmente. | Hitos vencidos sin entregables revisados. | Equipo |

## 12.6 Recursos y responsabilidades (RACI operativo inicial)

La asignación operativa se documenta abajo y debe ratificarse por ambos integrantes. Los responsables detallados por historia y subtarea están en [18. Desglose de subtareas y planificación Jira](18-plan-subtareas-jira.md).

| Actividad | Responsable de ejecución (R) | Aprobador (A) | Consultados (C) | Informados (I) |
|---|---|---|---|---|
| Requerimientos / historias | Juan Sebastián Morales Forero (PO) con apoyo del equipo | Equipo; PO para priorización y criterios | Integrantes, docente si aplica | Equipo |
| Procesos/arquitectura/modelos | Ambos Developers | Equipo | Interesado de negocio / docente cuando aplique | Equipo |
| Repo/CI/seguridad | Integrante asignado a la tarea Jira; colaboración de ambos | Equipo / propietario del repositorio | Ambos Developers | Equipo |
| Pruebas de aceptación | Integrante distinto del implementador de la historia | PO/equipo con criterios demostrados | Autor de historia, usuario/actor representado | Equipo |
| Control de cambios/trazabilidad | Juan mantiene priorización del backlog; Julián facilita seguimiento y mejora | Equipo | Todos los responsables | Docente en sustentación |

## 12.7 Reuniones y evidencias que guardar

- Planificación: objetivo, historias seleccionadas, estimación y capacidad.
- Seguimiento: bloqueos y acuerdos/acciones.
- Revisión: historia mostrada, criterios pasados/fallidos, observaciones.
- Retrospectiva: qué mantener/mejorar y acciones asignadas.
- Validación de artefactos: participantes, fecha, versión revisada y observaciones reales.

Guardar enlaces/capturas auténticas en las herramientas de curso. No reemplazar evidencia real por ejemplos de este documento.
