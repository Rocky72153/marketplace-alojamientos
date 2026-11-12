# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, disponibilidad, cotización, reserva y cancelación de alojamientos temporales.

## Alcance implementado

La base funcional cubre registro e inicio de sesión con token, perfil, publicaciones, disponibilidad, búsqueda, detalle público, cotización, reserva idempotente, historial propio y cancelación con reembolso simulado. También incluye notificaciones internas sobre cambios de publicación y reservas, reseñas de estancias completadas y moderación de reseñas por administradores. Los cobros reales y el envío real de correos no forman parte del MVP. Las tarifas, reglas de cálculo, privacidad y recuperación de disponibilidad se describen en [EP-05 y EP-06](docs/EP05-EP06-reservas-cancelaciones.md); la implementación y pruebas de notificaciones y reseñas se describen en [EP-07 y EP-08](docs/EP07-EP08-EVIDENCE.md).

## Entrega 1 — Análisis de requerimientos y diseño

El acta de constitución está disponible en [Word](docs/entrega-1/acta-constitucion-marketplace.docx) y [Markdown](docs/entrega-1/acta-constitucion-marketplace.md); la entrega final es el 26/11/2026; la aprobación y las firmas siguen pendientes. La [documentación de Entrega 1](docs/entrega-1/README.md) reúne análisis, diseño, 19 artefactos, metodología y la [evidencia parcial de inspección y pruebas](docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md).

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
- docs/EP07-EP08-EVIDENCE.md: alcance y comprobaciones de notificaciones y reseñas.
- docs/EP09-administracion-reportes.md: reglas del panel, administración, exportaciones y evidencias.
- docs/evidencias/: inspección, ejecución parcial de pruebas y auditoría de PR.
- scripts/: configuración auxiliar para reglas de protección vía GitHub CLI.

## Requisitos locales

Java 21, Maven, Node.js 22, npm, Docker Compose y Git.

## Ejecutar y verificar

Backend: mvn -B -f backend/pom.xml verify

Frontend: cd frontend && npm ci && npm run lint && npm test && npm run build

Base de datos local: docker compose up -d db

Para que los tokens sigan siendo válidos tras reiniciar el backend, configura `AUTH_TOKEN_SECRET` con al menos 32 bytes. `ADMIN_BOOTSTRAP_KEY` permite registrar una cuenta `ADMIN` de forma controlada; sin esa clave, el registro público solo admite `GUEST` y `HOST`. Define `CORS_ALLOWED_ORIGINS` si el frontend se sirve desde otro origen. La recuperación de cuenta solo registra la solicitud y devuelve el mismo mensaje exista o no la cuenta; no envía correo ni cambia contraseñas.

El backend expone autenticación y perfil, consulta de publicaciones, `POST /api/listings/{listingId}/quotes`, y el flujo protegido `/api/reservations` para confirmar, consultar y cancelar reservas. También ofrece notificaciones privadas, reseñas públicas y moderación protegida para administradores. El frontend consume estos flujos; ajusta la etiqueta `api-base-url` en `frontend/index.html` si el backend usa otra dirección.

## Catálogo y cuentas de demostración

El perfil predeterminado `local` carga 10.000 alojamientos ficticios repartidos entre los 1.122 destinos territoriales del catálogo DIVIPOLA de 2024 (1.103 municipios, una isla y 18 áreas no municipalizadas), con tarifas en COP, capacidades, servicios y disponibilidad de muestra. Solo admite carga automática en PostgreSQL de `localhost` o `127.0.0.1`. También crea 1.001 cuentas ficticias: 500 huéspedes (`guest.001@marketplace.test` a `guest.500@marketplace.test`), 500 anfitriones (`host.001@marketplace.test` a `host.500@marketplace.test`) y un administrador (`demo-admin@marketplace.test`). Los nombres de territorios y departamentos se basan en el [catálogo DIVIPOLA de DANE publicado en Datos Abiertos Colombia](https://www.datos.gov.co/Mapas-Nacionales/DIVIPOLA-C-digos-municipios/gdxc-w37w); los alojamientos y las cuentas no representan ofertas ni personas reales. Los datos de muestra no deben copiarse a producción.

Para cargar estos datos en una base local, inicia PostgreSQL y ejecuta el backend. Si deseas iniciar sesión en las cuentas de demostración, define la contraseña **antes de la primera carga**. En PowerShell:

```powershell
docker compose up -d db
$env:DEMO_SEED_PASSWORD = "una-clave-local-de-8-a-72-caracteres"
$env:AUTH_TOKEN_SECRET = "define-aqui-un-secreto-local-de-al-menos-32-caracteres"
mvn -f backend/pom.xml spring-boot:run
```

La contraseña se usa solo al crear cuentas demo; nunca reemplaza contraseñas ni modifica cuentas ya existentes. Si no defines `DEMO_SEED_PASSWORD`, se asigna una clave aleatoria desconocida y no podrás iniciar sesión en esas cuentas de muestra; puedes registrar tu propia cuenta normalmente. Si después configuras una contraseña, las cuentas demo existentes no cambian. El cargador es idempotente y conserva los datos. Cuando identifica con seguridad el catálogo anterior de 5.000, registra como omitidas las entradas antiguas editadas, eliminadas o ambiguas para no recrearlas, y agrega las nuevas: el total puede quedar por debajo de 10.000. Si no puede identificar el catálogo anterior con suficiente certeza, detiene la carga sin modificar los datos. `DEMO_SEED_ENABLED=false` la desactiva y `DEMO_SEED_LISTING_COUNT` permite cambiar la cantidad. Fuera del entorno local, configura explícitamente otro perfil y credenciales apropiadas; el perfil opcional `demo` también activa la carga de ejemplo y solo debe usarse con una base de pruebas.

Al abrir la página se muestran destinos sugeridos sin exigir inicio de sesión; el campo ciudad busca en los 1.122 destinos territoriales, tolera tildes y ofrece hasta diez opciones navegables con teclado. La consulta de alojamientos es pública; confirmar una reserva, consultar reservas y cancelarlas requiere una sesión de huésped.

## Flujo de trabajo

1. Asegura que la historia exista en Jira e incluya criterios que comiencen con «El sistema debe» y conserven el escenario Dado que / Cuando / Entonces.
2. Crea la rama de funcionalidad/corrección/documentación desde develop.
3. Realiza commits pequeños y coherentes, vinculando Jira cuando aplique.
4. Abre un Pull Request hacia develop con la plantilla incluida.
5. Otro integrante debe revisar el PR. Espera a que CI y los checks que estén configurados finalicen correctamente.
6. Integra a main mediante Pull Request cuando la versión esté estable.
7. Actualiza la matriz de trazabilidad con las claves/enlaces reales de Jira, commits, PR, pruebas y releases.

Consulta CONTRIBUTING.md y docs/entrega-1/13-checklist-entrega.md antes de preparar la entrega.

## Enlaces del proyecto

| Herramienta | Enlace |
|---|---|
| Confluence (documentación por fases) | https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview |
| Jira: sprint activo | https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2 |
| Jira: backlog | https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog |
| Jira: reportes (burndown y velocidad) | https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/reports |
| Estado de la Entrega 1 en Confluence | https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665 |
| SonarCloud | https://sonarcloud.io/dashboard?id=Rocky72153_marketplace-alojamientos |

Registra en cada historia los enlaces a sus PR y evidencias conforme estén disponibles. Para la configuración del repositorio consulta [GitHub Setup](docs/GITHUB_SETUP.md).
