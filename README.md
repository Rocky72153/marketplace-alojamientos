# Marketplace de Alojamientos

Proyecto de curso de Ingeniería de Software I — Universidad El Bosque, periodo 2026-2.

Marketplace web para conectar anfitriones con huéspedes y permitir publicación, búsqueda, disponibilidad, cotización, reserva, cancelación y gestión administrativa.

## Entrega 1 — Análisis de requerimientos y diseño

La documentación versionada de la entrega 1 se encuentra en [docs/entrega-1/README.md](docs/entrega-1/README.md). Incluye visión/alcance, actores/interesados, procesos de negocio y fuente BPMN 2.0, catálogo de requerimientos/reglas, backlog de historias con criterios de aceptación, casos de uso, modelos de dominio y datos, arquitectura orientada a servicios, contrato OpenAPI, prototipo, cronograma/WBS/riesgos, secuencias y checklist contra la guía del curso.

**Estado de los artefactos:** son una propuesta inicial basada en el enunciado del curso. Deben ser revisados y aprobados por el equipo; Jira, Confluence, reglas administrativas y SonarCloud requieren configuración/evidencia real antes de marcarlos como completos.

## Tecnologías base propuestas

- Backend: Java 21, Spring Boot y Maven.
- Persistencia: PostgreSQL.
- Frontend inicial: HTML, CSS y JavaScript nativo.
- CI: GitHub Actions.
- Análisis estático: SonarQube Cloud (SonarCloud), pendiente de vincular credenciales/variables.

Las tecnologías son una propuesta técnica inicial y deben confirmarse con el equipo.

## Estructura

- backend/: API REST Java/Spring Boot y pruebas.
- frontend/: interfaz web inicial, validaciones y pruebas con Node.js.
- .github/workflows/: integración continua y workflow SonarCloud.
- docs/entrega-1/: documentación de análisis y diseño.
- docs/GITHUB_SETUP.md: configuración administrativa de GitHub y SonarCloud.
- docs/TRACEABILITY_TEMPLATE.csv: matriz inicial de trazabilidad.
- docs/DECISIONS.md: decisiones técnicas y asuntos por validar.
- scripts/: configuración auxiliar para reglas de protección vía GitHub CLI.

## Requisitos locales

Java 21, Maven, Node.js 22, npm, Docker Compose y Git.

## Ejecutar y verificar

Backend: mvn -B -f backend/pom.xml verify

Frontend: cd frontend && npm ci && npm run lint && npm test && npm run build

Base de datos local: docker compose up -d db

## Flujo de trabajo

1. Asegura que la historia exista en Jira e incluya criterios Dado que / Cuando / Entonces.
2. Crea la rama de funcionalidad/corrección/documentación desde develop.
3. Realiza commits pequeños y coherentes, vinculando Jira cuando aplique.
4. Abre un Pull Request hacia develop con la plantilla incluida.
5. Otro integrante debe revisar el PR. Espera a que CI y los checks que estén configurados finalicen correctamente.
6. Integra a main mediante Pull Request cuando la versión esté estable.
7. Actualiza la matriz de trazabilidad con las claves/enlaces reales de Jira, commits, PR, pruebas y releases.

## Pendientes de configuración externa

- Verificar visibilidad del repositorio y agregar colaboradores reales.
- Proteger main y develop mediante Rulesets/Branch protection con revisión y checks reales.
- Crear el proyecto SonarCloud, configurar las variables SONAR_ORGANIZATION y SONAR_PROJECT_KEY y guardar SONAR_TOKEN como secreto de Actions.
- Cargar las épicas e historias en Jira y publicar/enlazar la documentación en Confluence.
- Sustituir cronograma relativo por fechas de la asignatura; asignar responsables y sprints reales.
- Capturar evidencia auténtica de validación, revisiones, PR y uso de IA conforme a la guía.

Consulta CONTRIBUTING.md y docs/entrega-1/13-checklist-entrega.md antes de preparar la entrega.