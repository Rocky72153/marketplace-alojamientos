# Diagrama de clases del backend

El [diagrama editable para diagrams.net](diagramas/diagrama-clases-marketplace.xml) presenta en un único lienzo los tipos declarados en `backend/src/main/java`: entidades, controladores, servicios, repositorios, seguridad, auditoría, datos de demostración, pagos de prueba y tipos anidados. Para abrirlo, utiliza **Archivo → Abrir** en diagrams.net. Las formas, textos y conexiones son editables; el documento usa únicamente blanco y negro.

El inventario se extrajo de los 71 archivos Java mediante el analizador sintáctico del JDK 21, sin tomar comentarios como declaraciones. Contiene **126 tipos**: 55 clases, 53 records, 10 interfaces y 8 enumeraciones, organizados en 13 paquetes. Los nombres como `ReservationService.QuoteView` identifican tipos anidados y su contenedor. Los cuadros muestran atributos y operaciones seleccionados; cuando se omiten miembros se indica su cantidad. No reemplazan la consulta del código para firmas completas, visibilidad de todos los miembros o comportamiento.

La notación `+`, `-`, `#` y `~` corresponde a visibilidad pública, privada, protegida y de paquete. Las conexiones representan relaciones principales verificadas en las declaraciones:

- **Línea continua y flecha abierta:** asociación JPA. La marca `[JPA 1]` señala que el destino es obligatorio. En las asociaciones mostradas, cada destino puede tener cero o más referencias de origen, salvo la reseña: la restricción única permite como máximo una reseña por reserva. Estas multiplicidades no implican que exista una colección inversa en Java.
- **Línea discontinua y flecha abierta:** dependencia entre tipos del proyecto declarada como campo final en controladores, servicios y otros componentes. No representa todas las dependencias indirectas ni todas las referencias en firmas de métodos.
- **Línea discontinua y triángulo vacío:** implementación o herencia entre tipos presentes en el inventario. No se representan las interfaces externas de las bibliotecas.
- **Línea punteada y flecha abierta:** referencia escalar por UUID de `StripeCheckoutAttempt`, no una asociación JPA. Su `reservationId` es único, de modo que puede existir como máximo un intento por reserva.

Las rutas ortogonales incluyen puntos de paso explícitos. Se verificó geométricamente que los segmentos generados no atraviesen el interior de los cuadros de clase. La selección de dependencias y las operaciones mostradas son una vista abreviada del código, no un análisis exhaustivo de cada referencia o del flujo de ejecución.

`ListingExtraOption` y `ReservationExtra` son valores incorporados en colecciones. El perfil forma parte de `MarketplaceUser`, y la cotización se calcula en el servicio sin una entidad persistente propia. `StripeProcessedEvent` guarda el identificador de un evento procesado sin una asociación JPA al intento. `AuditEvent` usa identificadores y tipos escalares; no se dibuja una asociación JPA inexistente. La notificación sí tiene una asociación JPA con su destinatario, mientras que sus campos `referenceType` y `referenceId` son escalares.

El diagrama corresponde al backend implementado y complementa el [modelo de dominio conceptual](entrega-1/07-modelo-dominio.md) y el [modelo lógico propuesto](entrega-1/08-modelo-datos.md), que son artefactos históricos de diseño. La entrega está relacionada con [MARKET-71](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-71); la [página del diagrama en Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/12451842) conserva su documentación publicada.
