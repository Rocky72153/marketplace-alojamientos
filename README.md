# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, disponibilidad, cotización, reserva y cancelación de alojamientos temporales.

## Alcance implementado

La base funcional cubre registro e inicio de sesión con token, perfil, publicaciones, disponibilidad, búsqueda, detalle público, cotización, reserva idempotente, historial propio y cancelación con reembolso simulado. Los cobros reales y el envío real de correos no forman parte del MVP. Las tarifas, reglas de cálculo, privacidad y recuperación de disponibilidad se describen en [EP-05 y EP-06](docs/EP05-EP06-reservas-cancelaciones.md).

## Entrega 1 — Análisis de requerimientos y diseño

El acta de constitución está disponible en [Word](docs/entrega-1/acta-constitucion-marketplace.docx) y [Markdown](docs/entrega-1/acta-constitucion-marketplace.md); las fechas oficiales, la aprobación y las firmas siguen pendientes. La [documentación de Entrega 1](docs/entrega-1/README.md) reúne análisis, diseño, 19 artefactos, metodología y la [evidencia parcial de inspección y pruebas](docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md).

**Estado de los artefactos:** el paquete documental de Entrega 1 está integrado en `main` y publicado en el [espacio Confluence MARKET](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo); fue ratificado por Juan Sebastián Morales y Julián Aguilera el 14/10/2026; la validación docente y las verificaciones externas aún no constan. En el commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907), CI pasó ([run 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799)) y SonarCloud aprobó el Quality Gate ([run 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828)); la cobertura de código nuevo reportada en PR #8 fue 0.0 %. La auditoría de [PR #1–#9](docs/evidencias/revision-pull-requests-2026-10.md) detalla qué se integró y qué no. La ratificación del paquete ya está registrada. La retroalimentación real de participantes y las funciones/casos de negocio aún no implementados no se presentan como realizados. La guía no fija un mínimo de 44 casos para Entrega 1; la evidencia de negocio y producto funcional se amplía en Entrega 2, mientras los tests de historias ya implementadas deben correr en CI.

## Tecnologías base propuestas

- Backend: Java 21, Spring Boot y Maven.
- Persistencia: PostgreSQL.
- Frontend inicial: HTML, CSS y JavaScript nativo.
- CI: GitHub Actions.
- Análisis estático: SonarQube Cloud (SonarCloud), integrado en GitHub Actions; [Quality Gate aprobado en el PR #8](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517).

Las tecnologías son una propuesta técnica inicial incluida en la ratificación del paquete; su implementación y validación se evidencian por separado.

## Estructura

- backend/: API REST Java/Spring Boot y pruebas.
- frontend/: interfaz web inicial, validaciones y pruebas con Node.js.
- .github/workflows/: integración continua y workflow SonarCloud.
- docs/entrega-1/: documentación de análisis y diseño.
- docs/GITHUB_SETUP.md: configuración administrativa de GitHub y SonarCloud.
- docs/TRACEABILITY_TEMPLATE.csv: matriz inicial de trazabilidad.
- docs/DECISIONS.md: decisiones técnicas y asuntos por validar.
- docs/evidencias/: inspección, ejecución parcial de pruebas y auditoría de PR.
- scripts/: configuración auxiliar para reglas de protección vía GitHub CLI.

## Requisitos locales

Java 21, Maven, Node.js 22, npm, Docker Compose y Git.

## Ejecutar y verificar

Backend: mvn -B -f backend/pom.xml verify

Frontend: cd frontend && npm ci && npm run lint && npm test && npm run build

Base de datos local: docker compose up -d db

Para que los tokens sigan siendo válidos tras reiniciar el backend, configura `AUTH_TOKEN_SECRET` con al menos 32 bytes. `ADMIN_BOOTSTRAP_KEY` permite registrar una cuenta `ADMIN` de forma controlada; sin esa clave, el registro público solo admite `GUEST` y `HOST`. Define `CORS_ALLOWED_ORIGINS` si el frontend se sirve desde otro origen. La recuperación de cuenta solo registra la solicitud y devuelve el mismo mensaje exista o no la cuenta; no envía correo ni cambia contraseñas.

El backend expone autenticación y perfil, consulta de publicaciones, `POST /api/listings/{listingId}/quotes`, y el flujo protegido `/api/reservations` para confirmar, consultar y cancelar reservas. El frontend consume la búsqueda, el detalle, la cotización y el historial; ajusta la etiqueta `api-base-url` en `frontend/index.html` si el backend usa otra dirección.

## Catálogo y cuentas de demostración

El perfil opcional `demo` carga 5.000 alojamientos ficticios repartidos entre los 1.122 destinos territoriales del catálogo DIVIPOLA de 2024 (1.103 municipios, una isla y 18 áreas no municipalizadas), con tarifas en COP, capacidades, servicios y disponibilidad de muestra. También crea 1.001 cuentas: 500 huéspedes (`guest.001@marketplace.test` a `guest.500@marketplace.test`), 500 anfitriones (`host.001@marketplace.test` a `host.500@marketplace.test`) y un administrador (`demo-admin@marketplace.test`). Los nombres de territorios y departamentos se basan en el [catálogo DIVIPOLA de DANE publicado en Datos Abiertos Colombia](https://www.datos.gov.co/Mapas-Nacionales/DIVIPOLA-C-digos-municipios/gdxc-w37w); los alojamientos y las cuentas sí son ficticios. Los registros de muestra usan dominios `.test` y no deben copiarse a producción.

Para cargar estos datos en una base local, inicia PostgreSQL y ejecuta el backend con el perfil `demo` y una contraseña definida por ti. En PowerShell:

```powershell
docker compose up -d db
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:DEMO_SEED_PASSWORD = "una-clave-local-de-8-a-72-caracteres"
$env:AUTH_TOKEN_SECRET = "define-aqui-un-secreto-local-de-al-menos-32-caracteres"
mvn -f backend/pom.xml spring-boot:run
```

La contraseña configurada se aplica a las cuentas demo locales. El seeder corre solo con el perfil `demo`, es idempotente y puede apagarse con `DEMO_SEED_ENABLED=false`; sin `DEMO_SEED_PASSWORD` válido, el backend falla antes de escribir datos. Al abrir la página se muestran destinos sugeridos sin exigir inicio de sesión; el campo ciudad busca en los 1.122 destinos territoriales, tolera tildes y ofrece hasta diez opciones navegables con teclado. La consulta de alojamientos es pública; confirmar una reserva, consultar reservas y cancelarlas requiere una sesión de huésped.

## Flujo de trabajo

1. Asegura que la historia exista en Jira e incluya criterios que comiencen con «El sistema debe» y conserven el escenario Dado que / Cuando / Entonces.
2. Crea la rama de funcionalidad/corrección/documentación desde develop.
3. Realiza commits pequeños y coherentes, vinculando Jira cuando aplique.
4. Abre un Pull Request hacia develop con la plantilla incluida.
5. Otro integrante debe revisar el PR. Espera a que CI y los checks que estén configurados finalicen correctamente.
6. Integra a main mediante Pull Request cuando la versión esté estable.
7. Actualiza la matriz de trazabilidad con las claves/enlaces reales de Jira, commits, PR, pruebas y releases.

Consulta CONTRIBUTING.md y docs/entrega-1/13-checklist-entrega.md antes de preparar la entrega.

Consulta [la configuración de GitHub](docs/GITHUB_SETUP.md), el [backlog de Jira](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog) y la [documentación de Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo).
