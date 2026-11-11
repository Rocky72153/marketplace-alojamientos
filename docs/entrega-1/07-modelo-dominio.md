# 7. Modelo de dominio

El siguiente modelo expresa las entidades y asociaciones principales deducidas del glosario y reglas del enunciado. Los tipos/campos son una propuesta conceptual y deben ajustarse al modelo de datos físico y a los servicios finalmente aprobados.

```mermaid
classDiagram
    class Usuario {
      +UUID id
      +String nombre
      +String correo
      +String credencialHash
      +Rol rol
      +EstadoCuenta estado
      +Instant creadoEn
    }
    class Perfil {
      +UUID usuarioId
      +String telefono
      +String fotoReferencia
    }
    class Alojamiento {
      +UUID id
      +UUID anfitrionId
      +String titulo
      +String descripcion
      +String ubicacionTextual
      +TipoAlojamiento tipo
      +int capacidad
      +BigDecimal precioNoche
      +EstadoPublicacion estado
    }
    class ServicioAlojamiento {
      +UUID alojamientoId
      +UUID servicioId
    }
    class ServicioCatalogo {
      +UUID id
      +String nombre
      +String descripcion
    }
    class ReglaAlojamiento {
      +UUID id
      +UUID alojamientoId
      +String descripcion
    }
    class ImagenAlojamiento {
      +UUID id
      +UUID alojamientoId
      +String referencia
      +int orden
    }
    class PeriodoDisponibilidad {
      +UUID id
      +UUID alojamientoId
      +LocalDate llegada
      +LocalDate salida
      +TipoPeriodo tipo
    }
    class PoliticaCancelacion {
      +UUID id
      +String nombre
      +String regla
      +boolean activa
    }
    class PublicacionPolitica {
      +UUID alojamientoId
      +UUID politicaId
    }
    class Cotizacion {
      +UUID id
      +UUID huespedId
      +UUID alojamientoId
      +LocalDate llegada
      +LocalDate salida
      +int noches
      +BigDecimal subtotalBase
      +BigDecimal tarifaLimpieza
      +BigDecimal tarifaServicio
      +BigDecimal descuentos
      +BigDecimal extras
      +BigDecimal total
      +Instant calculadaEn
    }
    class Reserva {
      +UUID id
      +UUID huespedId
      +UUID alojamientoId
      +UUID cotizacionId
      +LocalDate llegada
      +LocalDate salida
      +EstadoReserva estado
      +String claveIdempotencia
      +Instant creadaEn
      +Instant confirmadaEn
    }
    class PagoSimulado {
      +UUID id
      +UUID reservaId
      +String proveedor
      +String referenciaExterna
      +EstadoPago estado
      +Instant procesadoEn
    }
    class Cancelacion {
      +UUID id
      +UUID reservaId
      +UUID solicitanteId
      +String motivo
      +BigDecimal devolucionSimulada
      +EstadoCancelacion estadoFinal
      +Instant solicitadaEn
    }
    class Notificacion {
      +UUID id
      +UUID usuarioId
      +String tipo
      +String mensaje
      +Instant creadaEn
      +Instant leidaEn
    }
    class Resena {
      +UUID id
      +UUID reservaId
      +UUID huespedId
      +UUID alojamientoId
      +int calificacion
      +String contenido
      +EstadoResena estado
      +Instant creadaEn
    }
    class EventoAuditoria {
      +UUID id
      +UUID actorId
      +String tipoEvento
      +String entidadTipo
      +UUID entidadId
      +String resultado
      +String motivo
      +Instant creadoEn
      +String correlationId
    }
    class ExportacionReporte {
      +UUID id
      +UUID solicitanteId
      +String tipoReporte
      +String formato
      +String parametros
      +String estado
      +Instant solicitadaEn
    }
    Usuario "1" --> "0..1" Perfil
    Usuario "1" --> "0..*" Alojamiento : anfitrion
    Alojamiento "1" --> "0..*" ImagenAlojamiento
    Alojamiento "1" --> "0..*" ReglaAlojamiento
    Alojamiento "1" --> "0..*" ServicioAlojamiento
    ServicioCatalogo "1" --> "0..*" ServicioAlojamiento
    Alojamiento "1" --> "0..*" PeriodoDisponibilidad
    Alojamiento "1" --> "1..*" PublicacionPolitica
    PoliticaCancelacion "1" --> "0..*" PublicacionPolitica
    Usuario "1" --> "0..*" Cotizacion : huesped
    Alojamiento "1" --> "0..*" Cotizacion
    Cotizacion "1" --> "0..1" Reserva
    Usuario "1" --> "0..*" Reserva : huesped
    Alojamiento "1" --> "0..*" Reserva
    Reserva "1" --> "0..*" PagoSimulado
    Reserva "1" --> "0..1" Cancelacion
    Reserva "1" --> "0..1" Resena
    Usuario "1" --> "0..*" Resena : huesped
    Alojamiento "1" --> "0..*" Resena
    Usuario "1" --> "0..*" Notificacion
    Usuario "0..1" --> "0..*" EventoAuditoria : actor
    Usuario "1" --> "0..*" ExportacionReporte : solicitante
```

