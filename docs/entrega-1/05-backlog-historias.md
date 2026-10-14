# 5. Épicas e historias de usuario

Estas 10 épicas y 19 historias están registradas en el proyecto [Jira Marketplace de Alojamientos (MARKET)](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog). Este archivo conserva el detalle funcional y enlaza cada ID HU con su issue Jira. Las estimaciones iniciales de Story Points ya están aplicadas en Jira y las historias se distribuyeron en cuatro sprints como propuesta de planificación; el equipo debe ratificar estimaciones, responsables y fechas/capacidad antes de comprometerlas.

## 5.1 Épicas

| ID | Épica | Objetivo | Resultado de negocio | Jira |
|---|---|---|---|---|
| EP-01 | Identidad y perfil | Administrar identidad, sesión, roles y perfil. | Usuarios legítimos operan según sus permisos. | [MARKET-1](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-1) |
| EP-02 | Publicaciones | Permitir a anfitriones mantener anuncios completos y estados controlados. | Oferta de alojamientos administrada. | [MARKET-2](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-2) |
| EP-03 | Disponibilidad | Administrar periodos y evitar solapamientos. | Fuente central de disponibilidad. | [MARKET-3](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-3) |
| EP-04 | Descubrimiento | Buscar, filtrar y comparar alojamientos. | Huésped identifica una opción pertinente. | [MARKET-4](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-4) |
| EP-05 | Cotización y reserva | Mostrar costes y confirmar reserva idempotente con pago de prueba. | Flujo end-to-end verificable. | [MARKET-24](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-24) |
| EP-06 | Cancelaciones | Aplicar política y gestionar estado, devolución simulada y fechas. | Cancelaciones trazables y consistentes. | [MARKET-25](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-25) |
| EP-07 | Notificaciones | Informar eventos relevantes dentro de la plataforma. | Usuarios conocen los cambios. | [MARKET-26](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-26) |
| EP-08 | Reseñas y reputación | Habilitar evaluación posterior y moderación. | Historial de experiencia asociado a estancia real. | [MARKET-27](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-27) |
| EP-09 | Administración y reportes | Moderar, auditar, medir y exportar datos. | Operación observable y gestionable. | [MARKET-28](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-28) |
| EP-10 | Observabilidad y calidad | Registrar eventos técnicos/de negocio y asegurar flujo de cambios. | Evidencia verificable y soporte a diagnóstico. | [MARKET-29](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-29) |

## 5.2 Historias de usuario sincronizadas con Jira

En todas las historias: **responsable: por asignar; estado inicial: To Do / backlog; sprint: MARKET Sprint 1–4 según la propuesta documentada en `18-plan-subtareas-jira.md`**. Los puntos son una estimación inicial relativa, no una estimación acordada.

### HU-01 — Registro e inicio de sesión · [MARKET-5](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-5)

- **Épica:** EP-01 · **Requisito:** RF-001 · **Prioridad:** Must · **Estimación inicial:** 5 puntos.
- **Como** usuario, **quiero** registrarme e iniciar sesión con mi rol, **para** acceder a capacidades autorizadas.
- **AC1:** Dado que envío los campos de registro completos y válidos, cuando confirmo el registro, entonces el sistema crea una cuenta con uno de los roles permitidos y no expone la contraseña.
- **AC2:** Dado que envío credenciales válidas para una cuenta activa, cuando inicio sesión, entonces el sistema establece una sesión y aplica permisos según rol.
- **AC3:** Dado que las credenciales son incorrectas o la cuenta está desactivada, cuando intento iniciar sesión, entonces el acceso se rechaza con mensaje que no revele datos sensibles.
- **Dependencias:** modelo de usuario y definición de sesión/seguridad.

### HU-02 — Recuperación simulada de contraseña · [MARKET-6](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-6)

- **Épica:** EP-01 · **Requisito:** RF-002 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** usuario, **quiero** solicitar recuperación simulada, **para** conocer el flujo de recuperación sin integrar correo real.
- **AC1:** Dado que ingreso un correo, cuando solicito recuperación, entonces el sistema muestra confirmación neutral y registra el evento sin revelar si existe una cuenta.
- **AC2:** Dado que la función es simulada, cuando termina la operación, entonces no afirma que envió un correo real ni cambia una contraseña sin verificación.
- **Dependencias:** HU-01; mecanismo simulado por definir.

