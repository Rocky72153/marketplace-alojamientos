# 9. Diseño orientado a servicios

## 9.1 Criterios de diseño

La guía exige una aplicación web, frontend que consume servicios REST del backend, una arquitectura orientada a servicios, contratos (OpenAPI o equivalente), límites de responsabilidad, interacciones, propiedad de datos e integración con el proveedor de pagos simulado. La siguiente descomposición es una **propuesta lógica para validar**, no una lista prescrita por la guía ni una afirmación de que ya existen microservicios desplegados.

Para un proyecto académico, los límites lógicos pueden implementarse inicialmente en una aplicación Spring Boot modular con controladores/servicios/repositorios; separar procesos en microservicios físicos solo se justifica si los requisitos de despliegue/independencia lo demandan y el equipo puede operarlos.

## 9.2 Diagrama de contexto

```mermaid
flowchart LR
  Guest[Huésped] --> Web[Frontend web]
  Host[Anfitrión] --> Web
  Admin[Administrador] --> Web
  Web --> API[API REST Marketplace]
  API --> DB[(Base de datos relacional)]
  API --> Pay[Proveedor de pagos en modo de pruebas]
  API --> Audit[Auditoría y logs]
  API --> Export[Generación de reportes PDF/Excel]
```

## 9.3 Límites lógicos propuestos

| Servicio lógico | Responsabilidad y datos bajo su autoridad | APIs/capacidades | Requisitos/épicas |
|---|---|---|---|
| Identidad y perfiles | Usuarios, roles, sesiones, perfil y estado de cuenta. | Registro, login, recuperación simulada, perfil, activación de cuenta. | RF-001–003 · EP-01 |
| Publicaciones/Catálogo | Publicaciones, anfitrión dueño, servicios, reglas, imágenes y estado. | CRUD de anuncios, activar/pausar, detalle, búsqueda por características. | RF-004–006, RF-008–009 · EP-02/04 |
| Disponibilidad | Periodos disponibles, bloqueados y relacionados con reservas. | Consulta de disponibilidad, mantenimiento de periodos, bloqueo/liberación. | RF-007, reglas de solapamiento · EP-03 |
| Cotización | Componentes de precio, fórmulas configurables y snapshot de cotización. | Cotizar por fechas/personas/extras, explicar desglose, validar vigencia. | RF-010, RN-009 · EP-05 |
| Reservas y cancelaciones | Reservas, estados, clave idempotente y cancelaciones/devoluciones simuladas. | Crear reserva, consultar historial/estado, confirmar, cancelar. | RF-011–013 · EP-05/06 |
| Pagos de prueba | Referencias y resultados del proveedor sandbox/test. | Crear/consultar intento simulado, mapear éxito/fallo técnico. | RF-012, RN-016 · EP-05 |
| Notificaciones | Mensajes internos asociados a eventos y destinatarios. | Registrar/listar/marcar leídas según diseño final. | RF-014 · EP-07 |
| Reseñas y moderación | Reseñas, estado de moderación y vínculos de reserva. | Crear reseña permitida, consultar reseñas, ocultar moderadamente. | RF-015–016 · EP-08/09 |
| Administración/Reportes | Acciones administrativas, consulta de auditoría, métricas y solicitudes de exportación. | Bloquear/restringir, dashboard, PDF/Excel y registro de solicitante/fecha. | RF-017–019 · EP-09 |
| Observabilidad | Convenciones de evento, correlación y logs no sensibles. | Publicar/consultar auditoría/logs según permisos. | RF-020, RNF-004 · EP-10 |

## 9.4 Diagrama lógico de solución

```mermaid
flowchart TB
  subgraph Client[Canal de presentación]
    UI[Frontend web]
  end
  subgraph API[Backend REST — límites lógicos]
    Auth[Identidad / Perfiles]
    Listing[Publicaciones / Catálogo]
    Availability[Disponibilidad]
    Quote[Cotización]
    Booking[Reservas / Cancelaciones]
    Payment[Adaptador de pago de prueba]
    Notify[Notificaciones]
    Review[Reseñas / Moderación]
    Admin[Administración / Reportes]
    Audit[Auditoría / Observabilidad]
  end
  subgraph Data[Persistencia con propiedad lógica por módulo]
    DB[(PostgreSQL — esquema inicial propuesto)]
  end
  TestGateway[Stripe u otro proveedor en modo test]
  UI -->|HTTPS / JSON / REST| Auth
  UI --> Listing
  UI --> Availability
  UI --> Quote
  UI --> Booking
  UI --> Review
  UI --> Admin
  Listing --> DB
  Auth --> DB
  Availability --> DB
  Quote --> DB
  Booking --> Availability
  Booking --> Quote
  Booking --> Payment
  Booking --> Notify
  Payment --> TestGateway
  Booking --> DB
  Notify --> DB
  Review --> DB
  Admin --> DB
  Auth --> Audit
  Listing --> Audit
  Booking --> Audit
  Review --> Audit
  Admin --> Audit
  Audit --> DB
```

La persistencia única dibujada facilita el primer despliegue académico; el ownership de los datos sigue siendo lógico por módulo. Si se decide separación física más adelante, deben revisarse transacciones, consistencia y límites de integración.

## 9.5 Interacciones críticas

### Reserva inmediata

1. Frontend solicita disponibilidad y cotización; el resultado es informativo.
2. Al confirmar, Reservas revalida publicación activa, fechas, precio/snapshot y disponibilidad dentro de una operación consistente.
3. Se asegura idempotencia para la misma solicitud lógica.
4. Se realiza el flujo de pago de prueba según un contrato/adaptador y los estados aprobados.
5. Solo al cumplir la política acordada, la reserva se confirma y el calendario se bloquea.
6. Se persisten eventos de auditoría y notificaciones. Fallo/reintento debe ser recuperable sin duplicar reserva.

### Cancelación

1. Reservas valida titularidad y estado.
2. Consulta la política asociada al anuncio y calcula devolución simulada.
3. Actualiza cancelación/estado y libera disponibilidad solo si la regla lo indica.
4. Audita y notifica la operación.

### Bloqueo administrativo

1. Administración valida rol y motivo.
2. Publicaciones cambia estado a BLOQUEADA y crea evento auditable.
3. Reservas futuras quedan conservadas y marcadas para revisión manual según la guía; no se cancelan o eliminan automáticamente.

## 9.6 Contratos, seguridad y errores

- JSON y REST sobre HTTPS en entornos desplegados; OpenAPI inicial en `10-openapi.yaml`.
- Validación sintáctica y de negocio en backend; control de propietario y rol en servidor.
- Respuestas de error con código HTTP apropiado, `code`, mensaje comprensible y `correlationId` sin filtrar información interna.
- Secretos por variables de entorno/Actions Secrets; nunca versionados.
- Integración de pagos exclusivamente con claves/objetos de prueba. No recibir ni almacenar datos de tarjetas.
- Cambios de estado y seguridad trazables, logs estructurados sin contraseñas/tokens.

## 9.7 Mapeo requerimiento–servicio

La matriz completa debe mantenerse en Jira/Confluence y `docs/TRACEABILITY_TEMPLATE.csv`. Como mínimo, cada servicio lógico se relaciona con sus RF/ RN, procesos PB, historias HU, contrato API, propietario lógico de los datos y pruebas end-to-end. Antes de implementación, el equipo debe acordar si los servicios son módulos del mismo backend o despliegues independientes.
