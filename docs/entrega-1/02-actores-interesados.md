# 2. Actores e interesados

La guía solicita distinguir actores que interactúan directamente, interesados, responsables de administración/soporte y personas afectadas por disponibilidad, reservas, cancelaciones y publicaciones. Las necesidades siguientes se derivan del contexto y las capacidades obligatorias del MVP; deberán validarse con el grupo.

## 2.1 Actores del sistema

| ID | Actor | Descripción / relación con Inversiones LR | Objetivos y necesidades | Preocupaciones | Responsabilidades y funciones que lo afectan |
|---|---|---|---|---|---|
| ACT-01 | Huésped | Usuario que busca, consulta, cotiza, reserva y cancela alojamientos. | Comparar opciones, conocer disponibilidad y precio total, gestionar sus reservas y reseñar estancias terminadas. | Precios poco claros, datos desactualizados, reservas duplicadas, cancelación sin claridad. | Mantener su perfil, buscar, cotizar, reservar, cancelar, revisar historial, recibir notificaciones y reseñar solo reservas completadas. |
| ACT-02 | Anfitrión | Usuario que publica y administra uno o más alojamientos. | Administrar información, tarifas, servicios, reglas, disponibilidad y política de cancelación. | Cambios no autorizados, periodos bloqueados incorrectamente, publicación rechazada/bloqueada sin trazabilidad. | Crear/editar sus publicaciones, mantener información mínima, administrar disponibilidad y consultar reservas relacionadas. |
| ACT-03 | Administrador | Responsable de administración y moderación de la plataforma. | Mantener confiabilidad, atender incidentes, restringir usuarios y publicaciones engañosas, auditar eventos. | Acciones sin rastro, abuso de permisos, reservas futuras que desaparecen al bloquear un anuncio. | Bloquear publicaciones, restringir usuarios, moderar reseñas, consultar auditoría, reportes y dashboard. |
| ACT-04 | Proveedor de pagos en pruebas | Sistema externo de prueba usado para simular pagos, por ejemplo Stripe en modo test. | Procesar escenarios simulados de éxito o fallo sin movimiento monetario real. | Exposición de credenciales, confundir pagos simulados con cobros reales. | Responder a la integración de pago de prueba; la plataforma debe almacenar el resultado necesario para el flujo sin guardar secretos. |

## 2.2 Interesados adicionales

| ID | Interesado | Relación con la empresa | Necesidad / preocupación | Decisiones o resultados que le afectan |
|---|---|---|---|---|
| INT-01 | Dirección/propietarios de Inversiones LR | Patrocinador de negocio y receptor de indicadores. | Visibilidad sobre oferta, reservas, ingresos estimados y demanda por ciudad; trazabilidad. | Alcance, reglas, indicadores, reportes y prioridades de evolución. |
| INT-02 | Equipo de desarrollo | Responsable de analizar, diseñar, implementar y validar el producto académico. | Requerimientos estables, límites de servicios claros, backlog priorizado y entorno reproducible. | Arquitectura, responsabilidades, estimaciones, dependencias, calidad y entrega. |
| INT-03 | Docente/evaluador | Evalúa cumplimiento de la guía y sustentación técnica. | Evidencia coherente, modelos legibles, trazabilidad y comprensión individual del trabajo. | Criterios de aceptación, entregables, sustentación y evaluación. |
| INT-04 | Soporte/operación de plataforma | Atiende incidentes y apoya usuarios; rol operativo previsto, no necesariamente actor independiente del MVP. | Diagnóstico por logs, eventos auditables, guía de despliegue y errores reproducibles. | Registro de fallos, soporte de reservas, estado del sistema y procedimientos. |
| INT-05 | Propietarios de inmuebles representados por anfitriones | Interesados en la correcta representación comercial de sus propiedades. | Publicaciones fieles, tarifas y disponibilidad actualizadas, reglas visibles. | Calidad del anuncio, precio, servicios, cancelaciones y reputación. |
| INT-06 | Huéspedes nacionales e internacionales | Segmentos de demanda mencionados por la guía. | Información comprensible y fiable para comparar alojamientos temporales regionales. | Búsqueda, detalle, cotización, reserva y comunicación del estado. |

## 2.3 Mapa de influencia y participación

| Grupo | Influencia | Participación esperada | Mecanismo propuesto |
|---|---|---|---|
| Dirección de Inversiones LR | Alta | Define objetivos y valida indicadores/prioridades. | Revisión de visión y demostración de prototipos. |
| Administrador | Alta en operación | Define/ejecuta moderación, restricciones y auditoría dentro de reglas aprobadas. | Casos de uso y pruebas de permisos. |
| Anfitrión | Alta sobre la oferta | Mantiene publicaciones, precio, servicios y disponibilidad. | Historias, pruebas de aceptación y feedback. |
| Huésped | Alta sobre demanda y reservas | Usa búsqueda, cotización, reserva y cancelación. | Prototipo y escenarios end-to-end. |
| Equipo de desarrollo | Alta técnica | Gestiona backlog, diseño, CI, pruebas y trazabilidad. | Jira, Confluence, GitHub y revisión de PR. |
| Docente/evaluador | Alta sobre criterios académicos | Verifica entregables y sustentación. | Evidencias/versiones, revisión y sustentación. |
| Proveedor de pagos de prueba | Influencia técnica externa | Responde al flujo simulado de pagos. | Contrato de integración y pruebas sandbox. |

## 2.4 Matriz actor–capacidad

| Capacidad | Huésped | Anfitrión | Administrador | Proveedor de pagos |
|---|---|---|---|---|
| Registro/inicio de sesión | Sí | Sí | Sí | No aplica |
| Perfil y cuenta | Propio | Propio | Consulta/restricción según permisos | No aplica |
| Crear y mantener publicación | No | Propias | Moderación/bloqueo | No aplica |
| Administrar disponibilidad | Consulta | Administra propias | Consulta operativa si se implementa permiso | No aplica |
| Buscar y cotizar | Sí | Puede consultar como usuario solo si se define | Consulta/soporte si aplica | No aplica |
| Crear/cancelar reserva | Propias | Consulta reservas asociadas | Revisión manual de afectaciones; no cancelar automáticamente por bloquear un anuncio | Apoya pago simulado |
| Reseñas | Escribe según reserva completada | Consulta las publicadas | Modera/oculta | No aplica |
| Dashboard y reportes globales | No | No, salvo reporte propio si se prioriza | Sí | No aplica |
| Simular pago | Inicia/consulta resultado de su reserva | Consulta estado asociado si se define | Auditoría según permisos | Procesa resultado de prueba |

## 2.5 Decisiones por confirmar

- Si el anfitrión es un rol de usuario separado o una cuenta con permisos adicionales; la guía establece roles HUÉSPED, ANFITRIÓN y ADMINISTRADOR.
- Si soporte es un actor autenticado o un interesado operativo fuera del MVP.
- Si los administradores pueden consultar datos personales completos o solo los mínimos necesarios.
- Qué permisos específicos tiene administración sobre reseñas y reservas futuras.
