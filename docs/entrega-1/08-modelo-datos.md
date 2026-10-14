# 8. Modelo lógico de datos

El diagrama propone un modelo relacional conceptual compatible con PostgreSQL. Los nombres finales, tipos y normalización deben validarse frente a los servicios y el diseño físico. `UUID` y `TIMESTAMPTZ` son propuestas técnicas, no restricciones del enunciado.

```mermaid
erDiagram
    USUARIO ||--o| PERFIL : tiene
    USUARIO ||--o{ ALOJAMIENTO : administra
    ALOJAMIENTO ||--o{ ALOJAMIENTO_IMAGEN : incluye
    ALOJAMIENTO ||--o{ ALOJAMIENTO_REGLA : define
    ALOJAMIENTO ||--o{ ALOJAMIENTO_SERVICIO : ofrece
    SERVICIO_CATALOGO ||--o{ ALOJAMIENTO_SERVICIO : clasifica
    ALOJAMIENTO ||--o{ PERIODO_DISPONIBILIDAD : configura
    ALOJAMIENTO ||--|{ ALOJAMIENTO_POLITICA : asocia
    POLITICA_CANCELACION ||--o{ ALOJAMIENTO_POLITICA : aplica
    USUARIO ||--o{ COTIZACION : solicita
    ALOJAMIENTO ||--o{ COTIZACION : cotizado
    COTIZACION ||--o| RESERVA : origina
    USUARIO ||--o{ RESERVA : realiza
    ALOJAMIENTO ||--o{ RESERVA : reservado
    RESERVA ||--o{ PAGO_SIMULADO : registra
    RESERVA ||--o| CANCELACION : puede_tener
    RESERVA ||--o| RESENA : habilita
    USUARIO ||--o{ RESENA : escribe
    ALOJAMIENTO ||--o{ RESENA : recibe
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO o|--o{ EVENTO_AUDITORIA : ejecuta
    USUARIO ||--o{ EXPORTACION_REPORTE : solicita

    USUARIO {
      uuid id PK
      varchar nombre
      varchar correo UK
      varchar credencial_hash
      varchar rol
      varchar estado
      timestamptz creado_en
      timestamptz actualizado_en
    }
    PERFIL {
      uuid usuario_id PK,FK
      varchar telefono
      varchar foto_referencia
    }
    ALOJAMIENTO {
      uuid id PK
      uuid anfitrion_id FK
      varchar titulo
      text descripcion
      varchar ubicacion_textual
      varchar tipo
      int capacidad
      numeric precio_noche
      varchar estado
      timestamptz creado_en
      timestamptz actualizado_en
    }
    ALOJAMIENTO_IMAGEN {
      uuid id PK
      uuid alojamiento_id FK
      varchar referencia
      int orden
    }
    ALOJAMIENTO_REGLA {
      uuid id PK
      uuid alojamiento_id FK
      text descripcion
    }
    SERVICIO_CATALOGO {
      uuid id PK
      varchar nombre UK
      text descripcion
    }
    ALOJAMIENTO_SERVICIO {
      uuid alojamiento_id PK,FK
      uuid servicio_id PK,FK
    }
    PERIODO_DISPONIBILIDAD {
      uuid id PK
      uuid alojamiento_id FK
      date llegada
      date salida
      varchar tipo
    }
    POLITICA_CANCELACION {
      uuid id PK
      varchar nombre
      text regla
      boolean activa
    }
    ALOJAMIENTO_POLITICA {
      uuid alojamiento_id PK,FK
      uuid politica_id PK,FK
    }
    COTIZACION {
      uuid id PK
      uuid huesped_id FK
      uuid alojamiento_id FK
      date llegada
      date salida
      int noches
      numeric subtotal_base
      numeric tarifa_limpieza
      numeric tarifa_servicio
      numeric descuentos
      numeric adicionales
      numeric total
      varchar moneda
      timestamptz calculada_en
    }
    RESERVA {
      uuid id PK
      uuid huesped_id FK
      uuid alojamiento_id FK
      uuid cotizacion_id FK,UK
      date llegada
      date salida
      varchar estado
      varchar clave_idempotencia UK
      timestamptz creada_en
      timestamptz confirmada_en
      timestamptz completada_en
    }
    PAGO_SIMULADO {
      uuid id PK
      uuid reserva_id FK
      varchar proveedor
      varchar referencia_externa
      varchar estado
      timestamptz procesado_en
    }
    CANCELACION {
      uuid id PK
      uuid reserva_id FK,UK
      uuid solicitante_id FK
      text motivo
      numeric devolucion_simulada
      varchar estado_final
      timestamptz solicitada_en
    }
    RESENA {
      uuid id PK
      uuid reserva_id FK,UK
      uuid huesped_id FK
      uuid alojamiento_id FK
      int calificacion
      text contenido
      varchar estado
      timestamptz creada_en
    }
    NOTIFICACION {
      uuid id PK
      uuid usuario_id FK
      varchar tipo
      text mensaje
      timestamptz creada_en
      timestamptz leida_en
    }
    EVENTO_AUDITORIA {
      uuid id PK
      uuid actor_id FK
      varchar tipo_evento
      varchar entidad_tipo
      uuid entidad_id
      varchar resultado
      text motivo
      varchar correlation_id
      timestamptz creado_en
    }
    EXPORTACION_REPORTE {
      uuid id PK
      uuid solicitante_id FK
      varchar tipo_reporte
      varchar formato
      text parametros
      varchar estado
      timestamptz solicitada_en
    }
```

