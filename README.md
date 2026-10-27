# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, consulta de disponibilidad, cotización, reserva y cancelación de alojamientos temporales.

## Alcance implementado

La base funcional cubre las primeras historias priorizadas: registro e inicio de sesión con token, recuperación simulada, lectura/edición/desactivación del perfil, publicación y cambio de estado de alojamientos, administración de disponibilidad y búsqueda por ciudad, fechas, huéspedes, tipo, precio y servicios. Las publicaciones no activas no aparecen en la búsqueda pública. Las reservas y el envío real de correos siguen fuera de este alcance.

## Tecnologías base propuestas

- Backend: Java 21, Spring Boot y Maven.
- Persistencia: PostgreSQL.
- Frontend inicial: HTML, CSS y JavaScript nativo.
- CI: GitHub Actions.
- Calidad estática: SonarQube Cloud (SonarCloud).

El equipo debe confirmar estas decisiones antes de ampliar las funcionalidades.

## Estructura

- `backend/`: API REST Java/Spring Boot y pruebas.
- `frontend/`: interfaz web inicial y pruebas con Node.js.
- `.github/workflows/`: pipelines de integración continua y análisis estático.
- `docs/`: decisiones técnicas, guía de GitHub y matriz de trazabilidad.
- `scripts/`: configuración auxiliar.

## Requisitos locales

Java 21, Maven, Node.js 22, npm, Docker Compose y Git.

## Ejecutar y verificar

Backend: `mvn -B -f backend/pom.xml verify`

Frontend: `cd frontend && npm ci && npm run lint && npm test && npm run build`

Base de datos local: `docker compose up -d db`

Para que los tokens sigan siendo válidos tras reiniciar el backend, configura `AUTH_TOKEN_SECRET` con al menos 32 bytes. `ADMIN_BOOTSTRAP_KEY` permite registrar una cuenta `ADMIN` de forma controlada; sin esa clave, el registro público solo admite `GUEST` y `HOST`. Define `CORS_ALLOWED_ORIGINS` si el frontend se sirve desde otro origen. La recuperación de cuenta solo registra la solicitud y devuelve el mismo mensaje exista o no la cuenta; no envía correo ni cambia contraseñas.

El backend expone `/api/auth/register`, `/api/auth/login`, `/api/auth/recovery`, `/api/profile/me`, `/api/listings`, `/api/listings/{listingId}/state` y `/api/listings/{listingId}/availability`. El frontend consume la búsqueda pública en `GET /api/listings`; ajusta la etiqueta `api-base-url` en `frontend/index.html` si el backend usa otra dirección.

## Catálogo y cuentas de demostración

El perfil opcional `demo` carga 5.000 alojamientos ficticios repartidos entre los 1.103 municipios del catálogo DIVIPOLA de 2024, con tarifas en COP, capacidades, servicios y disponibilidad de muestra. También crea 1.001 cuentas: 500 huéspedes (`guest.001@marketplace.test` a `guest.500@marketplace.test`), 500 anfitriones (`host.001@marketplace.test` a `host.500@marketplace.test`) y un administrador (`demo-admin@marketplace.test`). Los nombres de municipio y departamento se basan en el [catálogo DIVIPOLA de DANE publicado en Datos Abiertos Colombia](https://www.datos.gov.co/Mapas-Nacionales/DIVIPOLA-C-digos-municipios/gdxc-w37w); los alojamientos y las cuentas sí son ficticios. Los registros de muestra usan dominios `.test` y no deben copiarse a producción.

Para cargar estos datos en una base local, inicia PostgreSQL y ejecuta el backend con el perfil `demo` y una contraseña definida por ti. En PowerShell:

```powershell
docker compose up -d db
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:DEMO_SEED_PASSWORD = "una-clave-local-de-8-a-72-caracteres"
$env:AUTH_TOKEN_SECRET = "define-aqui-un-secreto-local-de-al-menos-32-caracteres"
mvn -f backend/pom.xml spring-boot:run
```

La contraseña configurada se aplica a las cuentas demo locales. El seeder corre solo con el perfil `demo`, es idempotente y puede apagarse con `DEMO_SEED_ENABLED=false`; sin `DEMO_SEED_PASSWORD` válido, el backend falla antes de escribir datos. Al abrir la página se muestran destinos sugeridos sin exigir inicio de sesión; el campo ciudad busca en los 1.103 municipios, tolera tildes y ofrece hasta diez opciones navegables con teclado. La consulta de alojamientos es pública; confirmar una reserva, consultar reservas y cancelarlas requiere una sesión de huésped.

## Flujo de trabajo

1. Definir una historia en Jira con criterios de aceptación Dado que / Cuando / Entonces.
2. Crear una rama desde `develop`: `feature/US-001-registro`, `fix/US-010-disponibilidad` o `docs/US-020-documentacion`.
3. Hacer commits pequeños y referenciar Jira cuando aplique.
4. Abrir un Pull Request hacia `develop` con la plantilla del repositorio.
5. Esperar que GitHub Actions, las pruebas, el análisis de calidad y la revisión de otro integrante finalicen correctamente.
6. Integrar a `main` mediante Pull Request cuando exista una versión estable.

## Configuración pendiente

- Confirmar visibilidad y colaboradores del repositorio.
- Configurar `SONAR_TOKEN` como secreto y reemplazar la organización y clave de proyecto en `sonar-project.properties`.
- Crear `develop` y proteger `main` y `develop` después de que el pipeline publique sus checks.
- Enlazar historias, commits, Pull Requests, pruebas y releases en la matriz de trazabilidad.

Consulta `CONTRIBUTING.md` y `docs/GITHUB_SETUP.md` para el flujo detallado.
