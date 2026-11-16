# Guía de despliegue académico en una instancia EC2 existente

Este procedimiento prepara la publicación en una instancia Ubuntu existente con Nginx y PostgreSQL en la misma máquina. Es una guía y sus plantillas: **su presencia no demuestra que el despliegue haya sido ejecutado ni validado**. No crea instancias, bases administradas, balanceadores ni otros recursos de AWS. Ejecuta cada paso solo tras comprobar el estado real del servidor y el dominio.

## Alcance y requisitos previos

- Instancia existente y autorizada, acceso administrativo, DNS bajo control del equipo y posibilidad de abrir solo los puertos necesarios. El propietario eligió HTTP para la demostración: la comunicación no está cifrada y las contraseñas, sesiones y datos enviados pueden ser vistos o modificados en tránsito. Usar únicamente cuentas y datos ficticios, con contraseñas exclusivas para esta demostración, y no usar tarjetas reales. Para datos personales o cuentas reales es necesario HTTPS con certificado válido. La plantilla HTTPS opcional usa Let's Encrypt, pero no emite el certificado por sí sola.
- En el equipo de desarrollo: JDK 21, Maven, Node.js y npm. En el servidor: Nginx ya instalado, Java 21 en tiempo de ejecución y PostgreSQL local. No instalar Maven ni compilar en una máquina de 1 GB si es posible evitarlo.
- Comprobar arquitectura, espacio y memoria (`uname -m`, `df -h`, `free -h`), servicios y configuración existentes (`sudo nginx -T`, `sudo systemctl list-units --type=service`, `sudo ss -ltnp`). Comprobar paquetes disponibles y espacio estimado con `apt-cache policy openjdk-21-jre-headless postgresql` y `sudo apt-get -s install openjdk-21-jre-headless postgresql`. Si no hay espacio suficiente, detenerse; no borrar datos ajenos ni ampliar volúmenes por defecto.
- En una máquina de 1 GB sin swap, la carga inicial de 10.000 alojamientos puede tardar o agotar memoria. La JVM de ejemplo limita el heap a 384 MB, pero Java, PostgreSQL, Nginx y el sistema usan memoria adicional. Hay que observar consumo y logs; el éxito y el tiempo de carga no se garantizan. El swap usa espacio del mismo disco y su creación requiere comprobar capacidad y conveniencia. No es sustituto de memoria ni una razón para eliminar datos existentes.
- La reutilización evita crear infraestructura adicional, pero **no garantiza una factura de cero**: la instancia, el almacenamiento, la dirección pública, la transferencia y, según el tipo y modo, los créditos de CPU pueden generar cargos. Revisar la cuenta y la configuración antes de operar; no cambiar el tipo de instancia ni ampliar almacenamiento automáticamente. Consultar [documentación oficial de créditos EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/burstable-performance-instances-unlimited-mode.html).
- El catálogo y las cuentas iniciales son ficticios; no representan ofertas ni personas reales. El modo `SIMULATED_TEST` del proyecto es un simulador interno: por sí solo no demuestra integración con un proveedor externo de pruebas. No introducir ni procesar datos de tarjeta reales.

## 1. Preparar artefactos fuera del servidor

Desde la raíz del código revisado y aprobado, ejecuta `mvn -B -ntp -f backend/pom.xml verify`. En `frontend`, ejecuta `npm ci`, `npm run lint`, `npm test` y después, en la misma terminal, genera el sitio con `MARKETPLACE_SAME_ORIGIN=true npm run build` en Linux o con `$env:MARKETPLACE_SAME_ORIGIN='true'; npm run build` en PowerShell. El artefacto frontend resultante debe usar el mismo origen público para la API; comprueba el contenido de `frontend/dist/index.html` antes de copiarlo.

Copia de forma autenticada el JAR `backend/target/marketplace-backend-0.1.0-SNAPSHOT.jar` y el contenido de `frontend/dist/` a una carpeta de versión nueva del servidor, por ejemplo `/srv/marketplace/releases/VERSION/marketplace.jar` y `/srv/marketplace/releases/VERSION/frontend/`. `VERSION` debe identificar el commit o release revisado. El usuario de servicio solo necesita leer esos archivos; no copies `node_modules`, el repositorio completo ni archivos con secretos.

## 2. Preparar la base y la configuración

Instala únicamente los paquetes que falten después de revisar la simulación de instalación. PostgreSQL debe escuchar solo en loopback, sin abrir el puerto 5432 en la red. Comprueba el servicio y los puertos reales con `systemctl status postgresql` y `sudo ss -ltnp`.

Verifica que los nombres de usuario y base que vas a usar no existan o que sean los destinados explícitamente a este proyecto. Para una instalación nueva, abre `sudo -u postgres psql` y ejecuta las siguientes instrucciones **dentro de psql**:

```sql
CREATE ROLE marketplace LOGIN;
CREATE DATABASE marketplace OWNER marketplace;
\password marketplace
\q
```

Define la contraseña en el diálogo interactivo, sin escribirla en comandos, repositorio, capturas o conversaciones. Conserva una copia verificada de cualquier base existente antes de modificarla. No uses `dropdb`, `docker compose down -v` ni borres volúmenes para reinstalar.