## Diccionario y restricciones importantes

| Entidad | Responsabilidad | Restricciones propuestas |
|---|---|---|
| USUARIO / PERFIL | Identidad, rol, estado y datos básicos. | Correo único; nunca almacenar contraseña en texto plano; reglas de acceso por rol y propietario. |
| ALOJAMIENTO | Publicación del anfitrión. | `capacidad > 0`; `precio_noche > 0`; propietario obligatorio; estado de enumeración acordada. |
| ALOJAMIENTO_SERVICIO | Relación muchos-a-muchos con catálogo de amenidades. | Clave compuesta alojamiento/servicio. |
| PERIODO_DISPONIBILIDAD | Periodos configurados, bloqueados o reservados. | `llegada < salida`; no permitir conflictos con reservas confirmadas. |
| POLITICA_CANCELACION / ALOJAMIENTO_POLITICA | Define política asociada a la publicación. | El MVP debe aplicar al menos una política. La cardinalidad efectiva y versionado de política se deben decidir. |
| COTIZACION | Congela el desglose consultado por el huésped. | Noches > 0; importes no negativos; total calculado por reglas configuradas; registrar moneda y momento del cálculo. |
| RESERVA | Vincula huésped, alojamiento, periodo y cotización. | Salida > llegada; clave idempotente única dentro del alcance acordado; constraint/control transaccional para solapamientos, no solo consulta previa. |
| PAGO_SIMULADO | Resultado de integración de pruebas. | Solo usar entorno de pruebas; no guardar secretos ni datos de tarjeta. |
| CANCELACION | Evidencia de cancelación y devolución simulada. | Usuario, fecha, motivo, monto simulado y estado final obligatorios según guía. |
| RESENA | Reseña ligada a estancia. | `calificacion` dentro del rango acordado; única por reserva como propuesta; huésped y alojamiento deben coincidir con la reserva y estado completado. |
| EVENTO_AUDITORIA | Historial de cambios/acciones de negocio. | Actor, fecha, entidad, tipo y resultado consultables; no borrar registros para ocultar acciones. |
| EXPORTACION_REPORTE | Auditoría de generación PDF/Excel. | Solicitante/fecha obligatorios; incluir filtros/parámetros y estado de generación. |

## Reglas de integridad que requieren diseño físico

1. **Reserva solapada:** dos intervalos para el mismo alojamiento se solapan si `nueva_llegada < existente_salida AND nueva_salida > existente_llegada` cuando ambos ocupan inventario. La salida se modela excluida si el grupo aprueba esa convención. PostgreSQL puede implementar bloqueo/constraint apropiado; la elección final depende de la estrategia de servicio/transacción.
2. **Idempotencia:** insertar/registrar la clave idempotente en la misma transacción lógica que crea la reserva, con respuesta repetible para reintentos.
3. **Histórico de precio:** la reserva conserva los valores aceptados de la cotización, incluso si el anfitrión cambia después el precio.
4. **Bloqueo de anuncio:** no eliminar reservas futuras al bloquear la publicación; señalarlas para revisión manual.
5. **Auditoría/exportación:** registrar actor y fecha sin exponer secretos o innecesarios datos sensibles.

## Pendientes para completar el modelo físico

- Definir estrategia exacta de intervalos y bloqueos concurrentes.
- Definir moneda y redondeo decimal de importes.
- Definir si la publicación puede tener más de una política/versiones históricas.
- Alinear propiedad de tablas/entidades con límites de servicios para evitar acoplamiento transaccional indebido.
- Derivar migraciones SQL versionadas cuando se acepte el esquema físico.
