# 1. Visión, objetivos y alcance

## 1.1 Contexto

Inversiones LR gestiona propiedades para estancias cortas y largas. El crecimiento de huéspedes y propietarios hace necesario centralizar la oferta, la búsqueda, la disponibilidad, las cotizaciones, reservas y cancelaciones en un marketplace web.

## 1.2 Visión del producto

Ofrecer un marketplace confiable que conecte anfitriones y huéspedes para descubrir, comparar y reservar alojamientos temporales regionales, con información transparente y un proceso verificable para huéspedes, anfitriones y administradores.

## 1.3 Objetivos de negocio

- **OB-01:** conectar la oferta de alojamientos administrada por anfitriones con la demanda de huéspedes.
- **OB-02:** reducir reservas duplicadas mediante una fuente central de disponibilidad.
- **OB-03:** permitir que anfitriones publiquen y administren su oferta sin procesos manuales.
- **OB-04:** facilitar el descubrimiento y comparación mediante filtros, detalles y disponibilidad.
- **OB-05:** mantener trazabilidad de publicaciones, reservas, cancelaciones y acciones administrativas.

## 1.4 Problema y oportunidad

El aumento de oferta y demanda exige coordinar datos de los alojamientos, periodos disponibles, precios y estados de reserva. La oportunidad consiste en establecer una fuente única de información operativa y reglas verificables, evitando confirmar dos reservas solapadas para el mismo alojamiento.

## 1.5 Usuarios y valor esperado

| Segmento | Valor esperado | Indicador propuesto |
|---|---|---|
| Huésped | Descubrir y comparar alojamientos y reservar con una cotización transparente. | Porcentaje de búsquedas que llegan a cotización/reserva; pendiente de definir línea base. |
| Anfitrión | Gestionar publicaciones, precios, servicios, reglas y disponibilidad. | Publicaciones activas y tiempo de publicación; pendiente de medición. |
| Administrador | Moderar contenido y consultar auditoría y reportes. | Eventos administrativos trazables y reportes generados. |
| Inversiones LR | Centralizar operación y medir demanda. | Reservas por estado, ingresos estimados y ciudades con mayor demanda. |

Los indicadores son propuestas para el dashboard; el enunciado requiere indicadores básicos, pero no suministra metas numéricas ni una línea base.

## 1.6 Alcance obligatorio del MVP (MUST)

Para Entrega 1, MUST define el corte funcional mínimo y sus dependencias; SHOULD queda para después de completar el flujo MUST si hay capacidad, y COULD es opcional. La prioridad indica alcance planificado, no implementación existente. Las 19 historias del backlog describen el producto objetivo, pero solo estas 11 historias son puerta de salida del MVP:

1. HU-01: registro/inicio de sesión por rol, rutas protegidas y permisos.
2. HU-04 y HU-05: crear/editar publicaciones, datos mínimos, estados y bloqueo administrativo de publicaciones.
3. HU-06: disponibilidad y bloqueo de periodos confirmados.
4. HU-07 y HU-08: búsqueda, filtros y detalle de alojamientos.
5. HU-09: cotización desglosada.
6. HU-10 y HU-11: reserva inmediata idempotente, pago de prueba y consulta de estado/historial.
7. HU-12: cancelación conforme a la política y liberación autorizada de disponibilidad.
8. HU-19: trazabilidad técnica mínima para operaciones críticas, errores y acciones administrativas.

**SHOULD (HU-02, HU-03, HU-13, HU-14, HU-15, HU-16, HU-17):** recuperación simulada de contraseña, autogestión del perfil/cuenta, notificaciones, reseñas/moderación, restricción de cuentas/consulta administrativa y dashboard. Son capacidades valiosas, pero no bloquean el flujo transaccional mínimo.

**COULD (HU-18):** exportación PDF/Excel, si hay capacidad después de completar MUST y SHOULD prioritarios.

La prioridad, fuente de levantamiento y criterio verificable de cada RF están en [04. Catálogo de requerimientos](04-catalogo-requerimientos.md); la relación con historias, casos de uso, pruebas y evidencias está en la matriz de trazabilidad.

## 1.7 Fuera de alcance o límites explícitos

- WON'T de Entrega 1: procesar pagos reales o usar credenciales de producción. El pago del MVP se simula mediante un proveedor en modo de pruebas.
- No se da por hecho un sistema de chat, mapas, favoritos, MFA, contenerización de la aplicación completa ni despliegue automatizado: son funcionalidades opcionales.
- No se han definido impuestos, moneda alternativa, reglas antifraude, verificación legal de identidad ni políticas legales reales; no deben inventarse como requisitos confirmados.
- El prototipo actual es solamente una interfaz inicial de búsqueda y no constituye una implementación funcional del marketplace.

## 1.8 Restricciones y supuestos

- La arquitectura debe ser orientada a servicios; frontend web y backend con servicios REST consumidos desde el frontend.
- Se requiere modelo de datos y contratos documentados (OpenAPI u equivalente cuando aplique).
- Jira, Confluence, GitHub, GitHub Actions y SonarCloud son herramientas requeridas por la guía.
- La metodología ágil concreta y la organización por sprints deben aprobarse por el equipo.
- **Propuesta técnica inicial, pendiente de acuerdo:** Java 21/Spring Boot, PostgreSQL y frontend estático progresivo. El uso de estas tecnologías no procede del enunciado; es una decisión por validar.

## 1.9 Criterios globales de éxito de la entrega 1

- El sistema debe contar con una especificación en la que actores, procesos, requerimientos, historias y reglas de negocio sean consistentes entre sí.
- El sistema debe tener historias priorizadas con criterios de aceptación verificables y dependencias identificadas.
- El sistema debe estar descrito con modelos coherentes del dominio, los datos y la arquitectura propuesta.
- El sistema debe avanzar a implementación solo con decisiones abiertas documentadas, responsables asignados y aprobaciones requeridas.
- El sistema debe permitir trazar cada requerimiento al backlog, diseño y futura evidencia de código, pruebas y release.
