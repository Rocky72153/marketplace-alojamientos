# 14. Diagramas de secuencia

SD-01 a SD-03 representan interacciones de diseño propuestas en la Entrega 1 para escenarios críticos de la guía. SD-04 describe el flujo de pago implementado en `main`.

## SD-01 — Confirmar una reserva

```mermaid
sequenceDiagram
    actor H as Huésped
    participant FE as Frontend
    participant API as API REST
    participant L as Publicaciones
    participant D as Disponibilidad
    participant Q as Cotización
    participant R as Reservas
    participant P as Proveedor de pago test
    participant A as Auditoría/Notificaciones
    H->>FE: Selecciona alojamiento, fechas y confirma
    FE->>API: POST /bookings + Idempotency-Key + quoteId
    API->>L: Validar estado ACTIVA
    API->>Q: Validar cotización y desglose aceptado
    API->>D: Revalidar disponibilidad al confirmar
    D-->>API: Disponible / conflicto
    alt No disponible o anuncio inválido
      API-->>FE: 409 conflicto, sin reserva confirmada
    else Disponible
      API->>R: Crear reserva idempotente/transaccional
      R->>P: Simular pago de prueba
      P-->>R: Estado del intento de pago test
      alt Resultado que permite confirmar
        R->>D: Bloquear periodo de forma consistente
        R->>A: Emitir auditoría y notificaciones
        API-->>FE: Reserva y estado confirmados
      else Fallo o respuesta incierta
        R->>A: Registrar error/estado de recuperación
        API-->>FE: Estado no confirmado o pendiente según política
      end
    end
```

**Controles importantes:** no confiar solo en disponibilidad consultada durante búsqueda; validar en el punto de confirmación. La clave idempotente se guarda bajo una restricción de unicidad para evitar duplicados. La estrategia de compensación frente a fallo del proveedor debe quedar especificada por el equipo.

## SD-02 — Cancelar una reserva

```mermaid
sequenceDiagram
    actor H as Huésped
    participant FE as Frontend
    participant API as API REST
    participant R as Reservas/Cancelaciones
    participant P as Política de cancelación
    participant D as Disponibilidad
    participant A as Auditoría/Notificaciones
    H->>FE: Solicita cancelación y escribe motivo
    FE->>API: POST /bookings/{id}/cancel
    API->>R: Validar titularidad y estado
    R->>P: Obtener política vinculada al anuncio
    P-->>R: Regla vigente
    R->>R: Calcular devolución simulada y estado final
    alt Cancelación permitida
      R->>D: Liberar periodo solo si aplica
      R->>A: Registrar quién/cuándo/motivo/valor/resultado
      R->>A: Crear notificaciones pertinentes
      API-->>FE: Resultado de cancelación
    else No permitida
      R->>A: Registrar evento relevante de rechazo
      API-->>FE: 409, no se modifica la reserva
    end
```

## SD-03 — Bloquear publicación por administrador

```mermaid
sequenceDiagram
    actor AD as Administrador
    participant FE as Frontend
    participant API as API REST
    participant L as Publicaciones
    participant R as Reservas
    participant A as Auditoría

    AD->>FE: Selecciona la publicación e indica el motivo
    FE->>API: PATCH /listings/{id}/state
    Note over FE,API: Estado solicitado: BLOQUEADA
    API->>API: Validar rol y permisos
    API->>L: Cambiar estado de la publicación a BLOQUEADA
    L->>R: Marcar reservas futuras para revisión manual
    Note over L,R: Las reservas futuras se conservan y no se borran ni cancelan automáticamente
    L->>A: Registrar actor, entidad, fecha, motivo y resultado
    API-->>FE: Devolver estado y confirmación trazable
    FE-->>AD: Mostrar confirmación del bloqueo
```

## SD-04: Reservar y pagar con Stripe en modo de prueba (implementado)

A diferencia de SD-01 a SD-03, que son de diseño, este diagrama sale del código en `main`: `StripeCheckoutController`, `StripeCheckoutTransactions`, `StripeEventTransactions`, `StripeCheckoutEventDecision` y `StripeSdkTestGateway`. Aplica cuando `PAYMENT_MODE=STRIPE_TEST`, que es el modo del entorno publicado.

![SD-04 Reservar y pagar con Stripe](diagramas/14-sd04-pago-stripe.png)

```mermaid
sequenceDiagram
    autonumber
    actor H as Huésped
    participant W as Aplicación web
    participant C as StripeCheckoutController
    participant T as StripeCheckoutTransactions
    participant L as ListingService
    participant DB as PostgreSQL
    participant G as StripeSdkTestGateway
    participant S as Stripe (sandbox)
    participant CLI as Stripe CLI
    participant E as StripeEventTransactions

    H->>W: Confirma la reserva cotizada
    W->>C: POST /api/stripe-test/checkouts (JWT, Idempotency-Key)
    C->>T: prepare(huésped, solicitud, clave)
    T->>L: confirmReservation(fechas)
    L->>DB: Bloquear periodo (RESERVED)
    T->>DB: Reserva PENDING_PAYMENT e intento PREPARING
    T-->>C: intento PREPARING
    C->>G: createCheckout(intento, importe, vencimiento)
    G->>S: Crear sesión de Checkout (HTTPS)
    S-->>G: sesión cs_test y URL
    C->>T: bind(intento, sesión)
    T->>DB: Intento OPEN
    C-->>W: 200 {attemptId, status OPEN, checkoutUrl}
    W->>H: Redirige a Stripe Checkout
    H->>S: Paga con tarjeta de prueba
    S-->>CLI: Evento checkout.session.completed
    CLI->>C: POST /api/stripe-test/webhook (Stripe-Signature)
    C->>G: verifyWebhook(payload, firma)
    G-->>C: evento verificado
    C->>E: process(evento)
    E->>DB: Bloquear intento y comprobar si el evento ya se procesó
    alt Pagado dentro del plazo, cuenta y publicación activas
        E->>DB: Reserva CONFIRMED, intento PAID
        E->>DB: Notificación y auditoría STRIPE_TEST_PAYMENT_CONFIRMED
    else Fuera de plazo o cuenta/publicación no activa
        E->>L: releaseReservation (libera fechas)
        E->>DB: Reserva CANCELLED (TEST_EXPIRED), intento REFUND_PENDING
    end
    E->>DB: Guardar id del evento (idempotencia)
    C-->>CLI: 200
    S-->>H: Vuelve a la aplicación
    W->>C: GET /api/stripe-test/checkouts/{id}
    C-->>W: status PAID
    W-->>H: Reserva confirmada
```

Notas:

- La reserva y el bloqueo de fechas ocurren antes de ir a Stripe, en una sola transacción; así nadie más puede reservar esas fechas mientras el huésped paga.
- Las llamadas a Stripe se hacen fuera de los bloqueos de base de datos.
- Si el evento llega antes de que se guarde la sesión, el backend responde con reintento; si se pierde la respuesta de creación, recupera la sesión por su id firmado.
- `StripeCheckoutMaintenance` revisa cada minuto los intentos abiertos y vence los que pasaron su plazo.

## Criterios de consistencia de los diagramas

- Cada operación de escritura debe validar rol y dueño del recurso en el backend.
- Las transacciones deben conservar un solo resultado válido ante reintentos.
- Los cambios relevantes actualizan auditoría; las notificaciones no deben reemplazar el registro de negocio.
- Los códigos y estructuras de respuesta deben concordar con `10-openapi.yaml` cuando se implemente el contrato.
