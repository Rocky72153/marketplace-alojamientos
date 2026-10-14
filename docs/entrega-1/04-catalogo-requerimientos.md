# 4. Catálogo de requerimientos y reglas del negocio

**Fuente primaria:** guía oficial del proyecto Marketplace Alojamientos, secciones 5 y 6.  
**Estado:** especificación inicial derivada del enunciado; debe revisarse con actores y validarse antes de marcar historias como listas para desarrollo.

## 4.1 Requerimientos funcionales

| ID | Requerimiento | Prioridad | Criterio general de verificación | Épica |
|---|---|---|---|---|
| RF-001 | Registrar e iniciar sesión con roles HUÉSPED, ANFITRIÓN y ADMINISTRADOR. | Must | Cuenta válida puede autenticarse; inválida no obtiene sesión ni rutas protegidas. | EP-01 Identidad |
| RF-002 | Recuperar contraseña mediante flujo simulado. | Must | Sistema confirma recepción simulada sin revelar si un correo ajeno está registrado. | EP-01 Identidad |
| RF-003 | Consultar/actualizar perfil básico y activar/desactivar cuenta. | Must | Usuario modifica datos permitidos y el sistema respeta el estado de cuenta. | EP-01 Identidad |
| RF-004 | Crear, editar y consultar publicaciones de alojamiento. | Must | Solo anfitrión crea; anfitrión dueño o administrador autorizado puede modificar. | EP-02 Publicaciones |
| RF-005 | Gestionar estados BORRADOR, PENDIENTE_REVISION, ACTIVA, PAUSADA y BLOQUEADA. | Must | Se validan transiciones y solo publicaciones activas aparecen en búsqueda o reciben reservas. | EP-02 Publicaciones |
| RF-006 | Mantener los datos mínimos de publicación: título, descripción, ubicación textual, tipo, capacidad, precio/noche, servicios, reglas e imágenes. | Must | No se activa una publicación incompleta. | EP-02 Publicaciones |
| RF-007 | Administrar periodos disponibles por alojamiento. | Must | Anfitrión gestiona fechas de sus publicaciones y se bloquean periodos de reservas confirmadas. | EP-03 Disponibilidad |
| RF-008 | Buscar con filtros de ciudad, fechas, capacidad, tipo, rango de precio y servicios. | Must | Resultados respetan filtros y excluyen alojamientos no activos/no disponibles para fechas. | EP-04 Descubrimiento |
| RF-009 | Consultar detalle de publicación para comparar opciones. | Must | Detalle muestra información necesaria de alojamiento, capacidad, precio, servicios, reglas y estado público. | EP-04 Descubrimiento |
| RF-010 | Calcular cotización desglosada. | Must | Presenta noches, precio base, limpieza, tarifa de servicio, descuentos, extras y total. | EP-05 Cotización/reserva |
| RF-011 | Crear reserva de forma consistente y consultar estado e historial. | Must | Reserva válida se registra una sola vez y estado queda consultable por usuarios autorizados. | EP-05 Cotización/reserva |
| RF-012 | Simular pago usando proveedor en modo de pruebas; no procesar dinero real. | Must | Se registra resultado de simulación con identificador y estado no ambiguos. | EP-05 Cotización/reserva |
| RF-013 | Cancelar una reserva aplicando política de la publicación. | Must | Registra usuario, fecha, motivo, devolución simulada y estado; libera periodo cuando la regla lo autoriza. | EP-06 Cancelaciones |
| RF-014 | Registrar y mostrar notificaciones internas. | Must | Eventos relevantes de reserva/cancelación/publicación generan notificación consultable. | EP-07 Notificaciones |
| RF-015 | Crear reseña/calificación tras estancia completada y consultar reseñas del alojamiento. | Must (el alcance del MVP la incluye) | Solo huésped con reserva completada asociada al alojamiento puede reseñar; no duplica reseña para esa reserva. | EP-08 Reputación |
| RF-016 | Moderar reseñas y ocultar las que infringen políticas. | Must | Administrador puede ocultar con actor/fecha/motivo trazables; reseña original se conserva para auditoría según política de retención por definir. | EP-08 Reputación |
| RF-017 | Bloquear publicación, restringir usuario y consultar auditoría. | Must | Acciones administrativas autorizadas dejan registro; reservas futuras de anuncio bloqueado se preservan para revisión manual. | EP-09 Administración |
| RF-018 | Dashboard con usuarios, publicaciones activas, reservas por estado, ingresos estimados y ciudades de mayor demanda. | Must | Indicadores se calculan a partir de datos existentes y fecha de corte/definición documentadas. | EP-09 Administración |
| RF-019 | Exportar reportes de gestión a PDF o Excel para usuarios, publicaciones, reservas, ingresos estimados y demanda. | Must | Archivo corresponde a datos del sistema y se audita solicitante y fecha de exportación. | EP-09 Administración |
| RF-020 | Mantener logs estructurados de autenticación, publicación, reserva, cancelación, reseña, error y administración. | Must | Eventos incluyen como mínimo timestamp, tipo, actor cuando aplique, entidad/referencia y resultado; no registran contraseñas/tokens. | EP-10 Observabilidad |

