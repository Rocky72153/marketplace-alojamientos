# Evidencia actual de EP-05 y EP-06

Verificación ejecutada el 10 de noviembre de 2026 (America/Bogota).

## Criterios revisados

| Entrega | Comportamiento | Evidencia |
|---|---|---|
| EP-05 / HU-09 | Cotización de noches, limpieza, servicio, descuento y extras configurados por alojamiento. | `ReservationAcceptanceTest` y `frontend/tests/booking-ui.test.js`; prueba API real sobre PostgreSQL. |
| EP-05 / HU-10 | Reserva exclusiva para huésped, clave de idempotencia, pago simulado y bloqueo de fechas. | `ReservationAcceptanceTest`; flujo API PostgreSQL con cuenta `GUEST`. |
| EP-05 / HU-11 | Historial y detalle privados para el huésped titular. | `ReservationAcceptanceTest` y `frontend/tests/reservation-history.test.js`. |
| EP-06 / HU-12 | Cancelación con motivo, devolución simulada, liberación del calendario y política guardada en la reserva. | `ReservationAcceptanceTest`; flujo API PostgreSQL verificó cancelación y disponibilidad liberada. |

## Automatización

| Comando | Resultado |
|---|---|
| `mvn -B -ntp -f backend/pom.xml verify` | Correcto: 10 pruebas, 0 fallos, 0 errores. Incluye catálogo demo y búsqueda por ciudad sin autenticación. |
| `npm run lint` | Correcto. |
| `npm test` | Correcto: 18 pruebas, 0 fallos. Cobertura de líneas del frontend: 90,12%. |
| `npm run build` | Correcto. |

## Verificación con PostgreSQL y catálogo demo

Se inició Docker Desktop 29.9.2 y PostgreSQL 17.11 mediante `docker compose up -d db`. El backend arrancó con el perfil local `demo` y se conectó a esa base. La base recién creada contiene:

- 5.000 alojamientos ficticios y 5.000 periodos de disponibilidad.
- 1.001 cuentas ficticias: 500 huéspedes, 500 anfitriones y 1 administrador.
- 1.122 destinos territoriales colombianos.

El flujo de una cuenta huésped ejecutado contra PostgreSQL fue: iniciar sesión, cotizar dos noches por COP $240.521,80, confirmar la reserva, cancelarla y recibir el 100% de devolución simulada. Tras la cancelación, el API volvió a mostrar el intervalo como `AVAILABLE`.

La búsqueda inicial devolvía cinco alojamientos en 15,95 s porque cargaba todo el catálogo y filtraba en memoria. Se movieron los filtros de ciudad, huéspedes, tipo, rango de precio y servicios a PostgreSQL, se agruparon las consultas de disponibilidad y se agregó un índice para el estado/ciudad/fecha. Después del cambio, en la misma base con 5.000 alojamientos:

- Ciudad `Medellín`: 5 resultados en 843 ms, sin iniciar sesión.
- Ciudad, fechas y capacidad: 3 resultados en 110 ms.
- Ciudad, tipo, capacidad, precio y servicio: 1 resultado.

Son mediciones locales de esta ejecución. Jira MARKET-69 sigue pendiente porque el equipo aún debe acordar objetivos formales de rendimiento y probarlos como criterio de aceptación.

## Valores aprobados para el MVP

- Limpieza: COP $35.000 por estadía.
- Servicio: 10% del subtotal de noches.
- Descuento predeterminado: 0%.
- Extras: precio configurado por alojamiento.
- Devolución simulada: 100% con 7 o más días; 50% de 2 a 6 días; 0% con menos de 48 horas.

## Trazabilidad de integración

- EP-05/EP-06: [PR #15](https://github.com/Rocky72153/marketplace-alojamientos/pull/15), abierto contra `develop`; checks reportados como correctos, aún sin revisión.
- Catálogo y búsqueda: [PR #16](https://github.com/Rocky72153/marketplace-alojamientos/pull/16), abierto y apilado sobre PR #15; requiere integrar primero PR #15. El commit de cabeza de PR #16 no tenía checks al momento de esta verificación.

Las historias principales siguen en progreso hasta que los PR sean revisados e integrados. Los alojamientos y cuentas del perfil demo son ficticios, usan correos `.test` y no deben copiarse a producción.
