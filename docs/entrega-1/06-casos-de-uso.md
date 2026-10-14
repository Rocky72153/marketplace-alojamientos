# 6. Casos de uso

Estos casos describen interacciones funcionales candidatas para el MVP. Las precondiciones, flujos alternos y postcondiciones deberán usarse para derivar pruebas de aceptación.

## CU-01 — Registrarse e iniciar sesión

- **Actor primario:** huésped/anfitrión/administrador.
- **Objetivo:** acceder al marketplace con identidad y permisos correctos.
- **Precondición:** la plataforma está disponible; para iniciar sesión ya existe cuenta activa.
- **Flujo principal:** (1) usuario envía datos; (2) sistema valida campos/credenciales; (3) crea cuenta o sesión; (4) asigna/valida rol; (5) registra evento de auditoría relevante y responde con navegación autorizada.
- **Alternos:** datos inválidos; correo duplicado; credenciales inválidas; cuenta desactivada; rol no permitido. El sistema rechaza la operación y no revela secretos.
- **Postcondición:** cuenta creada o sesión válida; sin estado parcial inseguro.
- **Historias:** HU-01, HU-02, HU-03.

## CU-02 — Crear y publicar alojamiento

- **Actor primario:** anfitrión. **Actor secundario:** administrador si requiere revisión.
- **Objetivo:** ofrecer un alojamiento con información completa.
- **Precondición:** anfitrión autenticado y cuenta habilitada.
- **Flujo principal:** (1) anfitrión diligencia información; (2) sistema valida propiedad y campos; (3) guarda anuncio; (4) ejecuta reglas de estado/revisión; (5) registra evento; (6) si queda activo, permite que aparezca en búsquedas.
- **Alternos:** capacidad/precio inválidos, faltan datos, usuario no propietario, revisión rechazada, anuncio pausado/bloqueado. El anuncio no se publica como reservable.
- **Postcondición:** publicación guardada con un estado permitido y propietario identificable.
- **Historias:** HU-04, HU-05.

## CU-03 — Mantener disponibilidad

- **Actor primario:** anfitrión.
- **Objetivo:** controlar periodos válidos disponibles para sus anuncios.
- **Precondición:** anfitrión autenticado y autorizado sobre la publicación.
- **Flujo principal:** (1) selecciona rango; (2) sistema valida fechas y reservas existentes; (3) guarda el periodo; (4) el calendario actualizado queda disponible para consulta.
- **Alternos:** fechas invertidas, rango que se solapa con reserva confirmada o recurso ajeno; la operación se rechaza.
- **Postcondición:** calendario consistente y trazable.
- **Historias:** HU-06.

## CU-04 — Buscar, comparar y cotizar

- **Actor primario:** huésped.
- **Objetivo:** hallar un alojamiento que cumpla criterios y conocer su coste total.
- **Precondición:** formulario de búsqueda disponible.
- **Flujo principal:** (1) huésped introduce filtros/fechas; (2) sistema valida criterios; (3) consulta anuncios activos; (4) filtra por capacidad, tipo, precio, servicios y disponibilidad; (5) muestra detalle; (6) calcula y presenta desglose de la cotización.
- **Alternos:** no hay resultados, fechas inválidas o disponibilidad cambió. El sistema informa y permite ajustar criterios.
- **Postcondición:** el huésped tiene una cotización informativa, no una reserva todavía.
- **Historias:** HU-07, HU-08, HU-09.

## CU-05 — Confirmar reserva inmediata con pago de prueba

- **Actor primario:** huésped. **Actores secundarios:** servicio de disponibilidad, reservas, proveedor de pagos de prueba, notificaciones.
- **Objetivo:** confirmar una estancia evitando duplicados.
- **Precondición:** huésped autenticado; cotización válida; publicación activa.
- **Flujo principal:** (1) huésped solicita reserva con clave idempotente; (2) backend valida rol, anuncio, fechas, cotización y disponibilidad de nuevo; (3) crea operación transaccional; (4) invoca proveedor de prueba; (5) registra resultado simulado según política acordada; (6) confirma reserva si procede y bloquea periodo; (7) emite auditoría/notificaciones.
- **Alternos:** disponibilidad ocupada, fecha inválida, reintento duplicado, error/fallo del proveedor de pruebas. Se evita crear reservas confirmadas solapadas; resultado y estado deben seguir reglas de recuperación documentadas.
- **Postcondición:** reserva confirmada una sola vez y periodo bloqueado, o respuesta de rechazo/pendiente/fallo persistida sin doble reserva.
- **Historias:** HU-09, HU-10, HU-11, HU-13.

## CU-06 — Cancelar reserva

- **Actor primario:** huésped titular. **Actores secundarios:** servicio de reservas, política de cancelación, disponibilidad y notificaciones.
- **Objetivo:** cancelar conforme a reglas y calcular devolución simulada.
- **Precondición:** reserva pertenece al usuario y estado/política permiten cancelar.
- **Flujo principal:** (1) huésped solicita cancelación y motivo; (2) sistema valida titularidad/estado; (3) consulta política asociada al alojamiento; (4) calcula devolución simulada; (5) registra usuario, fecha, motivo, monto y estado final; (6) libera calendario si aplica; (7) audita y notifica.
- **Alternos:** reserva ajena, estado no cancelable, política no habilita la operación, error transaccional. No se libera disponibilidad si no se confirmó la cancelación.
- **Postcondición:** estado final y cálculo quedan disponibles para consulta.
- **Historias:** HU-12, HU-13.

## CU-07 — Reseñar y moderar estancia

- **Actor primario:** huésped para reseñar; administrador para moderar.
- **Objetivo:** asociar opinión a una estancia completada y aplicar políticas de contenido.
- **Precondición:** la reserva está completada y corresponde al huésped y alojamiento.
- **Flujo principal reseña:** (1) huésped envía calificación/contenido; (2) sistema verifica titularidad, relación y estado; (3) evita duplicado; (4) guarda reseña y la muestra según moderación.
- **Flujo principal moderación:** (1) administrador revisa reseña; (2) marca ocultar con motivo; (3) sistema registra autor/fecha/motivo y la oculta públicamente.
- **Alternos:** reserva incompleta/ajena, reseña repetida, actor sin permisos; se rechaza y no cambia el estado.
- **Historias:** HU-14, HU-15.

## CU-08 — Administrar operación y exportar informe

- **Actor primario:** administrador.
- **Objetivo:** moderar usuarios/publicaciones, inspeccionar eventos y obtener información para gestión.
- **Precondición:** sesión administrativa autorizada.
- **Flujo principal:** (1) administra restricción/bloqueo; (2) sistema aplica reglas y guarda auditoría; (3) consulta dashboard o selecciona reporte; (4) sistema genera PDF/Excel a partir de datos almacenados; (5) registra solicitante y fecha de exportación.
- **Alternos:** permisos insuficientes, filtro inválido, datos no disponibles o fallo de generación; la operación no se registra falsamente como exitosa.
- **Postcondición:** estado/auditoría o reporte coherente disponibles.
- **Historias:** HU-05, HU-16, HU-17, HU-18, HU-19.

## Relaciones entre casos de uso

- CU-04 consulta disponibilidad y reutiliza datos de CU-02/CU-03; cotizar no confirma una reserva.
- CU-05 incluye revalidación de disponibilidad, persistencia idempotente, simulación de pago y notificaciones.
- CU-06 incluye consulta de política de cancelación y, cuando corresponde, liberación de disponibilidad.
- CU-07 depende de CU-05 y de una transición verificable de reserva a completada.
- CU-08 depende de permisos administrativos y de los registros generados por los demás casos.