## 4.2 Requerimientos no funcionales

| ID | Requerimiento | Verificación propuesta | Estado |
|---|---|---|---|
| RNF-001 | Seguridad: rutas y operaciones protegidas por rol y propietario del recurso. | Pruebas de acceso permitido/denegado para cada rol y usuario distinto del dueño. | Umbrales y matriz de permisos detallada pendiente. |
| RNF-002 | Consistencia: una sola reserva confirmada puede ocupar un periodo de un alojamiento; validar en confirmación. | Pruebas concurrentes que intenten confirmar fechas superpuestas; máximo una confirma. | Requisito explícito de la guía. |
| RNF-003 | Idempotencia o mecanismo equivalente en operaciones de reserva ante reintentos. | Repetir la solicitud con la misma clave/referencia y verificar que no duplique reserva. | Requisito explícito de la guía; mecanismo por diseñar. |
| RNF-004 | Trazabilidad de eventos de seguridad y cambios de estado relevantes. | Inspección de audit logs para autenticación, publicación, reserva, cancelación y acciones admin. | Requisito explícito de la guía. |
| RNF-005 | Usabilidad: formularios, errores y desglose de precios deben ser comprensibles. | Pruebas de aceptación con escenarios y revisión del prototipo; criterios medibles por acordar. | Propuesta de calidad, validar con interesados. |
| RNF-006 | Mantenibilidad: servicios con responsabilidades delimitadas, contratos claros y código verificable. | Revisión de diseño/API y análisis estático integrado a PR. | Respaldado por requisito de arquitectura orientada a servicios y SonarCloud. |
| RNF-007 | Portabilidad/reproducibilidad de desarrollo y pruebas. | Un integrante distinto puede ejecutar instrucciones desde un clon limpio. | Criterio propuesto; confirmar sistemas operativos soportados. |
| RNF-008 | Rendimiento de búsqueda y consulta apto para la demostración académica. | Definir volúmenes y tiempos objetivos y medirlos en pruebas. | La guía no fija umbrales numéricos; no inventar SLA. |
| RNF-009 | Privacidad: no exponer secretos ni registrar credenciales en logs. | Revisión de configuración y pruebas de respuesta/log. | Buena práctica de diseño derivada de seguridad. |

## 4.3 Reglas de negocio

