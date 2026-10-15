# 14. Diagramas de secuencia

Los diagramas representan interacciones de diseño propuestas para escenarios críticos de la guía. Son contratos de comportamiento por validar; no demuestran implementación existente.

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

## Criterios de consistencia de los diagramas

- Cada operación de escritura debe validar rol y dueño del recurso en el backend.
- Las transacciones deben conservar un solo resultado válido ante reintentos.
- Los cambios relevantes actualizan auditoría; las notificaciones no deben reemplazar el registro de negocio.
- Los códigos y estructuras de respuesta deben concordar con `10-openapi.yaml` cuando se implemente el contrato.