### HU-03 — Perfil y estado de cuenta · [MARKET-7](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-7)

- **Épica:** EP-01 · **Requisito:** RF-003 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** usuario, **quiero** consultar y actualizar mis datos básicos y desactivar mi cuenta, **para** mantener mi información al día y controlar mi participación.
- **AC1:** Dado que tengo sesión válida, cuando consulto mi perfil, entonces veo únicamente mis datos permitidos.
- **AC2:** Dado que envío datos editables válidos, cuando guardo, entonces el sistema persiste los cambios y registra el evento correspondiente.
- **AC3:** Dado que intento modificar el perfil de otra persona sin permiso, cuando envío la solicitud, entonces el sistema la rechaza.
- **Dependencias:** HU-01 y matriz de permisos.

### HU-04 — Crear y mantener publicación · [MARKET-8](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-8)

- **Épica:** EP-02 · **Requisitos:** RF-004, RF-006 · **Prioridad:** Must · **Estimación:** 8 puntos.
- **Como** anfitrión, **quiero** crear y editar publicaciones de mis alojamientos, **para** ofrecerlos a los huéspedes.
- **AC1:** Dado que soy anfitrión autenticado, cuando creo un anuncio, entonces queda asociado únicamente a mi cuenta.
- **AC2:** Dado que envío campos mínimos completos y válidos, cuando guardo el anuncio, entonces se persisten título, descripción, ubicación textual, tipo, capacidad, precio, servicios, reglas e imágenes/referencias.
- **AC3:** Dado que un usuario distinto del propietario intenta editar una publicación, cuando solicita el cambio, entonces el backend lo rechaza salvo permiso administrativo válido.
- **AC4:** Dado que falta un campo obligatorio o capacidad/precio no son positivos, cuando intento activar el anuncio, entonces no se activa y se indican los errores.
- **Dependencias:** HU-01, modelo de alojamiento y catálogo de servicios/reglas.

### HU-05 — Gestionar estados y moderación de publicaciones · [MARKET-9](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-9)

- **Épica:** EP-02/EP-09 · **Requisitos:** RF-005, RF-017 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** anfitrión, **quiero** activar o pausar mis publicaciones y conocer su estado, **para** controlar cuándo están disponibles al público.
- **AC1:** Dado que mi publicación tiene todos los datos mínimos, cuando la activo conforme a las reglas, entonces el estado cambia a ACTIVA y el cambio queda auditado.
- **AC2:** Dado que una publicación está PAUSADA, BLOQUEADA o pendiente, cuando un huésped busca o reserva, entonces el anuncio no aparece como reservable.
- **AC3:** Dado que soy administrador autorizado y hay información engañosa, cuando bloqueo la publicación, entonces el estado queda BLOQUEADA, se registra actor/motivo/fecha y las reservas futuras se conservan para revisión manual.
- **Dependencias:** HU-04, permisos y auditoría.

### HU-06 — Administrar disponibilidad · [MARKET-10](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-10)

- **Épica:** EP-03 · **Requisito:** RF-007 · **Prioridad:** Must · **Estimación:** 8 puntos.
- **Como** anfitrión, **quiero** definir periodos disponibles de mis alojamientos, **para** que las búsquedas reflejen fechas reales.
- **AC1:** Dado que soy propietario del anuncio, cuando guardo un periodo válido, entonces queda asociado al alojamiento correcto.
- **AC2:** Dado que intento definir un periodo con salida igual/anterior a llegada o incompatible con reserva confirmada, cuando lo guardo, entonces el sistema lo rechaza.
- **AC3:** Dado que una reserva se confirma, cuando se actualiza su estado, entonces el periodo queda bloqueado para nuevas reservas de forma consistente.
- **Dependencias:** HU-04; reglas de intervalo de fecha.

### HU-07 — Buscar y filtrar alojamientos · [MARKET-11](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-11)

