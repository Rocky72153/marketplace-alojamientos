# EP-05 y EP-06: cotización, reserva y cancelación

## Valores acordados para el MVP

Juan Sebastián Morales confirmó el 27 de octubre de 2026 esta propuesta para hacer verificables HU-09 y HU-12:

- **Noches:** tarifa por noche × noches de la estadía.
- **Limpieza:** COP $35.000 por estadía.
- **Servicio:** 10% del subtotal de noches.
- **Descuento:** 0% por defecto, configurable; ningún descuento se inventa a partir de datos enviados por el navegador.
- **Extras:** opcionales, configurados en la publicación con precio unitario en COP; el backend valida su identificador y calcula precio unitario × cantidad.
- **Total:** noches + limpieza + servicio + extras − descuento. Se redondea a dos decimales con HALF_UP.
- **Cancelación:** reembolso simulado del 100% cuando faltan 7 o más días; 50% cuando faltan entre 2 y 6 días; 0% cuando faltan menos de 48 horas. No se permite cancelar una estadía que ya inició.

Cada publicación guarda su política de cancelación (por defecto 7 días, 2 días y 50%); un anfitrión puede reemplazarla al crear o editar la publicación. La reserva conserva la tarifa, los extras, el desglose y la política usados al confirmar, de modo que un cambio posterior de la publicación no altere una reserva existente.

La cotización siempre vuelve a consultar disponibilidad y tarifas. Confirmar vuelve a calcular y bloquea el calendario dentro de una transacción; si otra solicitud ocupa las fechas, la operación se rechaza. `Idempotency-Key` evita crear dos reservas ante reintentos y detecta si una clave se reutiliza con datos diferentes.

El pago es siempre una simulación local (`TEST_APPROVED`). No se solicitan ni almacenan tarjetas, no se usa una pasarela y no se hace un cobro real. Una devolución también se registra como simulada.

## Endpoints

| Operación | Endpoint | Acceso |
|---|---|---|
| Cotizar | `POST /api/listings/{listingId}/quotes` | Público; requiere fechas y huéspedes |
| Reservar | `POST /api/reservations` | Token de huésped y encabezado `Idempotency-Key` |
| Historial propio | `GET /api/reservations` | Token de huésped |
| Detalle de reserva propia | `GET /api/reservations/{reservationId}` | Token de huésped; otras cuentas reciben 404 |
| Cancelar | `POST /api/reservations/{reservationId}/cancel` | Token del huésped titular y motivo obligatorio |

Las publicaciones aceptan `extraOptions: [{"name":"Desayuno","unitPrice":20000}]` y una `cancellationPolicy` opcional con `freeRefundDays`, `partialRefundDays` y `partialRefundPercentage`. El API devuelve los identificadores de extras para que la cotización solo pueda seleccionar opciones configuradas por el anfitrión.

## Configuración

El backend usa `CLEANING_FEE_COP` (35000.00), `SERVICE_FEE_RATE` (0.10) y `STAY_DISCOUNT_RATE` (0.00). `PAYMENT_MODE` solo admite `SIMULATED_TEST`; cualquier otro valor bloquea confirmaciones en vez de activar cobros.

## Evidencia automatizada

`ReservationAcceptanceTest` cubre el desglose, cotización en intervalos contiguos, reserva con bloqueo de disponibilidad, reintento idempotente, privacidad del historial, colisión de fechas y reembolsos completo, parcial y cero.
