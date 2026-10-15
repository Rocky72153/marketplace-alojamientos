# 15. Requisitos del negocio

## 15.1 Fuente y propósito

Esta sección deriva los requisitos del negocio del contexto, la visión y los cinco objetivos indicados en la guía oficial del proyecto. Los requisitos de negocio describen resultados que Inversiones LR necesita lograr; no sustituyen los requisitos funcionales (`RF`), las reglas de negocio (`RN`) ni las historias (`HU`).

**Visión del negocio:** ofrecer un Marketplace confiable que conecte anfitriones y huéspedes para descubrir, comparar y reservar alojamientos temporales regionales, con información transparente y un proceso verificable para huéspedes, anfitriones y administradores.

## 15.2 Catálogo de requisitos del negocio

| ID | Requisito del negocio | Justificación en el contexto | Indicador / criterio de éxito propuesto | Trazabilidad funcional inicial | Prioridad |
|---|---|---|---|---|---|
| RBN-001 | Conectar la oferta de alojamientos administrada por anfitriones con la demanda de huéspedes que buscan alojamiento temporal. | La guía indica crecimiento simultáneo de huéspedes y propietarios que entregan la gestión de sus propiedades. | La plataforma permite a anfitriones publicar oferta y a huéspedes consultar las publicaciones activas. Indicador propuesto: publicaciones activas consultables y búsquedas ejecutadas. | RF-004, RF-006, RF-008, RF-009; HU-04, HU-07, HU-08; EP-02, EP-04. | Must |
| RBN-002 | Reducir reservas duplicadas mediante una fuente central de disponibilidad y confirmación consistente. | La guía identifica la prevención de reservas superpuestas como objetivo explícito de negocio. | Ante dos confirmaciones concurrentes de fechas solapadas para el mismo alojamiento, como máximo una puede quedar confirmada. Indicador: conflictos de disponibilidad detectados y pruebas de concurrencia aprobadas. | RF-007, RF-011; RN-008, RN-013; HU-06, HU-10, HU-11; PB-02. | Must |
| RBN-003 | Permitir que los anfitriones publiquen y administren su oferta sin procesos manuales externos al Marketplace. | El crecimiento de propietarios requiere centralizar los datos y las operaciones del anuncio. | El anfitrión autorizado puede crear/editar publicación, precio, servicios, reglas y disponibilidad desde el sistema, con validaciones. Indicador propuesto: porcentaje de publicaciones gestionadas digitalmente. | RF-003 a RF-007; HU-03, HU-04, HU-05, HU-06; EP-01, EP-02, EP-03. | Must |
| RBN-004 | Facilitar el descubrimiento y la comparación de alojamientos con información transparente y disponibilidad relevante. | La visión y el contexto requieren que huéspedes nacionales e internacionales descubran y comparen opciones. | La búsqueda admite ciudad, fechas, capacidad, tipo, rango de precio y servicios; el detalle y la cotización permiten comparar condiciones/costo. | RF-008 a RF-010; HU-07, HU-08, HU-09; EP-04, EP-05. | Must |
| RBN-005 | Gestionar el ciclo de reserva, estado e historial y aplicar cancelaciones conforme a la política asociada a cada publicación. | La plataforma centraliza reservas/cancelaciones y el contexto exige coordinación entre huésped, anfitrión y disponibilidad. | La reserva conserva estado e historial; la cancelación registra actor, fecha, motivo, devolución simulada y estado, y libera fechas cuando corresponde. | RF-011 a RF-014; RN-011 a RN-013; HU-10 a HU-13; EP-05, EP-06, EP-07. | Must |
| RBN-006 | Mantener trazabilidad verificable de publicaciones, reservas, cancelaciones y acciones administrativas. | La guía declara trazabilidad como objetivo de negocio y exige eventos de auditoría/logs estructurados. | Las operaciones relevantes guardan actor cuando aplica, entidad, fecha/hora, acción, resultado y referencia correlacionable, sin registrar credenciales. | RF-017, RF-020; RN-014, RN-015; HU-16, HU-19; EP-09, EP-10. | Must |
| RBN-007 | Permitir a Inversiones LR moderar contenido/cuentas y revisar información operativa para gestionar la plataforma. | La empresa necesita administrar la calidad de publicaciones, usuarios, reseñas y operación. | El administrador autorizado puede restringir usuarios, bloquear publicaciones, moderar reseñas, consultar auditoría y revisar reservas futuras asociadas a un anuncio bloqueado. | RF-016 a RF-018; RN-014, RN-018; HU-05, HU-15, HU-16, HU-17. | Must |
| RBN-008 | Apoyar decisiones de gestión con indicadores y reportes exportables basados en los datos disponibles. | La guía exige dashboard básico de usuarios, anuncios activos, reservas, ingresos estimados y ciudades con mayor demanda, además de exportación PDF/Excel. | Los indicadores se calculan con definiciones transparentes y las exportaciones identifican quién las solicitó y cuándo. | RF-018, RF-019; RN-019; HU-17, HU-18; EP-09. | Must |
| RBN-009 | Evolucionar el producto con cambios trazables, documentación consistente y validación incremental. | La guía exige ingeniería de requerimientos, herramientas ágiles, trazabilidad y flujo de integración continua. | Cada cambio se conecta con requisito/historia, criterios, rama/commit, PR, prueba/evidencia y release; las decisiones de cambios quedan registradas. | RNF-004, RNF-006; `docs/TRACEABILITY_TEMPLATE.csv`; CI y proceso ágil; EP-10. | Must |

**Nota sobre indicadores:** salvo la prevención de reservas solapadas, los indicadores numéricos de negocio no tienen metas cuantitativas en la guía. Se proponen como formas de verificar valor y deben validarse antes de comprometer cifras objetivo. No se inventan metas de rendimiento, conversión o ingresos.

## 15.3 Relación objetivo–requisito–medición

| Objetivo de la guía | Requisitos de negocio que lo satisfacen | Evidencia de aceptación futura |
|---|---|---|
| Conectar oferta y demanda | RBN-001, RBN-003 | Publicación activa aparece en búsqueda y puede consultarse por un huésped. |
| Reducir reservas duplicadas | RBN-002, RBN-005 | Pruebas de solapamiento, concurrencia e idempotencia demuestran que no hay doble confirmación. |
| Administrar oferta sin procesos manuales | RBN-003 | Escenarios de creación/edición, estado y calendario completan su flujo validado. |
| Facilitar descubrimiento y comparación | RBN-004 | Casos de búsqueda, detalle y cotización pasan criterios. |
| Mantener trazabilidad | RBN-006, RBN-009 | Auditoría consultable y matriz con referencias reales de Jira/GitHub/pruebas/release. |
| Moderar y medir la operación | RBN-007, RBN-008 | Acciones administrativas autorizadas, dashboard y reportes verificables. |

## 15.4 Relación con los otros catálogos

- **RBN** expresa el resultado de negocio.
- **RF** expresa la capacidad funcional que contribuye a ese resultado.
- **RN** expresa restricciones/reglas que gobiernan la operación.
- **RNF** expresa atributos de calidad o restricciones técnicas.
- **HU** expresa el valor para un actor y contiene criterios de aceptación en formato Dado/Cuando/Entonces.
- **CP** identifica un caso de prueba diseñado para comprobar uno o más criterios.

La matriz de trazabilidad debe extenderse con `Business_Requirement_ID` para mantener RBN ↔ RF/RNF/RN ↔ HU Jira ↔ criterios ↔ casos de prueba ↔ branch/commit ↔ PR ↔ release.
