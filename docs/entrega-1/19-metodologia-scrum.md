# 19. Metodología de trabajo: Scrum

**Proyecto:** Marketplace de Alojamientos  
**Fecha de registro:** 2026-10-14 (actualizado el 2026-11-04: el equipo trabaja solo con Scrum)  
**Cadencia de planificación en Jira:** sprints de una semana, desde el 19 de octubre de 2026  
**Estado:** marco documentado y reflejado parcialmente en Jira. Los roles quedaron definidos el 08/11/2026; la cadencia y la capacidad se confirman con la ejecución de los sprints.

## 19.1 Decisión metodológica

El proyecto utiliza **Scrum** como marco de trabajo: Product Backlog priorizado, objetivo por sprint, Sprint Planning, Daily Scrum, Sprint Review y Sprint Retrospective. Scrum se apoya en tres pilares:

- **Transparencia:** el estado del backlog, del sprint y del incremento se mantiene visible en Jira y GitHub.
- **Inspección:** en la Daily Scrum, la Sprint Review y la Sprint Retrospective se revisa el avance contra el objetivo del sprint y los criterios de aceptación.
- **Adaptación:** cuando algo se desvía, se ajusta el plan del sprint o el Product Backlog y se registra la decisión.

## 19.2 Roles y responsabilidades

| Rol | Responsable | Responsabilidades |
|---|---|---|
| Product Owner | Juan Sebastián Morales Forero | Ordenar y priorizar el Product Backlog; aclarar necesidades y reglas de negocio; mantener criterios de aceptación verificables y revisar si el incremento satisface esos criterios. |
| Scrum Master / gerente del proyecto | Julián David Aguilera Párraga | Facilitar la aplicación de Scrum; hacer visibles bloqueos y dependencias; promover las acciones de mejora acordadas en la retrospectiva; coordinar que acuerdos, revisiones y retrospectivas queden registrados. No actúa como jefe del equipo. |
| Developers | Juan Sebastián Morales Forero y Julián David Aguilera Párraga | Diseñar, implementar, probar, revisar código, mantener documentación y trazabilidad, estimar trabajo y producir un incremento que cumpla la Definition of Done. |
| Interesado / representante de negocio | Representante de Inversiones LR simulado en el contexto académico | Aclarar necesidades y revisar prototipos o incrementos cuando esté disponible. El feedback debe registrarse solo cuando realmente ocurra; no se presume validación del cliente. |

Esta asignación es la misma del documento 12 y del acta de constitución; se unificó el 08/11/2026. Como los dos integrantes también son Developers, los responsables de historias y subtareas en Jira no dependen del rol de Scrum de cada uno.

Los dos integrantes participan en desarrollo y pruebas. Para fomentar calidad y revisión cruzada, la subtarea de pruebas se asigna al integrante distinto de quien implementa la historia, conforme a la distribución registrada en Jira. La asignación y disponibilidad deben revisarse si hay ausencias o bloqueos.

## 19.3 Cadencia de trabajo

Los sprints están configurados en Jira con inicio y fin los lunes a las 09:00, hora de Bogotá. El final de uno coincide con el inicio del siguiente. La tabla es el plan inicial del 14/10/2026; los Story Points son estimaciones iniciales, no una velocidad comprobada. La ejecución real se registra en Jira y en Confluence (página 4.3 Sprints): los sprints 1 a 3 quedaron cerrados con 37, 3 y 0 puntos terminados, y HU-09 a HU-12 pasaron al Sprint 4.

| Sprint en Jira | Fechas locales de referencia | Objetivo | Historias | Puntos iniciales |
|---|---|---|---|---:|
| MARKET Sprint 1 | 19–26 oct 2026 | Identidad, recuperación y perfil inicial | HU-01 a HU-03 | 11 |
| MARKET Sprint 2 | 26 oct–2 nov 2026 | Crear, mantener y moderar publicaciones | HU-04 a HU-05 | 13 |
| MARKET Sprint 3 | 2–9 nov 2026 | Disponibilidad y búsqueda de alojamientos | HU-06 a HU-07 | 13 |
| MARKET Sprint 4 | 9–16 nov 2026 | Detalle de alojamiento y cotización | HU-08 a HU-09 | 8 |
| MARKET Sprint 5 | 16–23 nov 2026 | Crear y consultar reservas | HU-10 a HU-11 | 16 |
| MARKET Sprint 6 | 23–30 nov 2026 | Cancelación, notificaciones y reseñas | HU-12 a HU-14 | 16 |
| MARKET Sprint 7 | 30 nov–7 dic 2026 | Moderación, administración y dashboard | HU-15 a HU-17 | 16 |
| MARKET Sprint 8 | 7–14 dic 2026 | Exportación de reportes y trazabilidad técnica | HU-18 a HU-19 | 13 |
| **Total** | **8 semanas** | **Backlog completo propuesto; MVP obligatorio definido por MoSCoW** | **HU-01 a HU-19** | **106** |

La capacidad conocida es de aproximadamente 4–6 horas semanales por integrante (8–12 horas combinadas). Como no existe aún una velocidad histórica, no puede afirmarse que 106 puntos sean alcanzables en ocho semanas. Antes de comprometer cada sprint, el equipo revisará disponibilidad, dependencias y tamaño de historias; después del primer sprint calibrará el plan según trabajo realmente terminado. Los sprints con 16 puntos son especialmente sensibles a esa validación. HU-10 tiene 13 puntos iniciales y debe refinarse para verificar que sea terminable dentro del sprint; las subtareas por sí solas no sustituyen dividir una historia excesivamente grande.

