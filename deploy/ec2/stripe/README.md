# Servicios del sandbox de Stripe en la demostración EC2

Configuración para el entorno académico publicado en HTTP. Solo admite la cuenta sandbox autorizada. Las claves de Stripe no se incluyen en estos archivos.

## Dependencias y configuración

- PostgreSQL, aplicación y migración de `backend/deploy/postgresql/` comprobados antes de activar los servicios.
- Stripe CLI oficial en `/opt/marketplace/stripe-cli/stripe`, propiedad de root, con sesión del sandbox bajo el usuario no interactivo `marketplace-stripe`. Su configuración permanece en `/var/lib/marketplace-stripe/config`.
- AWS CLI en `/usr/bin/aws` y usuario no interactivo `marketplace-secrets`. El perfil de la instancia debe tener solo `ssm:GetParameter` para `/marketplace/demo/stripe-test-key` en `us-east-2`; el parámetro es Standard/SecureString con clave administrada `aws/ssm`.
- `/run` debe ser tmpfs, sin swap activo. La preparación falla si no se cumplen estas condiciones y no imprime claves ni salida del proveedor.

Instalar los tres ejecutables sin extensión con propietario root y permiso 0755 en `/usr/local/libexec/marketplace/`; instalar las dos unidades `.service` en `/etc/systemd/system/`. El archivo `marketplace-stripe.conf` es un complemento de la unidad existente: instalarlo como `/etc/systemd/system/marketplace.service.d/zz-stripe.conf`. Su nombre lo sitúa después del ajuste temporal de memoria instalado en la instancia. Sus opciones explícitas de arranque prevalecen sobre `EnvironmentFile`: no basta con poner `Environment=PAYMENT_MODE=STRIPE_TEST` si el archivo contiene otro valor.

El marcador `/etc/marketplace/stripe-sandbox-enabled` se crea al instalar una configuración cuyo recorrido real de prueba ya fue validado. Ejecutar `systemctl daemon-reload` y verificar las unidades antes de iniciar la aplicación. Las dependencias del servicio principal arrancan el receptor y las credenciales también después de reiniciar el servidor. La preparación almacena secretos únicamente en `/run`, con permiso 0600; `LoadCredential` los entrega al proceso Java. No copiar ese directorio a respaldos ni al repositorio.

La plantilla Nginx HTTP debe conservar el bloqueo exacto del webhook público. El listener reenvía los eventos de la cuenta de pruebas directamente a `127.0.0.1:8080/api/stripe-test/webhook`. Tras instalar, comprobar salud, modo `STRIPE_TEST` y 404 para el webhook público. Después de reautorizar la CLI hay que reiniciar coordinadamente credenciales, listener y aplicación y comprobar firmas.

## Límites operativos

Un proceso de CLI activo no demuestra recepción continua de eventos. Si se desconecta, revisar las sesiones y reservas pendientes contra Stripe; las respuestas inciertas pasan a revisión manual. No repetir manualmente un reembolso sin contrastar antes con Stripe.

La instancia es un entorno de demostración confiable: otros procesos locales con acceso a IMDS podrían usar el permiso del perfil para leer el único parámetro de prueba. Los administradores con sudo pueden acceder a la configuración. Los servicios de aplicación y listener niegan acceso directo a IMDS como defensa adicional. No se debe ampliar el perfil a otros secretos ni presentar esta configuración como aislamiento de producción.

Parameter Store Standard no tiene costo fijo; otros recursos, llamadas KMS y el consumo de la instancia están sujetos al plan y créditos de la cuenta. No se crean nuevas instancias, balanceadores ni bases administradas.
