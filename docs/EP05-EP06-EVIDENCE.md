# Evidencia de EP-05 y EP-06

Verificación ejecutada el 10 de noviembre de 2026 (America/Bogota).

## Criterios revisados

| Entrega | Comportamiento | Evidencia |
|---|---|---|
| EP-05 / HU-09 | Cotización de noches, limpieza, servicio, descuento y extras configurados por alojamiento. | `ReservationAcceptanceTest` y `frontend/tests/booking-ui.test.js`; prueba API real sobre PostgreSQL. |
| EP-05 / HU-10 | Reserva exclusiva para huésped, clave de idempotencia, pago simulado, bloqueo de fechas y solicitudes simultáneas. | `ReservationAcceptanceTest`; pruebas concurrentes de idempotencia y flujo API PostgreSQL con cuenta `GUEST`. |
| EP-05 / HU-11 | Historial y detalle privados para el huésped titular. | `ReservationAcceptanceTest` y `frontend/tests/reservation-history.test.js`. |
| EP-06 / HU-12 | Cancelación con motivo, devolución simulada, liberación del calendario y política guardada en la reserva. | `ReservationAcceptanceTest`; flujo API PostgreSQL verificó cancelación y disponibilidad liberada, incluidos los límites de la política. |

## Automatización y verificación

| Comando / verificación | Resultado |
|---|---|
| `mvn -B -ntp -f backend/pom.xml clean verify` | Correcto: 12 pruebas, 0 fallos, 0 errores. Incluye catálogo demo y búsqueda por ciudad sin autenticación. |
| `npm run lint` | Correcto. |
| `npm test` | Correcto: 18 pruebas, 0 fallos. Cobertura de líneas del frontend: 90,27%. |
| `npm run build` | Correcto. |
| GitHub Actions para `develop` en `54b0f9ca2f7288268b2b4ed2a77d16aee3e1c203` | Backend CI, Frontend CI y SonarCloud Code Analysis aprobaron. El Quality Gate no pudo consultar la rama `develop`: SonarCloud respondió que la organización no tiene habilitado el acceso a ramas que no sean la principal. Se ajustó el workflow para ejecutar análisis Sonar en PR y en `main`, no en pushes a `develop`. |

## Verificación con PostgreSQL y catálogo demo

Se usó Docker Desktop 29.9.2 y PostgreSQL 17.11 mediante `docker compose up -d db`. La base recién creada contiene:

- 5.000 alojamientos ficticios y 5.000 periodos de disponibilidad.
- 1.001 cuentas ficticias: 500 huéspedes, 500 anfitriones y 1 administrador.
- 1.122 destinos territoriales colombianos.

El flujo de una cuenta huésped ejecutado contra PostgreSQL fue: iniciar sesión, cotizar dos noches por COP $240.521,80, confirmar la reserva, cancelarla y recibir el 100% de devolución simulada. Tras la cancelación, el API volvió a mostrar el intervalo como `AVAILABLE`.

La búsqueda inicial devolvía cinco alojamientos en 15,95 s porque cargaba todo el catálogo y filtraba en memoria. Se movieron los filtros de ciudad, huéspedes, tipo, rango de precio y servicios a PostgreSQL, se agruparon las consultas de disponibilidad y se agregó un índice para el estado/ciudad/fecha. Después del cambio, en la misma base con 5.000 alojamientos:

- Ciudad `Medellín`: 5 resultados en 843 ms, sin iniciar sesión.
- Ciudad, fechas y capacidad: 3 resultados en 110 ms.
- Ciudad, tipo, capacidad, precio y servicio: 1 resultado.

Son mediciones locales de esta ejecución. Jira MARKET-69 sigue pendiente porque el equipo aún debe acordar objetivos formales de rendimiento y probarlos como criterio de aceptación. Los alojamientos y usuarios de demostración son ficticios, usan correos `.test` y no deben copiarse a producción. Docker Desktop quedó detenido al terminar la verificación.

## Valores aprobados para el MVP

- Limpieza: COP $35.000 por estadía.
- Servicio: 10% del subtotal de noches.
- Descuento predeterminado: 0%.
- Extras: precio configurado por alojamiento.
- Devolución simulada: 100% con 7 o más días; 50% de 2 a 6 días; 0% con menos de 48 horas.

## Trazabilidad de integración

- EP-05/EP-06: [PR #15](https://github.com/Rocky72153/marketplace-alojamientos/pull/15), integrado en `develop`.
- Catálogo colombiano, base de datos demo y búsqueda: [PR #16](https://github.com/Rocky72153/marketplace-alojamientos/pull/16), integrado en la rama de PR #15 y luego en `develop`.
- Se añadieron correcciones para idempotencia concurrente, el umbral de 48 horas y precisión monetaria. También se atendieron observaciones de Sonar sobre complejidad y formato del código. El análisis de código para `develop` terminó correctamente; la organización de SonarCloud no permite consultar un Quality Gate de esa rama. La configuración ya evita ese control inválido y mantiene análisis de PR y `main`.

La evidencia respalda el cierre de EP-05 y EP-06: PR integrados en `develop`, CI y análisis de código aprobados, y pruebas de aceptación ejecutadas. La limitación de rama de SonarCloud queda documentada. MARKET-69 permanece abierto hasta que el equipo acuerde sus objetivos de rendimiento; no se declara completado por las mediciones locales.

## Actualización de dependencias

La migración de Spring Boot 3.5.7 a 4.1.2 actualiza los usos de Jackson y las pruebas de Spring MVC. `mvn -B -ntp -f backend/pom.xml clean verify` pasó con 14 pruebas, incluidas dos que verifican un token con el formato anterior y el rechazo de un contenido JSON inválido firmado.

En PostgreSQL 17 aislado se verificaron registro, cotización, reserva idempotente, cancelación y disponibilidad. Además, se creó una cuenta y una reserva con Spring Boot 3 y se inició Spring Boot 4 sobre esa misma base: la sesión anterior, el inicio de sesión, la consulta y la cancelación de la reserva siguieron funcionando. La prueba utilizó datos ficticios y no sustituye una validación con todos los datos de un entorno real.
