# Verificación del frontend y Stripe — 15 de noviembre de 2026

La versión publicada en `http://moralesforero.lol/` usa el paquete de interfaz
`6006e79a47c4d669` y mantiene el servidor de pagos previamente validado. Conserva
los 10.000 alojamientos activos del catálogo de demostración.

## Correcciones

- La página y todos sus módulos usan la misma carpeta con versión. Nginx exige
  revalidar los archivos y devuelve 404 para módulos inexistentes. Se reprodujo
  el fallo de mezclar el HTML nuevo con el módulo principal anterior: abrir la
  cuenta dejaba visible el inicio porque el contenedor permanecía oculto.
- Una búsqueda pendiente ya no cambia la sección al terminar. Se comprobó el
  caso de buscar, abrir la cuenta y recibir después la respuesta de búsqueda.
- Los resultados tienen páginas de 12 tarjetas y conservan ciudad, fechas,
  huéspedes, filtros y página al abrir una ficha o volver a los resultados.
- La cotización recupera fechas y huéspedes de la búsqueda. El acceso solicitado
  desde una reserva permite volver a la ficha sin confirmar nada automáticamente.
- Al iniciar sesión desde el regreso de Stripe, se muestra nuevamente el estado
  del pago. La confirmación depende de la respuesta autenticada del servidor.
  Las acciones para volver a ficha o pago se ocultan cuando se abandona su contexto.

## Comprobaciones

Pasaron las 94 pruebas del frontend, su verificación de estructura y la
construcción para publicación. Las pruebas de integración cargan el HTML real
con jsdom e incluyen acceso, errores de autenticación, cierre de sesión,
navegación de huésped, anfitrión y administrador y regreso de Stripe tras entrar.

Desde Internet se compararon los 14 módulos y el CSS con el paquete construido;
todos coincidieron, con tipos de contenido correctos y revalidación. El dominio
principal y `www` sirven la misma versión. La API responde `UP`, la configuración
indica `STRIPE_TEST` y el webhook HTTP público permanece cerrado con 404.

Se cotizaron dos noches y se creó una sesión real de Stripe sandbox por
COP 654.850 con una cuenta ficticia. Repetir la solicitud devolvió el mismo
intento y el mismo enlace completo de Checkout; consultar sin sesión devolvió
401. Después se venció el Checkout sin pagar. El evento firmado dejó el intento
`EXPIRED` y la reserva `CANCELLED` con `TEST_EXPIRED`; la cuenta ficticia se
desactivó. No quedó una reserva de esta comprobación bloqueando las fechas.

Evidencia: [frontend-stripe-2026-11-15.json](evidencias/frontend-stripe-2026-11-15.json).
El pago completo con tarjeta de prueba y su reembolso de COP 255.000 ya están
registrados en [stripe-sandbox-2026-11-14.json](evidencias/stripe-sandbox-2026-11-14.json).

No se hizo un nuevo pago con tarjeta en esta revisión. El navegador de la
herramienta bloqueó la página, por lo que estas comprobaciones no incluyen una
revisión visual ni capturas nuevas. Las pruebas con DOM emulado y las peticiones
HTTP no sustituyen una validación visual en el navegador del usuario. Stripe
sigue en modo de prueba y el sitio conserva HTTP por decisión del proyecto.
