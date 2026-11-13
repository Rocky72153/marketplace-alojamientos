# Guía de ejecución local

Esta guía describe el arranque de desarrollo en un equipo con Git, Java y Node.js. El backend usa PostgreSQL; el frontend sirve una interfaz que consulta ese backend. Los alojamientos del catálogo inicial son ficticios.

## Herramientas

Instala Git, JDK 21, Maven 3.9 o compatible, Node.js 22 con npm y Docker Desktop con Docker Compose. Comprueba que Docker esté iniciado y registra las versiones para la evidencia de tu equipo:

```powershell
git --version
java -version
mvn -version
node --version
npm --version
docker version
docker compose version
```

El repositorio y Docker deben descargarse por primera vez con acceso a Internet. Los ejemplos siguientes usan PowerShell; en otros sistemas adapta la asignación de variables de entorno.

## 1. Clonar y arrancar PostgreSQL

```powershell
git clone https://github.com/Rocky72153/marketplace-alojamientos.git
cd marketplace-alojamientos
docker compose up -d db
docker compose ps
```

Espera a que el servicio `db` figure como saludable. Compose conserva la información en el volumen `marketplace-postgres-data`, aun después de detener el contenedor. Su configuración predeterminada es PostgreSQL local en el puerto 5432, base y usuario `marketplace` y contraseña de ejemplo `marketplace_local`; corresponde solo a desarrollo.

Para verificar un clon con una base vacía sin modificar un volumen existente, puedes usar en su lugar un contenedor temporal aislado. Estos datos se eliminan al detener ese contenedor; no lo uses para información que quieras conservar:

```powershell
docker run --rm -d --name marketplace-validacion-db -e POSTGRES_DB=marketplace -e POSTGRES_USER=marketplace -e POSTGRES_PASSWORD=marketplace_local -p 15433:5432 postgres:17-alpine
```

En la terminal del backend, antes de arrancarlo, usa `$env:DB_URL = "jdbc:postgresql://localhost:15433/marketplace"`. Al terminar, `docker stop marketplace-validacion-db` elimina solo ese contenedor temporal y sus datos, no el volumen de Compose.

## 2. Configurar y arrancar el backend

En otra terminal PowerShell, desde la raíz del clon, define una clave propia de al menos 32 bytes para mantener válidas las sesiones entre reinicios. Si deseas iniciar sesión con las cuentas de muestra, define también su contraseña propia de 8 a 72 caracteres antes del primer arranque. No copies los ejemplos literalmente ni guardes tus valores en Git:

```powershell
$env:AUTH_TOKEN_SECRET = "<tu-secreto-local-de-al-menos-32-bytes>"
# Opcional; solo afecta a las cuentas demo al crearlas por primera vez.
$env:DEMO_SEED_PASSWORD = "<tu-clave-local-de-demostracion>"
mvn -f backend/pom.xml spring-boot:run
```

Si no se define `AUTH_TOKEN_SECRET`, el backend genera un secreto temporal: las sesiones existentes dejan de ser válidas al reiniciar. Si se proporciona, debe tener al menos 32 bytes. La clave `ADMIN_BOOTSTRAP_KEY` es opcional y permite registrar un administrador mediante la API; sin ella el registro público admite huéspedes y anfitriones. Para un frontend en otro origen, configura `CORS_ALLOWED_ORIGINS`. La dirección de la base, el usuario, la contraseña y el puerto pueden ajustarse con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `PORT`.

El perfil predeterminado `local` carga hasta 10.000 alojamientos ficticios en Colombia y 1.001 cuentas de muestra en PostgreSQL local. La primera carga puede tardar; espera a que finalice antes de comprobar el catálogo. Sin `DEMO_SEED_PASSWORD`, las cuentas demo reciben una contraseña aleatoria desconocida. Definirla después no cambia contraseñas ya creadas; puedes registrar tu propia cuenta de huésped. `DEMO_SEED_ENABLED=false` desactiva la carga y `DEMO_SEED_LISTING_COUNT` ajusta su tamaño. Una base con el catálogo anterior se amplía conservando los registros; las entradas antiguas editadas o eliminadas que no puedan identificarse se omiten. Si el catálogo es demasiado ambiguo, la carga se detiene sin modificarlo.

Verifica en otra terminal:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod "http://localhost:8080/api/listings/cities?query=bogota"
```

La respuesta de salud incluye `status: UP` y `application: marketplace-alojamientos`. La segunda consulta debe devolver sugerencias de ciudad si el catálogo está cargado.

## 3. Arrancar el frontend

En otra terminal, desde la raíz:

```powershell
cd frontend
npm ci
npm run dev
```

Abre [http://localhost:4173](http://localhost:4173). La búsqueda y las sugerencias consultan el backend; puedes explorar sin iniciar sesión. Las reservas requieren cuenta de huésped. Por defecto la página usa `http://localhost:8080`; si cambias la dirección del backend, ajusta la etiqueta `api-base-url` de `frontend/index.html` y la configuración CORS correspondiente.

Puedes crear una cuenta de anfitrión y entrar a «Administrar alojamientos» para crear y editar anuncios y disponibilidad. Si un anfitrión desactiva su cuenta o un administrador la restringe, sus publicaciones dejan de mostrarse y no aceptan nuevas reservas. Las reservas futuras confirmadas conservan su estado y se señalan para revisión administrativa. El pago y la recuperación de contraseña son simulaciones: no hay cobros ni envío de correos reales.

## 4. Verificar las pruebas

Desde la raíz del clon:

```powershell
mvn -B -ntp -f backend/pom.xml verify
cd frontend
npm run lint
npm test
npm run build
```

Las pruebas del backend usan bases H2 aisladas con el perfil de pruebas. La comprobación anterior no reemplaza ejecutar el backend contra PostgreSQL ni verificar el flujo completo en el navegador.

## 5. Detener el entorno

Finaliza el backend y el servidor del frontend con Ctrl+C en sus terminales. Desde la raíz, `docker compose stop db` detiene el contenedor y conserva el volumen con los datos. Después puedes cerrar Docker Desktop.

Esta guía está preparada para ser comprobada desde un clon limpio; por sí sola no demuestra que esa comprobación se haya ejecutado. No es un manual de despliegue de producción.
