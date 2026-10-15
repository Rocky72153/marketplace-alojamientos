# 19. Metodología de trabajo: Scrum + Lean

**Proyecto:** Marketplace de Alojamientos  
**Fecha de registro:** 2026-10-14  
**Cadencia de planificación en Jira:** sprints de una semana, desde el 19 de octubre de 2026  
**Estado:** propuesta de trabajo documentada y reflejada parcialmente en Jira; el equipo debe ratificarla explícitamente antes de tratar roles, cadencia y capacidad como acuerdos vigentes.

## 19.1 Decisión metodológica

El proyecto utilizará un enfoque combinado:

- **Scrum** será el marco de organización: backlog priorizado, objetivo por sprint, planificación, seguimiento, revisión del incremento y retrospectiva.
- **Lean** aportará principios de flujo y mejora: maximizar valor, eliminar trabajo que no aporta valor, limitar trabajo en curso (WIP), reducir tiempos de espera y retrabajo, incorporar calidad desde el principio y mejorar continuamente.

Lean no se adopta como sinónimo de “trabajar más rápido” ni como justificación para reducir pruebas. Se busca entregar valor de forma más predecible, con menos bloqueos, cambios innecesarios y defectos.

## 19.2 Roles y responsabilidades

| Rol | Responsable | Responsabilidades |
|---|---|---|
| Product Owner / analista de requisitos | Julián David Aguilera Párraga | Aclarar necesidades y reglas de negocio; mantener criterios de aceptación verificables; apoyar la priorización y revisar si el incremento satisface esos criterios. |
| Scrum Master / gerente del proyecto | Juan Sebastián Morales Forero | Facilitar la aplicación de Scrum; hacer visibles bloqueos y dependencias; promover acciones de mejora Lean; coordinar que acuerdos, revisiones y retrospectivas queden registrados. No actúa como jefe del equipo. |
| Developers | Juan Sebastián Morales Forero y Julián David Aguilera Párraga | Diseñar, implementar, probar, revisar código, mantener documentación y trazabilidad, estimar trabajo y producir un incremento que cumpla la Definition of Done. |

La asignación propuesta en esta tabla sigue la información del acta de constitución; su ratificación conjunta sigue pendiente. Los responsables de historias o subtareas en Jira no se toman por sí solos como evidencia de que el equipo haya aprobado los roles.
| Interesado / representante de negocio | Representante de Inversiones LR simulado en el contexto académico | Aclarar necesidades y revisar prototipos o incrementos cuando esté disponible. El feedback debe registrarse solo cuando realmente ocurra; no se presume validación del cliente. |

Los dos integrantes participan en desarrollo y pruebas. Para fomentar calidad y revisión cruzada, la subtarea de pruebas se asigna al integrante distinto de quien implementa la historia, conforme a la distribución registrada en Jira. La asignación y disponibilidad deben revisarse si hay ausencias o bloqueos.

## 19.3 Cadencia de trabajo

Los sprints están configurados en Jira con inicio y fin los lunes a las 09:00, hora de Bogotá. El final de uno coincide con el inicio del siguiente. La tabla resume la distribución vigente en el tablero; los Story Points son estimaciones iniciales, no una velocidad comprobada.

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
| **Total** | **8 semanas** | **Alcance completo planificado** | **HU-01 a HU-19** | **106** |

La capacidad conocida es de aproximadamente 4–6 horas semanales por integrante (8–12 horas combinadas). Como no existe aún una velocidad histórica, no puede afirmarse que 106 puntos sean alcanzables en ocho semanas. Antes de comprometer cada sprint, el equipo revisará disponibilidad, dependencias y tamaño de historias; después del primer sprint calibrará el plan según trabajo realmente terminado. Los sprints con 16 puntos son especialmente sensibles a esa validación. HU-10 tiene 13 puntos iniciales y debe refinarse para verificar que sea terminable dentro del sprint; las subtareas por sí solas no sustituyen dividir una historia excesivamente grande.

## 19.4 Ceremonias ligeras y salidas obligatorias

