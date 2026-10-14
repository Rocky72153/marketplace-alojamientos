# Entrega 1 — Análisis de requerimientos y diseño

**Proyecto:** Marketplace de alojamientos temporales para Inversiones LR  
**Curso:** Ingeniería de Software I · Ingeniería de Sistemas · Universidad El Bosque · 2026-2  
**Estado:** paquete documental preparado para revisión; las decisiones abiertas y evidencias externas están identificadas, no se presumen aprobadas.

## Navegación de artefactos

1. [Visión, objetivos y alcance](01-vision-alcance.md)
2. [Actores e interesados](02-actores-interesados.md)
3. [Procesos de negocio](03-procesos-negocio.md)
4. [Catálogo de requerimientos y reglas](04-catalogo-requerimientos.md)
5. [Épicas e historias de usuario](05-backlog-historias.md)
6. [Casos de uso](06-casos-de-uso.md)
7. [Modelo de dominio](07-modelo-dominio.md)
8. [Modelo lógico de datos](08-modelo-datos.md)
9. [Arquitectura orientada a servicios](09-arquitectura-servicios.md)
10. [Contrato REST OpenAPI](10-openapi.yaml)
11. [Prototipo y estrategia de validación](11-prototipo-validacion.md)
12. [Planificación, WBS, cronograma relativo y riesgos](12-planificacion-riesgos.md)
13. [Checklist contra la guía](13-checklist-entrega.md)
14. [Diagramas de secuencia](14-secuencias.md)

## Fuentes de diagramas y material complementario

- BPMN 2.0 de publicación: [bpmn/publicacion-alojamiento.bpmn](bpmn/publicacion-alojamiento.bpmn).
- BPMN 2.0 de búsqueda/reserva: [bpmn/busqueda-reserva-cancelacion.bpmn](bpmn/busqueda-reserva-cancelacion.bpmn).
- BPMN 2.0 de cancelación: [bpmn/cancelacion-reserva.bpmn](bpmn/cancelacion-reserva.bpmn).
- Matriz de trazabilidad inicial: [../TRACEABILITY_TEMPLATE.csv](../TRACEABILITY_TEMPLATE.csv).
- Registro de decisiones/preguntas abiertas: [../DECISIONS.md](../DECISIONS.md).
- Guía de ejecución local: [../LOCAL_SETUP.md](../LOCAL_SETUP.md).
- Evidencias que debe recopilar el equipo: [../evidencias/README.md](../evidencias/README.md).

## Cómo interpretar este material

Los artefactos se construyeron a partir de la guía oficial del curso compartida en el proyecto. Las decisiones que el enunciado deja abiertas —por ejemplo, tecnologías, límites físicos de servicios, tarifas, política concreta de cancelación, metodología y fechas— se marcan como propuestas o pendientes de validación y no deben presentarse como acuerdos ya tomados por el grupo.

Los identificadores se mantienen consistentes: ACT, INT, RN, RF, RNF, EP, HU, CU y ADR. Las historias tienen criterios de aceptación en formato **Dado que / Cuando / Entonces** y dependencias identificadas. La matriz CSV enlaza las claves reales de Jira y el PR de Entrega 1; los enlaces a pruebas de implementación, evidencias y release se agregan conforme existan.

## Validación y evidencia pendiente

- Aprobación/revisión de los artefactos por integrantes y docente.
- Completar en Jira las estimaciones acordadas, responsables, estados de trabajo y sprints/iteraciones reales; el backlog y sus claves están en la matriz sincronizada.
- Publicar o enlazar los modelos aprobados en Confluence, como pide el enunciado.
- Abrir los BPMN en un modelador compatible, validar reglas y exportar capturas/diagramas legibles para Confluence si se requiere.
- Configurar protección de ramas, revisar un PR real con otro integrante, verificar checks CI y configurar SonarCloud.
- Capturar evidencia auténtica de feedback del prototipo y del uso de IA con fecha/hora y prompts completos, conforme a la guía.

No se incluyen capturas ficticias ni se declara una validación que todavía no ha ocurrido.