# API implementada para el MVP

Este inventario describe las rutas del backend actual. La URL base local es `http://localhost:8080`; las rutas incluyen el prefijo `/api`. El [OpenAPI de Entrega 1](entrega-1/10-openapi.yaml) conserva el diseño inicial y contiene operaciones y códigos propuestos que no son equivalentes a la implementación.

Para las operaciones protegidas se envía `Authorization: Bearer <token>`. El servidor agrega `X-Correlation-ID` a las respuestas y lo incluye como `correlationId` en el cuerpo de los errores. Las cuentas inactivas o restringidas no pueden operar con su sesión. El pago y la recuperación de contraseña son simulados.

Las contraseñas de registro deben tener entre 8 y 72 caracteres y ocupar como máximo 72 bytes UTF-8; los caracteres acentuados y otros símbolos pueden ocupar varios bytes.

## Identidad y perfil

| Método y ruta | Uso |
| --- | --- |
| `GET /api/health` | Comprobar disponibilidad del servicio. Público. |
| `POST /api/auth/register` | Registrar huésped o anfitrión y obtener sesión. Administrador requiere la clave de arranque configurada. |
| `POST /api/auth/login` | Obtener una sesión. |
| `POST /api/auth/recovery` | Registrar una solicitud simulada; no envía correo ni cambia la contraseña. |
| `GET`, `PATCH`, `DELETE /api/profile/me` | Consultar, actualizar o desactivar la cuenta autenticada. |

## Alojamientos y disponibilidad

| Método y ruta | Uso |
| --- | --- |
| `GET /api/listings` | Buscar anuncios activos; admite ciudad, fechas, huéspedes, tipo, precios y servicios. Devuelve `items` y `total`, sin paginación. |
| `GET /api/listings/cities?query=...` | Sugerencias públicas de ciudad. |
| `GET /api/listings/suggestions` | Hasta ocho tarjetas públicas; no garantiza disponibilidad para fechas concretas. |
| `GET /api/listings/{id}` | Detalle público de un anuncio disponible; también permite consultar el propio o como administrador. |
| `GET /api/host/listings?page=0&size=20` | Publicaciones propias del anfitrión, paginadas; tamaño máximo 100. |
| `POST /api/listings` | Crear anuncio como anfitrión. |
| `PATCH /api/listings/{id}` | Editar el anuncio propio. |
| `PATCH /api/listings/{id}/state` | Cambiar el estado conforme al rol y las reglas de moderación. |
| `GET /api/listings/{id}/availability` | Consultar períodos; admite `from` y `to`. |
| `PUT /api/listings/{id}/availability` | Reemplazar los períodos editables del anuncio propio con un objeto `{ "periods": [...] }`; la lista es obligatoria, `[]` la vacía y las reservas confirmadas se protegen. |

Los anuncios de un anfitrión inactivo o restringido no aparecen en búsquedas ni admiten reservas nuevas; las reservas futuras confirmadas se conservan y se señalan para revisión administrativa.

## Cotización y reserva

| Método y ruta | Uso |
| --- | --- |
| `POST /api/listings/{id}/quotes` | Cotizar fechas, huéspedes y extras; público y sin crear una reserva. |
| `POST /api/reservations` | Confirmar reserva como huésped con la cabecera obligatoria `Idempotency-Key`. |
| `GET /api/reservations` | Consultar el historial propio. |
| `GET /api/reservations/{id}` | Consultar una reserva propia. |
| `POST /api/reservations/{id}/cancel` | Cancelar una reserva propia con motivo. |

La solicitud de cotización incluye `checkIn`, `checkOut`, `guests` y, opcionalmente, `extras` con `extraId` y `quantity`. La reserva incluye esos mismos datos y `listingId`; no recibe un identificador de cotización. Los montos se calculan de nuevo en el servidor. El pago y cualquier reembolso son simulados.

## Notificaciones, reseñas y administración

| Método y ruta | Uso |
| --- | --- |
| `GET /api/notifications`, `GET /api/notifications/{id}`, `PATCH /api/notifications/{id}/read` | Consultar y marcar las notificaciones propias. |
| `GET /api/listings/{id}/reviews` | Consultar reseñas visibles. Público. |
| `POST /api/listings/{id}/reviews` | Crear una reseña vinculada a una reserva completada. |
| `GET /api/admin/reviews`, `POST /api/admin/reviews/{id}/hide` | Moderar reseñas. Solo administrador. |
| `GET /api/admin/users`, `GET /api/admin/listings`, `POST /api/admin/users/{id}/restrict` | Consultar y moderar cuentas y publicaciones. Solo administrador. |
| `GET /api/admin/audit`, `GET /api/admin/audit/{id}` | Consultar auditoría. Solo administrador. |
| `GET /api/admin/reservations/manual-review`, `POST /api/admin/reservations/{id}/resolve-review` | Consultar y resolver revisiones manuales. Solo administrador. |
| `GET /api/admin/dashboard`, `GET /api/admin/reports` | Consultar indicadores y exportaciones. Solo administrador. |

Los detalles de campos y validaciones se encuentran en los controladores y tipos de solicitud del backend, y su comportamiento se comprueba en las pruebas de aceptación. Este inventario no afirma que exista un servicio de pagos o de correo externo.
