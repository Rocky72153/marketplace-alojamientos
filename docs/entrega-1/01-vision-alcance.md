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

## 1.6 Alcance obligatorio del MVP

1. Registro e inicio de sesión para roles HUÉSPED, ANFITRIÓN y ADMINISTRADOR; protección de rutas y recuperación simulada de contraseña.
2. Consulta y actualización de perfil; activación y desactivación de cuenta.
3. Crear, editar, activar, pausar y consultar publicaciones con título, descripción, ubicación textual, tipo, capacidad, precio por noche, servicios, reglas e imágenes.
4. Administración de periodos disponibles y bloqueo automático de periodos reservados.
5. Búsqueda por ciudad, fechas, capacidad, tipo, rango de precio y servicios; detalle para comparación.
6. Cotización con noches, precio base, limpieza, servicio, descuentos, servicios adicionales y total.
7. Creación consistente de reservas, consulta de estado e historial.
8. Cancelaciones aplicando la política de la publicación y liberando disponibilidad cuando corresponda.
9. Registro y consulta de notificaciones en la plataforma.
10. Reseñas/calificaciones después de una estancia completada, consulta y moderación administrativa.
11. Administración: bloquear publicaciones, restringir usuarios y consultar auditoría.
12. Dashboard básico: usuarios, publicaciones activas, reservas por estado, ingresos estimados y ciudades con mayor demanda.
13. Exportación PDF o Excel de reportes de usuarios, publicaciones, reservas, ingresos estimados o demanda.
14. Logs estructurados de autenticación, publicación, reserva, cancelación, reseñas, errores y acciones administrativas.

## 1.7 Fuera de alcance o límites explícitos

- Pagos reales: prohibidos en el MVP académico; el pago debe simularse mediante un proveedor como Stripe en modo de pruebas.
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

- Actores, procesos, requerimientos, historias y reglas de negocio son consistentes entre sí.
- Cada historia priorizada tiene criterios de aceptación verificables y dependencias identificadas.
- Los modelos explican el dominio, los datos y la arquitectura propuesta.
- Las decisiones abiertas se documentan, se asigna responsable y se obtiene aprobación antes de implementar.
- La trazabilidad permite conectar cada requerimiento con backlog, diseño y futura evidencia de código/pruebas/release.