## Conceptos y restricciones

- **Usuario/Rol:** roles de negocio HUÉSPED, ANFITRIÓN y ADMINISTRADOR. A nivel de autorización, validar roles y propiedad del recurso en backend; no depender solo de la interfaz.
- **Alojamiento/estado:** el anuncio no es el inmueble físico por sí solo, sino su representación dentro del marketplace. Tiene un único anfitrión y los estados descritos en el glosario.
- **Servicio (amenity):** facilidad como Wi-Fi o cocina. No confundir con **servicio de software**, que se describe en la arquitectura.
- **Disponibilidad:** representar intervalos de fecha sin permitir nuevos periodos que contradigan reservas confirmadas. Propuesta para fechas: llegada incluida y salida excluida; se debe confirmar como decisión de negocio.
- **Cotización:** almacenar el desglose que se mostró y aceptó, evitando que futuros cambios de tarifa reescriban el histórico de una reserva ya creada.
- **Reserva:** enlaza huésped, alojamiento y periodo. Una restricción de solapamiento efectiva requiere control en la base de datos o transacción equivalente, no solo una validación de frontend.
- **PagoSimulado:** evidencia del resultado de prueba; no es transacción monetaria real.
- **Auditoría vs logs:** auditoría es registro de negocio consultable de acciones con actor/entidad/resultado; los logs técnicos sirven para diagnóstico y deben excluir secretos.
- **Reseña:** solo tras estancia completada, vinculada a la reserva y al alojamiento evaluado. La unicidad (una reseña por reserva) se propone como regla coherente a confirmar.

## Enumeraciones propuestas

| Enumeración | Valores iniciales |
|---|---|
| Rol | HUESPED, ANFITRION, ADMINISTRADOR |
| EstadoCuenta | ACTIVA, DESACTIVADA, RESTRINGIDA |
| EstadoPublicacion | BORRADOR, PENDIENTE_REVISION, ACTIVA, PAUSADA, BLOQUEADA |
| EstadoReserva | SOLICITADA, CONFIRMADA, CANCELADA, COMPLETADA, RECHAZADA, PENDIENTE_PAGO, PAGO_FALLIDO (validar transiciones) |
| EstadoPago | INICIADO, APROBADO_TEST, RECHAZADO_TEST, ERROR_TECNICO |
| EstadoResena | PUBLICADA, OCULTA_POR_MODERACION |
| TipoPeriodo | DISPONIBLE, BLOQUEADO, RESERVADO |

Los estados adicionales de reserva/pago son candidatos de diseño, no una enumeración impuesta literalmente por la guía. El equipo debe aprobar las transiciones y casos de error.