| ID | Regla | Interpretación verificable |
|---|---|---|
| RN-001 | Cada publicación pertenece a un único anfitrión. | La publicación guarda un anfitrión; no se acepta propietario ambiguo. |
| RN-002 | Solo anfitrión crea; dueño o administrador puede modificar. | Autorización se valida en backend, no solo en interfaz. |
| RN-003 | Estados: BORRADOR, PENDIENTE_REVISION, ACTIVA, PAUSADA y BLOQUEADA. | Transiciones de estado se implementan y auditan. |
| RN-004 | Solo publicaciones activas aparecen en búsquedas y reciben reservas. | Búsqueda y confirmación filtran/validan estado activo. |
| RN-005 | La publicación debe tener datos mínimos antes de activarse. | Validación de título, descripción, ubicación, tipo, capacidad, precio, servicios/reglas e imágenes según implementación. |
| RN-006 | Capacidad > 0 y precio por noche > 0. | Rechazar valores cero/negativos. |
| RN-007 | Fecha de salida posterior a fecha de llegada. | Rechazar rangos vacíos/invertidos. |
| RN-008 | No se confirman reservas superpuestas para un alojamiento. | Revalidar al confirmar con control transaccional adecuado. Intervalos modelados como llegada incluida y salida excluida. |
| RN-009 | El total se compone de precio base, limpieza, servicio y cargos por adicionales; porcentajes configurables. | Guardar desglose/versionar componentes usados para que la reserva conserve el cálculo aceptado. La fórmula exacta de descuentos/extras debe documentarse en la política de cotización. |
| RN-010 | La reserva puede ser inmediata o requerir aprobación según configuración. | La guía también define la reserva inmediata como modelo obligatorio del MVP; propuesta: entregar reserva inmediata y dejar aprobación como extensión configurable, salvo decisión explícita del equipo. |
| RN-011 | Al menos una política de cancelación asociada a la publicación. | El cálculo consulta la política vigente ligada a la publicación; política concreta debe acordarse. |
| RN-012 | Cancelación registra usuario, fecha, motivo, devolución aplicable y estado final. | Los campos no pueden omitirse en la operación aceptada. |
| RN-013 | Reserva idempotente o mecanismo equivalente contra duplicados. | Un reintento lógico no crea reserva adicional. |
| RN-014 | Bloqueo administrativo conserva reservas futuras para auditoría y las pasa a revisión manual. | No borrar/cancelar silenciosamente las reservas futuras al bloquear el anuncio. |
| RN-015 | Acción de seguridad o cambio de estado relevante deja registro trazable. | Registro consultable de quién, qué, cuándo y resultado. |
| RN-016 | Pagos no reales; usar servicio en modo de pruebas para simular pagos. | No cobrar ni usar credenciales de producción. |
| RN-017 | Solo huésped con reserva completada para ese alojamiento puede reseñar/calificar. | Verificar relación reserva-huésped-alojamiento y estado completado. |
| RN-018 | Reseñas/calificaciones respetan moderación administrativa. | Una reseña puede ocultarse conforme a política; conservar rastro de moderación. |
| RN-019 | Los reportes corresponden a información existente y auditan solicitante/fecha. | Registrar parámetros básicos del reporte y resultado. |

## 4.4 Preguntas abiertas de requisitos

1. ¿Qué fórmula y porcentajes predeterminados se usarán para limpieza, tarifa de servicio, descuentos y servicios adicionales?
2. ¿Cuál es la política concreta de cancelación y el cálculo de devolución simulada?
3. ¿Cuáles transiciones exactas de publicación requieren revisión administrativa?
4. ¿Cómo se define una estancia completada para habilitar reseñas en ausencia de un proceso operacional externo?
5. ¿Qué ocurre con una reserva si el proveedor de pago de pruebas simula error o queda sin respuesta?
6. ¿Qué intervalos de fecha y zona horaria se admiten? ¿Se excluye el día de salida del bloqueo? Propuesta habitual: sí, llegada incluida/salida excluida, confirmar.
7. ¿Qué métricas y filtros requieren los reportes y dashboard? La guía da categorías mínimas, pero no define formato exacto.
8. ¿Qué requisitos RNF cuantitativos (tiempos de respuesta, concurrencia y disponibilidad) desea aprobar el interesado? El enunciado no ofrece cifras.
