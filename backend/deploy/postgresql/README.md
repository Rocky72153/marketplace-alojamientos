# Migración de estados para Stripe de prueba

La base PostgreSQL existente puede conservar una restricción generada por una versión anterior de Hibernate que solo permite `CONFIRMED`, `CANCELLED` y `COMPLETED`. El código de Stripe requiere también `PENDING_PAYMENT`; `ddl-auto=update` no garantiza actualizar esa restricción. El script `20261114-reservation-pending-payment.sql` modifica únicamente la restricción conocida, en una transacción, y falla si el esquema o sus valores son inesperados. No borra ni actualiza filas.

Antes de aplicar el cambio, comprobar el respaldo restaurable, detener el servicio de la aplicación y verificar que la base y el esquema son los previstos. Ejecutar sobre la base correcta con una cuenta que sea dueña de la tabla:

```sh
psql -X -v ON_ERROR_STOP=1 -d NOMBRE_DE_LA_BASE -f 20261114-reservation-pending-payment.sql
```

El script fija un tiempo de espera de bloqueo y devuelve error si no puede adquirirlo. Su ejecución se puede repetir después de una migración exitosa. Volver a una versión que no reconoce `PENDING_PAYMENT` exige primero conciliar todos los intentos de Stripe: revertir el binario no revierte automáticamente los datos.

La prueba `test-reservation-pending-payment.sql` crea una tabla `reservations` e inserta datos; ejecutarla **solo en una base nueva, vacía y desechable**, nunca sobre la base del proyecto. El 14 de noviembre de 2026 se ejecutó en PostgreSQL 18.6 aislado: preservó los estados anteriores, admitió el nuevo estado, rechazó valores desconocidos y soportó ejecución repetida.

La migración no instala Stripe ni prueba pagos. Un evento de una sesión de prueba que se haya creado sin dejar una respuesta y que no llegue al receptor requerirá revisión manual. Los reembolsos cuyo resultado siga incierto después de la ventana de reintento seguro también quedan en revisión manual; no deben repetirse manualmente sin consultar primero a Stripe.