- **Épica:** EP-04 · **Requisito:** RF-008 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** huésped, **quiero** filtrar alojamientos por ciudad, fechas, capacidad, tipo, precio y servicios, **para** encontrar opciones que satisfagan mis necesidades.
- **AC1:** Dado que ingreso criterios de búsqueda válidos, cuando busco, entonces se muestran solo anuncios activos que cumplan los filtros implementados.
- **AC2:** Dado que las fechas están completas, cuando se buscan resultados, entonces no se ofrecen como disponibles alojamientos con periodos incompatibles.
- **AC3:** Dado que no hay resultados, cuando termina la búsqueda, entonces se informa de manera clara y puedo ajustar filtros.
- **Dependencias:** HU-04, HU-06, datos de catálogo y definición de filtros.

### HU-08 — Consultar detalle de alojamiento · [MARKET-12](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-12)

- **Épica:** EP-04 · **Requisito:** RF-009 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** huésped, **quiero** consultar el detalle de un alojamiento, **para** comparar sus características antes de cotizar.
- **AC1:** Dado que una publicación está activa, cuando consulto el detalle, entonces veo descripción, ubicación textual, tipo, capacidad, precio, servicios, reglas e imágenes disponibles.
- **AC2:** Dado que el anuncio no está activo, cuando intento acceder por URL directa sin permiso administrativo, entonces el sistema no lo muestra como oferta pública reservable.
- **Dependencias:** HU-04 y catálogo de servicios/reglas.

### HU-09 — Cotización desglosada · [MARKET-13](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-13)

- **Épica:** EP-05 · **Requisito:** RF-010 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** huésped, **quiero** ver el desglose del coste para mis fechas, **para** decidir con información transparente.
- **AC1:** Dado que envío fechas válidas y un alojamiento cotizable, cuando solicito una cotización, entonces se presentan noches, subtotal base, limpieza, tarifa de servicio, descuentos, extras y total.
- **AC2:** Dado que cambian fechas o cantidades de extras, cuando solicito una nueva cotización, entonces los importes se recalculan con la configuración vigente y se evita usar la cifra anterior sin recalcular.
- **AC3:** Dado que una publicación no está disponible, cuando cotizo para el periodo no disponible, entonces el sistema informa el problema y no presenta la cotización como reserva confirmable.
- **Dependencias:** HU-06, HU-07/08 y fórmulas aprobadas.

### HU-10 — Crear reserva inmediata e idempotente · [MARKET-14](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-14)

- **Épica:** EP-05 · **Requisitos:** RF-011, RF-012; reglas RN-008, RN-013, RN-016 · **Prioridad:** Must · **Estimación:** 13 puntos.
- **Como** huésped, **quiero** confirmar una reserva para la cotización elegida, **para** asegurar el alojamiento para mis fechas.
- **AC1:** Dado que solicito reservar un anuncio activo con fechas válidas, cuando el backend procesa la confirmación, entonces revalida disponibilidad y permisos antes de confirmar.
- **AC2:** Dado que otra solicitud acaba de reservar un periodo solapado, cuando mi solicitud intenta confirmar, entonces no se crea una segunda reserva confirmada para el mismo alojamiento y periodo.
- **AC3:** Dado que reintento la misma operación lógica, cuando reutilizo la clave/id de idempotencia, entonces recibo el resultado previo o equivalente sin crear otro registro de reserva.
- **AC4:** Dado que el MVP simula pagos, cuando se ejecuta el flujo de pago, entonces solo usa proveedor/credenciales de prueba y registra el resultado sin realizar cobro real.
- **AC5:** Dado que la reserva se confirma válidamente, cuando finaliza la operación, entonces se conserva el desglose aceptado, se bloquea el periodo y se emiten los eventos/notificaciones correspondientes.
- **Dependencias:** HU-06, HU-09, configuración de pago test y control transaccional.

### HU-11 — Consultar estado e historial de reservas · [MARKET-15](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-15)

- **Épica:** EP-05 · **Requisito:** RF-011 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** huésped, **quiero** consultar estado e historial de mis reservas, **para** hacer seguimiento de mis estancias.
- **AC1:** Dado que tengo sesión, cuando consulto mis reservas, entonces veo solo las relacionadas con mi cuenta y sus estados.
- **AC2:** Dado que una reserva pertenece a otro usuario, cuando intento consultar su detalle sin permiso, entonces se deniega el acceso.
- **Dependencias:** HU-10 y modelo de estados de reserva.

### HU-12 — Cancelar reserva · [MARKET-16](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-16)

