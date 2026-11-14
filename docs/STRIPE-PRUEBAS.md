# Integración de Stripe para demostración

Esta integración es exclusivamente para un sandbox de Stripe. No recibe claves de producción ni realiza cobros reales. Su código y pruebas automatizadas no demuestran por sí solos que el despliegue haya sido configurado o que se haya completado un pago en Stripe.

La aplicación inicia Checkout en Stripe y mantiene la reserva pendiente hasta recibir un evento firmado válido. La página de retorno no confirma el pago. La cancelación solicita un reembolso de prueba y solo lo muestra como procesado cuando Stripe lo confirma. Los eventos perdidos y los reembolsos cuyo resultado siga incierto se señalan para revisión; no deben repetirse manualmente sin consultar primero en Stripe.

Para configurar el servidor se necesitan el modo `STRIPE_TEST`, el identificador de la cuenta sandbox, la URL pública de la demostración, una clave restringida del sandbox y el secreto de firma del receptor. Las credenciales se deben suministrar por un mecanismo privado y permanecer fuera del repositorio, capturas y registros. La opción `marketplace.payment.stripe-allow-http-demo` habilita explícitamente una URL de retorno HTTP solo para este modo de pruebas. En HTTP se deben utilizar exclusivamente datos y cuentas ficticios.

Stripe exige HTTPS para un webhook público. En la demostración HTTP, Stripe CLI puede escuchar eventos de la cuenta de pruebas y reenviarlos a `127.0.0.1:8080/api/stripe-test/webhook`; el proceso debe permanecer disponible y cada evento debe verificarse con su secreto de firma. La plantilla Nginx devuelve 404 para el webhook público. Stripe CLI es una herramienta de pruebas: su desconexión puede impedir la recepción de eventos, por lo que hay que comprobar su estado y revisar los intentos pendientes.

La base PostgreSQL existente necesita la migración documentada en [backend/deploy/postgresql/README.md](../backend/deploy/postgresql/README.md). Aplicarla requiere respaldo verificado, aplicación detenida y comprobación del esquema. Antes de habilitar el modo Stripe deben probarse una reserva, la entrega firmada del evento, la confirmación y la cancelación con reembolso en el sandbox, además de los casos de fallo y repetición.
