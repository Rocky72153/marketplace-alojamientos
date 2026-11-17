# 14. Diagramas de secuencia

SD-01 a SD-03 conservan el diseño histórico de la Entrega 1 para escenarios críticos de la guía; sus rutas y pasos conceptuales no sustituyen el contrato de la implementación actual. SD-04 describe el flujo de pago implementado en `main`.

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

A diferencia de SD-01 a SD-03, que son de diseño histórico, este diagrama sale del código en `main`: `StripeCheckoutController`, `StripeCheckoutTransactions`, `StripeEventTransactions`, `StripeCheckoutEventDecision`, `StripeSdkTestGateway` y `frontend/src/stripe-checkout.js`. Aplica cuando `PAYMENT_MODE=STRIPE_TEST`, que es el modo del entorno publicado. La creación de sesión muestra el caso inicial correcto; el procesamiento distingue eventos pagados, pendientes, vencidos y repetidos.

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
    H->>S: Completa Checkout con datos de prueba
    S-->>CLI: Evento de sesión (completed, async o expired)
    CLI->>C: POST /api/stripe-test/webhook (Stripe-Signature)
    C->>G: verifyWebhook(payload, firma)
    G-->>C: evento verificado
    C->>E: process(evento)
    E->>DB: Bloquear intento, comprobar duplicado y clasificar evento
    alt Evento ya procesado o estado terminal compatible
        Note over DB,E: Conservar estado, sin confirmar ni cobrar otra vez
    else completed con unpaid e intento OPEN
        Note over DB,E: Acción WAIT_FOR_PAYMENT, sin cambio de estado<br/>Reserva PENDING_PAYMENT, intento OPEN<br/>Esperar async_payment_succeeded o async_payment_failed
    else Evento paid e intento OPEN
        alt Dentro del plazo, huésped y anfitrión habilitados, publicación ACTIVE
            E->>DB: Reserva CONFIRMED, intento PAID
            E->>DB: Notificación y auditoría STRIPE_TEST_PAYMENT_CONFIRMED
        else Plazo vencido, cuenta restringida o publicación no activa
            E->>L: releaseReservation (libera fechas)
            E->>DB: Reserva CANCELLED, intento REFUND_PENDING
            E->>DB: Auditoría STRIPE_TEST_REFUND_REQUIRED
        end
    else Evento paid e intento EXPIRED
        E->>DB: Reserva sigue CANCELLED, intento REFUND_PENDING
        Note over DB,E: Pago tardío, no reabrir las fechas ni confirmar la reserva
    else expired o async_payment_failed con unpaid e intento OPEN
        E->>L: releaseReservation (libera fechas)
        E->>DB: Reserva CANCELLED, intento EXPIRED
    end
    E->>DB: Guardar id si es un evento nuevo aceptado (idempotencia)
    C-->>CLI: 200 si el procesamiento terminó sin error
    Note over C,E: Evento inválido: 400. Falta de vínculo o condición transitoria: 503<br/>No guardar el evento como procesado si la transacción falla
    Note over H,S: El regreso del navegador puede ocurrir antes o después del webhook
    S-->>H: Vuelve a la aplicación (no acredita el pago)
    W->>C: GET /api/stripe-test/checkouts/{id} (JWT del titular)
    C-->>W: Estado actual del intento guardado en PostgreSQL
    alt status PAID
        W-->>H: Pago de prueba y reserva confirmados
    else status PREPARING u OPEN
        W-->>H: Pago pendiente, consultar de nuevo sin confirmar la reserva
    else status EXPIRED o CANCELLED
        W-->>H: Pago vencido o reserva cancelada, sin confirmación
    else status REFUND_PENDING, REFUND_FAILED, REFUND_REVIEW o REFUNDED
        W-->>H: Mostrar estado del reembolso y consultar historial
    end
```

Notas:

- La reserva y el bloqueo de fechas ocurren antes de ir a Stripe, en una sola transacción; así nadie más puede reservar esas fechas mientras el huésped paga.
- Las llamadas a Stripe se hacen fuera de los bloqueos de base de datos.
- Si falta el vínculo con la sesión, el controlador intenta recuperarla mediante el id del evento firmado y la verifica con Stripe. Si el intento aún no puede procesarse, responde `503` para reintento sin confiar en una sesión no vinculada.
- Los eventos admitidos son `checkout.session.completed`, `checkout.session.async_payment_succeeded`, `checkout.session.async_payment_failed` y `checkout.session.expired`. `completed` con `payment_status=unpaid` produce la **acción** `WAIT_FOR_PAYMENT`: no es un estado nuevo de la base de datos; el intento sigue `OPEN` y la reserva sigue `PENDING_PAYMENT` hasta otro evento o el vencimiento.
- Solo un evento `paid` válido sobre un intento `OPEN`, con plazo vigente, reserva `PENDING_PAYMENT`, huésped y anfitrión habilitados y sin restricción, y publicación `ACTIVE`, puede dejar el intento `PAID` y la reserva `CONFIRMED`. Si falla esa validación de vigencia, se liberan las fechas, la reserva queda `CANCELLED` y el intento `REFUND_PENDING`. Un pago tardío sobre `EXPIRED` también solicita reembolso sin confirmar ni recuperar las fechas.
- Los eventos repetidos y los ya compatibles con un estado terminal no confirman de nuevo. El id del evento nuevo se guarda en la misma transacción que sus efectos; un error no se marca como procesado. Un evento inválido se rechaza y una condición transitoria devuelve `503` para reintento.
- El retorno desde Stripe puede preceder al webhook. La web consulta con la cuenta titular el estado guardado por el servidor; solo `PAID` presenta confirmación. `PREPARING`/`OPEN`, `EXPIRED`/`CANCELLED` y los estados de reembolso muestran mensajes distintos, sin convertirlos en una reserva confirmada por el mero regreso del navegador.
- `StripeCheckoutMaintenance` revisa cada minuto los intentos abiertos y vence los que pasaron su plazo; también concilia los reembolsos pendientes. Su resultado se refleja como `REFUNDED`, `REFUND_FAILED` o `REFUND_REVIEW`, según la respuesta o la incertidumbre del proveedor. No se presenta un reembolso pendiente como ejecutado.

## Criterios de consistencia de los diagramas

- Cada operación de escritura debe validar rol y dueño del recurso en el backend.
- Las transacciones deben conservar un solo resultado válido ante reintentos.
- Los cambios relevantes actualizan auditoría; las notificaciones no deben reemplazar el registro de negocio.
- SD-01 a SD-03 conservan el contrato propuesto en `10-openapi.yaml`; para las rutas y respuestas vigentes de SD-04 se consulta [el contrato implementado](../openapi.yaml) y [la referencia de la API actual](../API-IMPLEMENTADA.md).