- **Épica:** EP-06 · **Requisito:** RF-013 · **Prioridad:** Must · **Estimación:** 8 puntos.
- **Como** huésped, **quiero** cancelar una reserva según la política aplicable, **para** conocer el resultado y la devolución simulada.
- **AC1:** Dado que soy el huésped titular de una reserva cancelable, cuando solicito cancelar e ingreso motivo, entonces el sistema determina la política asociada a la publicación.
- **AC2:** Dado que la política permite cancelar, cuando se procesa la operación, entonces se registra usuario, fecha, motivo, devolución simulada y estado final.
- **AC3:** Dado que la regla de negocio autoriza liberar el periodo, cuando la cancelación se confirma, entonces disponibilidad vuelve a reflejarlo sin afectar otras reservas.
- **AC4:** Dado que la reserva no es cancelable o no soy su titular, cuando solicito cancelar, entonces se rechaza la operación y no se modifica el periodo.
- **Dependencias:** HU-10/11; política aprobada.

### HU-13 — Consultar notificaciones internas · [MARKET-17](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-17)

- **Épica:** EP-07 · **Requisito:** RF-014 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** usuario, **quiero** consultar notificaciones internas sobre eventos relevantes, **para** conocer cambios en mis publicaciones o reservas.
- **AC1:** Dado que ocurre un evento notifiable, cuando la operación finaliza, entonces se registra una notificación para el usuario destinatario correspondiente.
- **AC2:** Dado que inicio sesión, cuando consulto mis notificaciones, entonces solo veo las destinadas a mi cuenta.
- **Dependencias:** eventos de publicación, reserva y cancelación.

### HU-14 — Reseñar estancia completada · [MARKET-18](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-18)

- **Épica:** EP-08 · **Requisito:** RF-015 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** huésped, **quiero** calificar y reseñar el alojamiento después de la estancia, **para** compartir mi experiencia con futuros huéspedes.
- **AC1:** Dado que tengo una reserva completada asociada al alojamiento, cuando envío calificación y contenido válidos, entonces queda una reseña vinculada a huésped, reserva, alojamiento, fecha y contenido.
- **AC2:** Dado que la reserva no está completada o corresponde a otro alojamiento/huésped, cuando intento reseñar, entonces el sistema rechaza la operación.
- **AC3:** Dado que ya se registró una reseña para la reserva, cuando intento crear otra si la regla de unicidad es una por reserva, entonces se evita el duplicado.
- **Dependencias:** HU-10/11 y definición de estado completado.

### HU-15 — Moderar reseñas · [MARKET-19](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-19)

- **Épica:** EP-08/EP-09 · **Requisito:** RF-016 · **Prioridad:** Must · **Estimación:** 3 puntos.
- **Como** administrador, **quiero** ocultar reseñas que incumplen las políticas, **para** mantener la calidad del contenido publicado.
- **AC1:** Dado que soy administrador autorizado, cuando oculto una reseña e indico un motivo, entonces deja de mostrarse públicamente y la acción queda auditada.
- **AC2:** Dado que un usuario sin permiso solicita moderar una reseña, cuando ejecuta la petición, entonces es rechazada.
- **Dependencias:** HU-14, permisos y auditoría.

### HU-16 — Moderar usuarios/publicaciones y consultar auditoría · [MARKET-20](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-20)

- **Épica:** EP-09 · **Requisito:** RF-017 · **Prioridad:** Must · **Estimación:** 8 puntos.
- **Como** administrador, **quiero** restringir usuarios, bloquear publicaciones y consultar el historial de acciones, **para** responder a incumplimientos de la plataforma.
- **AC1:** Dado que tengo rol de administrador, cuando restrinjo una cuenta o bloqueo publicación con motivo, entonces el estado se actualiza y se registra actor, fecha, entidad, motivo y resultado.
- **AC2:** Dado que un anuncio bloqueado tiene reservas futuras, cuando queda bloqueado, entonces esas reservas se conservan y quedan marcadas para revisión manual; no se borran ni cancelan silenciosamente.
- **AC3:** Dado que un usuario no es administrador, cuando intenta una acción administrativa, entonces la operación se rechaza en el backend.
- **Dependencias:** identidad, estados de publicación/reserva y auditoría.

### HU-17 — Consultar dashboard · [MARKET-21](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-21)

