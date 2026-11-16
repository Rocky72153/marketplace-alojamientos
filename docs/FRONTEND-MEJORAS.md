# Mejoras de experiencia y diseño — 15 de noviembre de 2026

Paquete de interfaz: `49e2397dfdde3191`. La actualización mantiene el backend,
el catálogo y Stripe en modo de prueba.

## Cambios entregados

1. **Tarjetas:** imagen publicada cuando existe, alternativa local identificada
   cuando falta o no carga, tipo, ciudad, capacidad, servicios y precio por noche
   en COP. El catálogo ficticio no incluye fotografías; las cuatro ilustraciones
   SVG fueron creadas para esta interfaz. No se inventan fotos ni valoraciones.
2. **Búsqueda:** filtros desplegables con recuento, aplicación y limpieza,
   resumen de destino/fechas/huéspedes, accesos a destinos y estados de carga,
   vacío y error con reintento. Se mantienen criterios y páginas al abrir fichas.
3. **Ficha:** galería con miniaturas y teclado, información organizada por
   servicios, reglas, cancelación y extras. Cotización lateral fija en escritorio
   y acceso directo al formulario en móvil.
4. **Reserva:** guía de cuatro pasos, cotización compacta con fechas, huéspedes,
   desglose y total, opción de editar sin perder selecciones y reintentos seguros.
   Las cuentas de administrador y anfitrión pueden cotizar; reservar requiere
   huésped. Stripe conserva su URL completa e idempotencia; la pantalla de retorno
   solo confirma un pago cuando el servidor devuelve `PAID`.
5. **Móvil:** menú compacto con Escape y cierre al navegar, formularios y botones
   cómodos, tarjetas en una columna y tablas con desplazamiento propio.
6. **Paneles:** historial filtrable por alojamiento/código/estado y acciones
   plegadas; anfitrión con filtros y editor central; administración separada en
   Resumen, Cuentas, Publicaciones, Revisión manual y Auditoría, con tablas y
   estados legibles. Los filtros de anfitrión y administración se identifican
   como aplicables a la página actual; conservan los totales y páginas del servidor.
7. **Acabados y accesibilidad:** indicadores de carga, mensajes útiles, navegación
   por teclado, foco visible y enlace para saltar al contenido sin cambiar la
   ruta. Hay animaciones suaves de sección, tarjetas, imágenes, menús y filtros;
   `prefers-reduced-motion: reduce` desactiva animaciones y transiciones.

## Verificación

Se probó la interfaz real en el navegador a 1440 × 900 y 390 × 844, usando una
vista previa local del paquete con las API del sitio publicado. Se comprobaron
búsqueda, aplicación/limpieza de filtros, conservación de fechas, cotización,
edición, acceso requerido y navegación administrativa. No hubo desbordamiento
horizontal de la página; las tablas móviles desplazan su propio contenedor.

Las pruebas automatizadas cubren también permisos, paginación, respuestas
atrasadas, cierre de sesión, galería, imágenes inseguras o rotas y estados de
Stripe. El backend no requiere una migración ni un reinicio por esta actualización.
No se realizó un nuevo pago con tarjeta durante esta revisión; las pruebas del
pago completo y reembolso siguen en [la evidencia de Stripe](evidencias/stripe-sandbox-2026-11-14.json).

## Capturas

- [Inicio en escritorio](evidencias/frontend-redisenio-2026-11-15/inicio-escritorio.jpg)
- [Inicio en móvil](evidencias/frontend-redisenio-2026-11-15/inicio-movil.jpg)
- [Ficha y cotización](evidencias/frontend-redisenio-2026-11-15/detalle-cotizacion.jpg)
- [Resumen administrativo](evidencias/frontend-redisenio-2026-11-15/administracion-resumen.jpg)

Las capturas no incluyen contraseñas, tokens ni listas de correos de usuarios.
