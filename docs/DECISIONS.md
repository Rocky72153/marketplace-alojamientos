# Registro de decisiones técnicas y de negocio

Las entradas marcadas como propuestas no constituyen acuerdos adoptados hasta ser revisadas por el equipo y, cuando corresponda, por el interesado/docente. Actualizar responsable, fecha de decisión, justificación y enlaces reales a Jira/Confluence.

## Decisiones de arquitectura y proceso

| ID | Fecha de registro | Decisión/propuesta | Motivo | Estado |
|---|---|---|---|---|
| ADR-001 | 2026-10-13 | Base técnica provisional: Java 21 + Spring Boot + PostgreSQL; frontend estático inicial. | Permite iniciar CI y un prototipo sin elegir un framework frontend no confirmado. | Propuesta; requiere aprobación del equipo |
| ADR-002 | 2026-10-13 | main para versión estable y develop para integración. | Separar entrega estable y trabajo en curso. | Estructura creada; reglas de protección pendientes |
| ADR-003 | 2026-10-13 | Límites de servicio lógicos dentro de un backend modular como punto de partida; no asumir microservicios desplegados por separado. | La guía pide arquitectura orientada a servicios y límites claros, pero no impone despliegues independientes. | Propuesta; equipo debe aprobar y justificar |
| ADR-004 | 2026-10-13 | El MVP implementa reserva inmediata como flujo obligatorio; aprobación manual de reserva se deja para una decisión explícita/extensión configurada. | El glosario define reserva inmediata como modelo obligatorio del MVP y la regla general permite configuraciones de aprobación. | Interpretación propuesta; validar con equipo/docente |
| ADR-005 | 2026-10-13 | Pagos exclusivamente en modo de pruebas; nunca usar credenciales ni cobros de producción. | Restricción explícita de la guía académica. | Restricción obligatoria |
| ADR-006 | 2026-10-13 | Propuesta de intervalos de estancia: llegada incluida y salida excluida. | Permite reservar la noche de salida para una llegada el mismo día sin solapamiento temporal. | Pendiente de decisión de negocio |
| ADR-007 | 2026-10-13 | El snapshot de cotización aceptado se conserva ligado a la reserva. | Evita que un cambio posterior de precio reescriba el histórico de una operación aceptada. | Propuesta de diseño; confirmar |
| ADR-008 | 2026-10-13 | SonarCloud no se exige como check hasta que proyecto, variables y secreto estén configurados y el análisis real pase. | Evita bloquear PR con un check que todavía no puede ejecutarse. | Pendiente de configuración externa |

## Decisiones de negocio pendientes

| ID | Pregunta | Opciones/impacto | Responsable propuesto | Estado |
|---|---|---|---|---|
| DEC-01 | ¿Qué porcentajes y fórmula componen tarifa de servicio, limpieza, descuentos y extras? | Cambia cotización, pruebas, persistencia de snapshot y reportes. | Product Owner/negocio por asignar | Abierta |
| DEC-02 | ¿Cuál es la política concreta de cancelación y devolución simulada? | Cambia elegibilidad, cálculo y liberación de disponibilidad. | Product Owner/negocio por asignar | Abierta |
| DEC-03 | ¿Qué estados/transiciones se aplican a reserva y pago test? | Define fallo, reintentos, expiración, confirmación y liberación de inventario. | Backend + negocio por asignar | Abierta |
| DEC-04 | ¿Cómo se marca una estancia como COMPLETADA? | Condición de elegibilidad para reseñas y algunos reportes. | Product Owner/negocio por asignar | Abierta |
| DEC-05 | ¿Qué publicación requiere PENDIENTE_REVISION y quién la activa? | Cambia la ruta de moderación y disponibilidad pública. | Administrador/product owner por asignar | Abierta |
| DEC-06 | ¿Qué campos/criterios exactos definen el tipo de alojamiento, servicios y reglas? | Afecta catálogo, filtros y criterios de aceptación. | Equipo de requerimientos por asignar | Abierta |
| DEC-07 | ¿Qué permisos administrativos aplican a datos personales, reservas y reportes? | Impacta seguridad, privacidad y auditoría. | Product Owner/equipo por asignar | Abierta |
| DEC-08 | ¿Qué objetivos cuantitativos de rendimiento y datos de prueba se usarán? | La guía no fija tiempos ni volumen; sin definición no se puede declarar un SLA. | Equipo técnico por asignar | Abierta |
| DEC-09 | ¿Cuál es el framework frontend y la estrategia definitiva de despliegue? | Cambia pipeline, estructura y manual de despliegue. | Equipo técnico por asignar | Abierta |
| DEC-10 | ¿Qué metodología, duración de sprint, fechas de entrega y responsables se adoptan? | Cambia cronograma, campos de Jira y evidencia de seguimiento. | Equipo por asignar | Abierta |

## Control de cambios

Cada decisión adoptada debe registrar fecha, participantes/rol, contexto, alternativas analizadas, decisión, consecuencias, requisitos/historias afectadas y links a Jira/Confluence/PR. Cuando cambie una regla, actualizar catálogo, historias, procesos, modelos de dominio/datos, OpenAPI, pruebas y matriz de trazabilidad como parte del mismo cambio revisado por PR.