Crea, si no existe, un usuario de sistema sin acceso interactivo llamado `marketplace`. Crea `/etc/marketplace` y el archivo `/etc/marketplace/marketplace.env` de propietario `root`, grupo `marketplace` y permisos `0640`, antes de introducir secretos. Ajusta sus valores al entorno:

```text
SPRING_PROFILES_ACTIVE=local
SERVER_ADDRESS=127.0.0.1
PORT=8080
DB_URL=jdbc:postgresql://127.0.0.1:5432/marketplace
DB_USERNAME=marketplace
DB_PASSWORD=<contraseña-generada-en-el-servidor>
AUTH_TOKEN_SECRET=<secreto-aleatorio-de-al-menos-32-bytes>
PAYMENT_MODE=SIMULATED_TEST
DEMO_SEED_ENABLED=true
DEMO_SEED_LISTING_COUNT=10000
```

Los valores entre `<...>` son marcadores, no credenciales funcionales. Usa valores propios, sin espacios ni saltos de línea, y protege el archivo; nunca los subas a Git. Si se necesita crear un administrador, configura un `ADMIN_BOOTSTRAP_KEY` aleatorio solo el tiempo necesario y realiza la operación mediante una conexión protegida, no por HTTP público. Sin `DEMO_SEED_PASSWORD`, las cuentas demo reciben una contraseña aleatoria desconocida; el equipo puede registrar sus propias cuentas ficticias. La primera carga crea datos demo en la base local. Verifica que haya terminado antes de abrir la aplicación; no la actives sobre datos reales.

## 3. Iniciar el backend y publicar la demostración HTTP

Instala la plantilla [marketplace.service](../deploy/ec2/marketplace.service) en `/etc/systemd/system/marketplace.service`. Confirma que `/srv/marketplace/current` no sea un directorio ajeno: debe ser un enlace simbólico controlado por el despliegue que apunte a la versión preparada. No sustituyas archivos o servicios de otros proyectos sin identificar su propiedad. Ejecuta `sudo systemctl daemon-reload` y `sudo systemctl enable --now marketplace`; revisa `systemctl status marketplace`, `journalctl -u marketplace -n 100` y `curl --fail http://127.0.0.1:8080/api/health`. No publiques el puerto 8080.

La plantilla considera el código de salida 143 como cierre normal de Java tras SIGTERM: durante la prueba se observó el cierre ordenado de Tomcat y de la conexión a PostgreSQL con ese código. Los demás códigos de error siguen sujetos a la política de reinicio.

Apunta el dominio a la instancia e instala [nginx-marketplace-http-demo.conf.example](../deploy/ec2/nginx-marketplace-http-demo.conf.example), sustituyendo `MARKETPLACE_DOMAIN` por el dominio real. Verifica `sudo nginx -t` y recarga Nginx solo si la prueba pasa. La plantilla no redirige a HTTPS; `/api/` conserva su ruta y se reenvía a `127.0.0.1:8080`. Esta configuración es solo para demostraciones con datos ficticios. Para habilitar HTTPS en el futuro existe la plantilla [nginx-marketplace.conf.example](../deploy/ec2/nginx-marketplace.conf.example), que requiere un certificado válido previamente instalado.

Stripe requiere HTTPS para entregar webhooks a un destino público. El sitio HTTP no ofrece por sí solo un destino público válido para esos eventos: una integración de pruebas necesita un mecanismo de reenvío seguro y verificar la firma del evento en el servidor. Su configuración y prueba son independientes de la plantilla HTTP; no se debe afirmar que los pagos externos funcionan sin comprobar el flujo completo. El simulador interno no sustituye esa integración.

Antes de reemplazar el sitio anterior, identifica su servicio y su configuración activa, guarda una copia de la configuración y conserva los datos. El reemplazo autorizado no implica borrar bases, directorios o volúmenes; desactiva únicamente el sitio o servicio que se haya identificado como anterior. No deshabilites ni sobrescribas servicios no relacionados. Mantén acceso administrativo restringido y no abras los puertos 5432 ni 8080 en el grupo de seguridad.

## 4. Comprobaciones y reversión

Desde fuera, abre `http://DOMINIO/` y comprueba sugerencias, búsqueda y la ruta `http://DOMINIO/api/health`. Usa cuentas y alojamientos ficticios para verificar alta, publicación, disponibilidad, cotización y reserva simulada. No introduzcas datos personales o tarjetas reales. No indiques que el proveedor externo de pagos se probó si solo se ejecutó el simulador interno. Registra fecha, versión, condiciones, resultados y capturas sin credenciales; actualiza Jira y Confluence con enlaces reales.

Para revertir, conserva la carpeta de la versión anterior y las copias de configuración; vuelve a apuntar el enlace `current` a la versión conocida, comprueba Nginx y reinicia el servicio. Un cambio de aplicación puede afectar el esquema (`ddl-auto=update`): una reversión binaria no garantiza revertir la base. Antes de cualquier migración, prueba restaurar una copia en un entorno aislado.

El almacenamiento de PostgreSQL persiste mientras exista el disco de la instancia, pero un respaldo guardado en el mismo disco no protege frente a pérdida de ese disco. Define y prueba una copia externa cifrada con el equipo y revisa su costo antes de configurarla. Si se habilita HTTPS más adelante, la renovación del certificado también debe probarse y vigilarse.
