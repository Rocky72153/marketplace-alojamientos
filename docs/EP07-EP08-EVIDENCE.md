# EP-07 y EP-08: notificaciones y reseñas

Este documento registra el alcance implementado y las comprobaciones realizadas el 11 de noviembre de 2026 para las historias HU-13 (MARKET-17), HU-14 (MARKET-18) y HU-15 (MARKET-19), asociadas a EP-07 (MARKET-26) y EP-08 (MARKET-27). No sustituye la revisión de los cambios ni acredita por sí solo una integración o el cierre de las historias en Jira.

## Alcance

- **Notificaciones internas:** el anfitrión recibe un aviso al cambiar el estado de una publicación. El huésped y el anfitrión reciben avisos al confirmar o cancelar una reserva. Cada persona autenticada consulta y marca como leídas solo sus propias notificaciones; una consulta directa a una notificación ajena responde que no se encontró.
- **Reseñas:** un huésped puede publicar una reseña por reserva completada del alojamiento correspondiente, con puntuación entre 1 y 5 y texto no vacío de hasta 5.000 caracteres. Las reseñas visibles se consultan públicamente en el detalle del alojamiento. La regla de unicidad también se aplica ante solicitudes concurrentes.
- **Moderación:** un administrador puede ocultar una reseña indicando un motivo. La reseña oculta deja de mostrarse en la consulta pública y la acción registra administrador, fecha y motivo; una cuenta sin el rol de administrador no puede moderar.
- **Fin de estancia:** de acuerdo con DEC-04 en [el registro de decisiones](DECISIONS.md), una reserva confirmada pasa a completada después de su fecha de salida, según el calendario de Bogotá. Las reservas canceladas no se completan. La comprobación corre al arrancar la aplicación, diariamente a medianoche de Bogotá y al consultar reservas o crear reseñas.

Las notificaciones se almacenan dentro de la aplicación; este alcance no incluye envío de correo real.

## Pruebas realizadas

| Comprobación | Resultado observado |
|---|---|
| Backend: `mvn -B -ntp -f backend/pom.xml clean verify` | 20 pruebas aprobadas, sin fallos ni errores. Incluyen privacidad y eventos de notificación, elegibilidad y concurrencia de reseñas, moderación y límite de fecha de finalización. |
| Frontend: `npm test` en `frontend/` | 33 pruebas aprobadas, incluidas la visualización del historial de reservas ya reseñadas y la presentación de errores al cargar reseñas. |
| Frontend: `npm run lint` y `npm run build` en `frontend/` | Ambas verificaciones finalizaron correctamente. |
| Documentación: `python scripts/validate_delivery_docs.py` | Validación correcta del OpenAPI, BPMN, CSV y enlaces locales del paquete documental. |
| Integración local con PostgreSQL 17 | Se ejecutaron solicitudes HTTP contra una base de datos de prueba desechable, con cuentas y alojamientos ficticios. Se verificaron avisos de confirmación y cancelación, privacidad y marcado de lectura; reserva idempotente; creación de reseñas; rechazo de duplicados y de usuarios no elegibles; y moderación con auditoría y exclusión de la vista pública. |

Para ensayar las reseñas de estancias completadas sin esperar fechas reales, las fechas de dos reservas se modificaron directamente **solo en la base de datos local de prueba**. Este procedimiento comprueba el comportamiento con fechas pasadas simuladas; no constituye una observación del paso del tiempo en producción.

Los contratos HTTP actualizados están en [OpenAPI](entrega-1/10-openapi.yaml). Las pruebas automatizadas se conservan en los directorios de pruebas del backend y frontend para poder repetirlas.
