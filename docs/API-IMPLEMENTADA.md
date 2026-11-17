# API implementada para el MVP

El [contrato OpenAPI vigente](openapi.yaml), en formato OpenAPI 3.1.0, describe las **35 rutas y 42 operaciones** de los controladores Java actuales, con solicitudes, respuestas, validaciones, permisos y errores. La URL base local es `http://localhost:8080`; las rutas incluyen el prefijo `/api`. Este inventario es su guía de lectura. El [OpenAPI de Entrega 1](entrega-1/10-openapi.yaml) se conserva como diseño histórico y no es el contrato de integración actual.

Para las operaciones protegidas se envía `Authorization: Bearer <token>`. El servidor agrega `X-Correlation-ID` a las respuestas y lo incluye como `correlationId` en el cuerpo de los errores. Las cuentas inactivas o restringidas no pueden operar con su sesión. La recuperación de contraseña es simulada. Los pagos usan el simulador interno o Stripe sandbox según la configuración; no hay cobros reales.

Las contraseñas de registro deben tener entre 8 y 72 caracteres y ocupar como máximo 72 bytes UTF-8; los caracteres acentuados y otros símbolos pueden ocupar varios bytes.

El servidor devuelve `200` en las operaciones exitosas, incluidas registro y creación de anuncio o reserva; la creación de reseña devuelve `201`. El webhook devuelve `200` sin cuerpo cuando acepta el evento. Los errores del backend usan `{ "code": "...", "message": "...", "correlationId": "..." }`; el contrato no promete ese formato ni la cabecera de correlación para un rechazo anterior de Nginx.

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
| `POST /api/reservations` | Confirmar reserva en el simulador interno como huésped con `Idempotency-Key`; una reserva nueva en modo Stripe devuelve `409 PAYMENT_MODE_MISMATCH`. |
| `GET /api/reservations` | Consultar el historial propio. |
| `GET /api/reservations/{id}` | Consultar una reserva propia. |
| `POST /api/reservations/{id}/cancel` | Cancelar una reserva propia con motivo. |

La solicitud de cotización incluye `checkIn`, `checkOut`, `guests` y, opcionalmente, `extras` con `extraId` y `quantity`. La reserva incluye esos mismos datos y `listingId`; no recibe un identificador de cotización. Los montos se calculan de nuevo en el servidor.

## Stripe sandbox

| Método y ruta | Uso |
| --- | --- |
| `GET /api/payments/config` | Consultar públicamente el modo de pago, sin credenciales. |
| `POST /api/stripe-test/checkouts` | Crear Checkout como huésped, con el cuerpo de reserva y `Idempotency-Key`. Devuelve `attemptId`, `status`, `checkoutUrl` y `expiresAt`. |
| `GET /api/stripe-test/checkouts/{id}` | Consultar el intento propio autenticado. |
| `POST /api/stripe-test/webhook` | Recibir el cuerpo sin modificar y `Stripe-Signature`; solo en modo Stripe. En la demostración HTTP se entrega por loopback con Stripe CLI y Nginx bloquea la ruta pública. |

La reserva queda pendiente hasta un evento firmado válido. La cancelación de una reserva pagada solicita una devolución de prueba; el historial diferencia reembolso pendiente, confirmado, fallido o en revisión. [Configuración y evidencia](STRIPE-PRUEBAS.md).

Las tres operaciones `/api/stripe-test/...` solo están registradas con `marketplace.payment.mode=STRIPE_TEST`. El modo local predeterminado es `SIMULATED_TEST`; se consulta con `/api/payments/config`. En el webhook, `Stripe-Signature` es obligatoria y se valida sobre el cuerpo original: no es un token de sesión ni una clave privada que el cliente deba enviar. El endpoint público de la demostración HTTP responde `404`; el contrato describe su entrega interna por Stripe CLI a loopback.

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

El [contrato vigente](openapi.yaml) incluye 57 esquemas reutilizables. Los campos y validaciones provienen de los controladores, sus DTO y servicios; las dependencias de bibliotecas y los contratos propuestos que no tienen controlador no añaden rutas. El validador documental comprueba las referencias internas y contrasta las operaciones descritas con los mappings REST del código para detectar rutas omitidas o inventadas.

La integración externa de Stripe fue comprobada en modo de prueba; el envío real de correo continúa fuera del alcance implementado. El contrato no autoriza cobros reales ni sustituye las comprobaciones de rol, propiedad, disponibilidad y estado que realiza el backend.
