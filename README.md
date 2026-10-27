# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, consulta de disponibilidad, cotización, reserva y cancelación de alojamientos temporales.

## Alcance implementado

La base funcional cubre registro e inicio de sesión con token, perfil, publicaciones, disponibilidad, búsqueda, detalle público, cotización, reserva idempotente, historial propio y cancelación con reembolso simulado. Los cobros reales y el envío real de correos no forman parte del MVP. Las tarifas, reglas de cálculo, privacidad y recuperación de disponibilidad se describen en [EP-05 y EP-06](docs/EP05-EP06-reservas-cancelaciones.md).

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

El backend expone autenticación y perfil, consulta de publicaciones, `POST /api/listings/{listingId}/quotes`, y el flujo protegido `/api/reservations` para confirmar, consultar y cancelar reservas. El frontend consume la búsqueda, el detalle, la cotización y el historial; ajusta la etiqueta `api-base-url` en `frontend/index.html` si el backend usa otra dirección.

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