## 19.4 Eventos de Scrum

| Evento | Cadencia propuesta | Salida que se registra |
|---|---|---|
| Sprint | Una semana, de lunes a lunes | Contiene los demás eventos y produce un incremento que cumple la Definition of Done. |
| Sprint Planning | Al inicio de cada semana; 45-60 min | Objetivo del sprint, historias seleccionadas según capacidad, riesgos, dependencias y compromiso revisado en Jira. |
| Daily Scrum | Asíncrona al menos 3 veces por semana en Jira; reunión breve solo cuando ayude | Qué se avanzó, qué sigue e impedimentos, con responsable de resolverlos. No afirmar que se celebró una reunión si solo hubo actualización asíncrona. |
| Sprint Review | Al cierre de la semana; 30 min | Demostración real frente a criterios de aceptación; historias aceptadas o devueltas y feedback real registrado. |
| Sprint Retrospective | Al cierre, después de la Review; 20-30 min | Una o dos acciones concretas de mejora, con responsable y fecha objetivo para el siguiente sprint. |

Además del ciclo de eventos, el equipo realiza dos actividades continuas:

- **Refinamiento del Product Backlog:** una sesión semanal de 30 min antes de la siguiente planificación, para dejar historias entendibles, criterios verificables, preguntas resueltas y estimaciones revisadas.
- **Revisión de PR e integración:** durante el sprint, con revisor distinto del autor cuando sea posible, CI y pruebas pertinentes verificadas y trazabilidad actualizada antes del merge.

Las duraciones son una propuesta compatible con la dedicación semanal reducida. El equipo puede ajustarlas, pero debe conservar la finalidad de cada evento y documentar los acuerdos reales.

## 19.5 Artefactos de Scrum y compromisos

| Artefacto | Compromiso | Dónde se gestiona |
|---|---|---|
| Product Backlog | Objetivo del producto: un MVP que permita publicar un alojamiento, buscarlo, consultar su disponibilidad, cotizarlo y reservarlo de forma inmediata. | Jira MARKET: épicas e historias ordenadas por prioridad MoSCoW. |
| Sprint Backlog | Objetivo del sprint definido en la Sprint Planning. | Sprint activo en Jira, con historias y subtareas. |
| Incremento | Definition of Done (sección 19.6). | Rama `develop` y releases en GitHub, con CI en verde. |

El Product Owner ordena el Product Backlog por valor y riesgo: primero registro y permisos, publicaciones, disponibilidad, cotización y reserva; las funcionalidades opcionales no desplazan los flujos centrales. Las decisiones de cambio de alcance se enlazan a las historias afectadas.

## 19.6 Definition of Ready y Definition of Done

**Definition of Ready (DoR):** una historia puede entrar en planificación cuando tiene objetivo comprensible, prioridad, criterios de aceptación en formato verificable, dependencias identificadas, preguntas críticas resueltas o explícitamente registradas, estimación revisada por el equipo y tamaño razonable para una semana de capacidad disponible.

**Definition of Done (DoD):** una historia solo se considera terminada cuando:
- cumple sus criterios de aceptación demostrados;
- el código y las pruebas pertinentes están en el repositorio y pasan las verificaciones aplicables;
- otro integrante revisó el PR cuando sea posible y se resolvieron sus comentarios;
- la documentación, contrato OpenAPI y matriz de trazabilidad se actualizaron si corresponde;
- no hay defectos conocidos que impidan el flujo aceptado;
- la evidencia refleja ejecución real, no casos de prueba meramente diseñados.

La creación de una historia, subtarea, PR o caso de prueba no equivale a trabajo terminado ni a prueba aprobada.

## 19.7 Seguimiento del sprint

Al cierre de cada sprint, registrar con datos del tablero y del repositorio:

| Medida | Cómo observarla | Para qué se utiliza |
|---|---|---|
| Trabajo terminado (velocidad) | Historias aceptadas y puntos realmente terminados; reportar también lo no terminado | Comparar con lo planificado sin convertir la estimación inicial en promesa. |
| Burndown del sprint | Gráfico de trabajo restante del sprint en Jira | Detectar a tiempo si el objetivo del sprint está en riesgo. |
| Impedimentos | Bloqueos registrados en la Daily Scrum, con duración y causa | Resolverlos y llevar los recurrentes a la retrospectiva. |
| Calidad / retrabajo | Pruebas ejecutadas y resultado, defectos encontrados después de la review, correcciones repetidas | Reducir defectos y validar que la velocidad no se gane a costa de la calidad. |
| Cumplimiento del objetivo | Objetivo del sprint alcanzado total, parcial o no alcanzado, con causa documentada | Ajustar alcance, capacidad y dependencias para la siguiente semana. |

En el primer sprint se establece una línea base. No se fijan objetivos numéricos de velocidad antes de observar datos reales. Los Story Points sirven para planificación de equipo; no son horas ni una métrica individual de productividad.

## 19.8 Registro y mejora del proceso

Jira será la fuente de seguimiento del backlog, responsables, estados, objetivos y sprints. GitHub será la fuente de cambios versionados, PR y CI; la documentación de Entrega 1 enlazará decisiones y evidencia. Al final de cada sprint se conservarán el resultado de la review, las acciones de retrospectiva y los cambios de alcance/capacidad, distinguiendo siempre planes de resultados verificados.

La aplicación de este marco queda sujeta a ratificación explícita del equipo y a los requisitos docentes. Las reuniones, revisiones, feedback y métricas solo se marcarán como realizados cuando exista evidencia real.
