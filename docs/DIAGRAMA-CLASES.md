# Diagrama de clases del sistema implementado

El [diagrama editable en formato XML de diagrams.net](diagramas/diagrama-clases-marketplace.xml) describe las clases persistentes principales del backend. Para consultarlo o modificarlo, abre el archivo en diagrams.net mediante **Archivo → Abrir** y selecciona una de sus tres páginas. El documento está en blanco y negro y contiene formas y conexiones editables.

1. **Dominio persistente:** usuarios, alojamientos, disponibilidad, reservas, reseñas, notificaciones y auditoría.
2. **Stripe de prueba:** intento de pago, sus referencias y estados; además, el registro de eventos procesados.
3. **Valores y estados:** enumeraciones y objetos incorporados usados por publicaciones y reservas.

El diagrama muestra los atributos y operaciones esenciales, con firmas abreviadas para facilitar su lectura; no es un inventario exhaustivo de métodos, clases auxiliares ni servicios. Los nombres corresponden a Java. El perfil forma parte de `MarketplaceUser`; `ListingExtraOption` y `ReservationExtra` son valores incorporados en colecciones. La cotización es una vista calculada del servicio y no se presenta como entidad persistente.

Las líneas continuas de la página de dominio identifican asociaciones JPA y muestran sus multiplicidades. El lado inverso indicado por la multiplicidad no implica que exista una colección inversa en la clase Java. Por ejemplo, una reseña corresponde a una reserva y la restricción de unicidad permite como máximo una reseña por reserva.

En la página de pagos, las líneas discontinuas representan referencias escalares por UUID: `StripeCheckoutAttempt` guarda `reservationId`, `guestId` y `listingId` y no define asociaciones JPA hacia esas clases. La reserva puede tener como máximo un intento por la restricción única de `reservationId`. `StripeProcessedEvent` registra un identificador de evento procesado, sin una asociación JPA al intento. Del mismo modo, `AuditEvent` conserva identificadores y tipos escalares; no se ha dibujado una asociación JPA que no existe. La asociación entre notificación y destinatario sí está implementada mediante JPA, mientras que `referenceType` y `referenceId` son campos escalares.

Este diagrama documenta la implementación actual y complementa el [modelo de dominio conceptual](entrega-1/07-modelo-dominio.md) y el [modelo lógico propuesto](entrega-1/08-modelo-datos.md), que son artefactos históricos de diseño. Las fuentes para contrastarlo se encuentran en los paquetes `security`, `listing`, `booking`, `review`, `notification` y `audit` de `backend/src/main/java/co/edu/unbosque/marketplace/`.

La entrega se registra en [MARKET-71](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-71). El XML también está adjunto a la [página del diagrama en Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/12451842).