- **Épica:** EP-09 · **Requisito:** RF-018 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** administrador/dirección autorizada, **quiero** ver indicadores básicos, **para** entender el estado de la operación y la demanda.
- **AC1:** Dado que tengo permiso, cuando consulto el dashboard, entonces veo usuarios, publicaciones activas, reservas por estado, ingresos estimados y ciudades con mayor demanda.
- **AC2:** Dado que hay un periodo/fecha de corte aplicable, cuando se muestran indicadores, entonces las definiciones de conteo y alcance temporal quedan documentadas y los valores provienen del sistema.
- **AC3:** Dado que no tengo permiso, cuando solicito el dashboard administrativo, entonces el sistema deniega el acceso.
- **Dependencias:** reservas, publicaciones, usuarios y reglas de cómputo de ingresos estimados.

### HU-18 — Exportar reportes · [MARKET-22](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-22)

- **Épica:** EP-09 · **Requisito:** RF-019 · **Prioridad:** Must · **Estimación:** 8 puntos.
- **Como** administrador, **quiero** exportar reportes PDF o Excel, **para** analizar usuarios, publicaciones, reservas, ingresos estimados y demanda.
- **AC1:** Dado que selecciono un reporte autorizado, cuando solicito exportar, entonces se genera PDF o Excel con información consistente con los filtros/datos del sistema.
- **AC2:** Dado que termina una exportación, cuando se registra el resultado, entonces quedan auditados el solicitante y la fecha/hora.
- **AC3:** Dado que la exportación falla, cuando se reporta el error, entonces no se registra como exportación exitosa y se deja evidencia técnica sin filtrar datos sensibles.
- **Dependencias:** dashboard, consultas de datos y librería/formato de exportación por seleccionar.

### HU-19 — Trazas y logs estructurados · [MARKET-23](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-23)

- **Épica:** EP-10 · **Requisito:** RF-020 · **Prioridad:** Must · **Estimación:** 5 puntos.
- **Como** responsable de soporte/administración, **quiero** consultar eventos estructurados relevantes, **para** investigar operaciones e incidentes.
- **AC1:** Dado que ocurre autenticación, publicación, reserva, cancelación, reseña, error o acción administrativa, cuando la operación se ejecuta, entonces se genera un evento con tipo, fecha/hora, resultado y referencia de entidad/actor cuando aplique.
- **AC2:** Dado que se registra un evento, cuando se inspeccionan logs, entonces no aparecen contraseñas, tokens ni credenciales.
- **AC3:** Dado que ocurre un error, cuando se registra, entonces incluye un identificador de correlación o referencia que permita relacionarlo con la solicitud, según diseño aprobado.
- **Dependencias:** convenciones de logging y modelo de auditoría.

## 5.3 Definition of Ready — propuesta

Una historia está lista para sprint solo cuando tiene ID Jira, actor/valor claros, criterios Dado-Cuando-Entonces, prioridad acordada, estimación del equipo, dependencias identificadas, reglas relacionadas, diseño/API afectada identificado y responsable/iteración asignados. Si hay una pregunta abierta que cambia reglas o datos, debe resolverse o registrarse como decisión explícita.

## 5.4 Definition of Done — propuesta

- [ ] Criterios de aceptación demostrados y pruebas correspondientes aprobadas.
- [ ] Código revisado por otro integrante mediante PR; no se integra directamente a rama principal.
- [ ] GitHub Actions y las verificaciones exigidas pasan; análisis SonarCloud completado cuando esté configurado.
- [ ] API/modelos/documentación actualizados; matriz de trazabilidad tiene links reales a Jira, commit, PR y prueba.
- [ ] Seguridad, estados y registros de auditoría verificados donde aplique.
- [ ] Historia aceptada por el rol responsable y asociada a release cuando se entregue.

## 5.5 Sincronización con Jira

Las 10 épicas y 19 historias ya están creadas en Jira MARKET. Los enlaces anteriores y la matriz de trazabilidad apuntan a las claves reales. Jira contiene las descripciones y criterios de aceptación de este documento; las estimaciones se conservan como propuestas en la descripción hasta que el equipo las acuerde. Responsables, estados de trabajo e iteraciones deben actualizarse en Jira cuando se planifique el equipo.
