# EP-09: administración y reportes

La épica MARKET-28 comprende la moderación y consulta de auditoría (MARKET-20), el tablero administrativo (MARKET-21) y la exportación de reportes (MARKET-22). El acceso a estas capacidades corresponde exclusivamente al rol administrador, conforme a DEC-07 en [el registro de decisiones](DECISIONS.md).

## Definiciones del tablero

El Product Owner aprobó estas reglas para el MVP el 11 de noviembre de 2026 (DEC-11):

- Los usuarios y las publicaciones activas son totales actuales, calculados al generar la consulta; no representan una reconstrucción histórica.
- El período filtra las reservas por su fecha de creación. Sus fechas inicial y final son inclusivas y se interpretan con la zona horaria de Bogotá.
- Las reservas se muestran por estado. Los ingresos estimados suman el total de las reservas confirmadas o completadas del período; los pagos del proyecto son simulados y no representan recaudo real.
- La demanda por ciudad cuenta las reservas confirmadas o completadas del período. El tablero muestra el momento en que se generó la información.
- Los reportes PDF y XLSX aplican los mismos cálculos y filtros que el tablero.

El bloqueo de una publicación conserva las reservas confirmadas cuya fecha de llegada sea hoy o posterior y las señala para revisión manual; la operación no equivale a una cancelación ni autoriza cobros o reembolsos reales. Las acciones administrativas y la exportación dejan trazabilidad.

## Evidencia de ejecución

El 11 de noviembre de 2026 se verificó el flujo contra PostgreSQL 17 en una base local desechable con usuarios, alojamiento y reserva ficticios. Se compararon los indicadores con la reserva, se rechazó el acceso de un huésped, se descargaron PDF y XLSX, se comprobó la auditoría de un formato inválido y se bloqueó una publicación conservando su reserva para revisión. Los archivos exportados se comprobaron por su formato; las pruebas automatizadas inspeccionan también su contenido.

- [Panel y reportes con datos ficticios](evidencias/ep09-panel-demo.jpg).
- [Auditoría de acciones de prueba](evidencias/ep09-auditoria-demo.jpg).
- Suite backend: 26 pruebas aprobadas después de aislar las bases de datos de prueba. Suite frontend: 42 pruebas aprobadas; revisión de archivos y compilación aprobadas.

El 12 de noviembre de 2026 se amplió el catálogo de demostración de la base local persistente de 5.000 a 10.000 alojamientos, conservando los identificadores de los 5.000 originales y las 1.001 cuentas ya existentes. Se verificaron 1.122 destinos activos. La prueba automatizada también cubre una base nueva, reinicios y la preservación de registros antiguos editados o eliminados; los datos generados son ficticios.
