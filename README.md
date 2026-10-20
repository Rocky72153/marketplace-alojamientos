# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, disponibilidad, cotización, reserva, cancelación y gestión administrativa.

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

## Flujo de trabajo

1. Asegura que la historia exista en Jira e incluya criterios que comiencen con «El sistema debe» y conserven el escenario Dado que / Cuando / Entonces.
2. Crea la rama de funcionalidad/corrección/documentación desde develop.
3. Realiza commits pequeños y coherentes, vinculando Jira cuando aplique.
4. Abre un Pull Request hacia develop con la plantilla incluida.
5. Otro integrante debe revisar el PR. Espera a que CI y los checks que estén configurados finalicen correctamente.
6. Integra a main mediante Pull Request cuando la versión esté estable.
7. Actualiza la matriz de trazabilidad con las claves/enlaces reales de Jira, commits, PR, pruebas y releases.

## Pendientes de configuración externa

- Verificar visibilidad del repositorio y agregar colaboradores reales.
- Proteger main y develop mediante Rulesets/Branch protection con revisión y checks reales.
- Calidad de CI y SonarCloud verificada en main, commit [e4d192e](https://github.com/Rocky72153/marketplace-alojamientos/commit/e4d192e8054484174b80a5820131a419da55e907): [CI 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799) y [Quality Gate 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828); la cobertura de código nuevo de PR #8 fue 0.0 %.
- Ratificar en Jira las estimaciones y capacidad; MARKET-68 y MARKET-69 tienen responsables pero siguen sin sprint. La documentación de Entrega 1 ya está enlazada en el [índice de Confluence MARKET](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo).
- Sustituir cronograma relativo por fechas de la asignatura; asignar responsables y sprints reales.
- Completar sesiones de validación con participantes reales; la inspección técnica y sus límites están en `docs/evidencias/validacion-prototipo-y-pruebas-2026-10-14.md`.
- Recopilar capturas auténticas de uso de IA con fecha/hora y prompts completos; la plantilla no sustituye esas capturas.

Consulta CONTRIBUTING.md y docs/entrega-1/13-checklist-entrega.md antes de preparar la entrega.

Links: 
https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/pages/753665/Entrega+1+estado+evidencia+y+ratificaci+n+del+equipo
https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog
