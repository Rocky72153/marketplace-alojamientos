# Evidencia de cierre: EP-05 y EP-06

Fecha de verificación: 2 de noviembre de 2026 (America/Bogota).

## Alcance comprobado

| Entrega | Comportamiento verificado | Evidencia automatizada |
|---|---|---|
| EP-05 | Cotización con noches, limpieza, servicio, descuento y extras configurados por el alojamiento. | `ReservationAcceptanceTest.quoteAndReservationAreRecalculatedIdempotentAndPrivate` y `frontend/tests/booking-ui.test.js` |
| EP-05 | Reserva exclusiva para huésped, clave de idempotencia, pago estrictamente simulado y bloqueo de fechas. | `ReservationAcceptanceTest.quoteAndReservationAreRecalculatedIdempotentAndPrivate` |
| EP-05 | Historial y detalle restringidos a la cuenta huésped titular. | `ReservationAcceptanceTest.quoteAndReservationAreRecalculatedIdempotentAndPrivate` y `frontend/tests/reservation-history.test.js` |
| EP-06 | Cancelación con motivo, liberación de calendario y reembolso total, parcial o nulo según la política conservada en la reserva. | `ReservationAcceptanceTest.cancellationAppliesFullRefundAndReleasesTheReservedDates` y `ReservationAcceptanceTest.cancellationUsesPartialAndZeroRefundThresholds` |
| EP-06 | Interfaz de historial con resultado de reembolso y manejo de error al cancelar. | `frontend/tests/reservation-history.test.js` |

## Resultados de la verificación

| Comando | Resultado |
|---|---|
| `mvn -B -f backend/pom.xml verify` | Correcto: 9 pruebas, 0 fallos, 0 errores. |
| `npm run lint` | Correcto. |
| `npm test` | Correcto: 14 pruebas, 0 fallos. Cobertura de líneas del frontend: 89,93%. |
| `npm run build` | Correcto: compilación estática generada. |

## Controles de calidad añadidos

- La clave `Idempotency-Key` se genera con la API criptográfica del navegador; no se usa una fuente pseudoaleatoria.
- La reserva conserva en una estructura única el precio, las fechas, los extras y la política aplicados al confirmar.
- El historial y la cancelación están divididos en funciones pequeñas y cuentan con pruebas tanto de resultado correcto como de error.
- Se eliminaron duplicaciones y advertencias de mantenibilidad detectadas durante la revisión automatizada.

## Trazabilidad de entrega

- PR de EP-05 y EP-06: https://github.com/Rocky72153/marketplace-alojamientos/pull/15
- PR posterior de catálogo colombiano y acceso público a sugerencias: https://github.com/Rocky72153/marketplace-alojamientos/pull/16

La integración final debe conservar la revisión humana de los PR; no se realizó una fusión automática.