| Actividad | Cadencia propuesta | Salida que se registra |
|---|---|---|
| Sprint Planning | Al inicio de cada semana; 45–60 min | Objetivo, historias seleccionadas según capacidad, riesgos, dependencias y compromiso revisado en Jira. |
| Seguimiento de flujo | Asíncrono al menos 3 veces por semana; breve reunión solo cuando ayude | Estado actualizado, próximo paso, impedimentos, responsable de resolverlos y fecha de seguimiento en Jira. No afirmar que se celebró una reunión si solo hubo actualización asíncrona. |
| Backlog Refinement | Una sesión semanal de 30 min, antes de la siguiente planificación | Historias entendibles, criterios verificables, preguntas resueltas y estimaciones revisadas. |
| Sprint Review | Al cierre de la semana; 30 min | Demostración real frente a criterios de aceptación; historias aceptadas/devueltas y feedback real registrado. |
| Sprint Retrospective | Al cierre, después de Review; 20–30 min | Una o dos acciones concretas, con responsable y fecha objetivo para el siguiente sprint. |
| Revisión de PR e integración | Durante el sprint | Revisor distinto del autor cuando sea posible; CI y pruebas pertinentes verificadas; trazabilidad actualizada antes del merge. |

Las duraciones son una propuesta compatible con la dedicación semanal reducida. El equipo puede ajustarlas, pero debe conservar la finalidad de cada actividad y documentar los acuerdos reales.

## 19.5 Prácticas Lean concretas

1. **Priorizar valor y riesgo:** trabajar primero en dependencias de registro/permisos, publicaciones, disponibilidad, cotización y reserva; no comenzar funcionalidades opcionales que desplacen los flujos centrales.
2. **Limitar WIP:** con dos Developers, mantener como máximo dos historias activas de implementación en el equipo; las historias bloqueadas o pendientes de revisión se hacen visibles y se atienden antes de abrir trabajo adicional.
3. **Reducir espera y handoffs:** abrir PR pequeños y coherentes; pedir revisión tan pronto el cambio sea revisable; hacer visible quién debe responder y cuál es el siguiente paso.
4. **Evitar retrabajo:** aclarar reglas y criterios antes de codificar; enlazar las decisiones a las historias y actualizar los documentos afectados dentro del mismo cambio.
5. **Calidad integrada:** escribir y ejecutar pruebas pertinentes, revisar criterios de aceptación y mantener CI en verde; no retrasar pruebas para ganar velocidad aparente.
6. **Mejora continua:** elegir una o dos acciones realistas en cada retrospectiva, asignar responsable y comprobar la semana siguiente si mejoraron el flujo.
7. **Evitar trabajo innecesario:** no duplicar documentación en varios lugares sin una fuente de verdad; enlazar artefactos y conservar trazabilidad útil para la entrega.

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

## 19.7 Métricas de flujo y revisión semanal

Al cierre de cada sprint, registrar con datos del tablero y del repositorio:

| Métrica | Cómo observarla | Para qué se utiliza |
|---|---|---|
| Trabajo terminado | Historias aceptadas y puntos realmente terminados; reportar también lo no terminado | Comparar con lo planificado sin convertir la estimación inicial en promesa. |
| Cycle time | Tiempo transcurrido entre empezar una historia y terminarla | Encontrar colas o historias demasiado grandes; comparar tendencias, no evaluar personas. |
| WIP y bloqueos | Historias activas, bloqueadas o esperando revisión; anotar duración y causa | Resolver impedimentos y evitar empezar más trabajo del que se puede terminar. |
| Calidad / retrabajo | Pruebas ejecutadas y resultado, defectos encontrados después de review, correcciones repetidas | Reducir defectos y validar que velocidad no se gane a costa de calidad. |
| Cumplimiento del objetivo | Objetivo de sprint alcanzado total/parcial/no alcanzado, con causa documentada | Ajustar alcance, capacidad y dependencias para la siguiente semana. |

En el primer sprint se establece una línea base. No se fijan objetivos numéricos de velocidad o cycle time antes de observar datos reales. Los Story Points sirven para planificación de equipo; no son horas ni una métrica individual de productividad.

## 19.8 Registro y mejora del proceso

Jira será la fuente de seguimiento del backlog, responsables, estados, objetivos y sprints. GitHub será la fuente de cambios versionados, PR y CI; la documentación de Entrega 1 enlazará decisiones y evidencia. Al final de cada sprint se conservarán el resultado de la review, las acciones de retrospectiva y los cambios de alcance/capacidad, distinguiendo siempre planes de resultados verificados.

La aplicación de este marco queda sujeta a ratificación explícita del equipo y a los requisitos docentes. Las reuniones, revisiones, feedback y métricas solo se marcarán como realizados cuando exista evidencia real.
