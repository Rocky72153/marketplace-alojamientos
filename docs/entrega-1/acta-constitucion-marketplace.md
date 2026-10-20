# Acta de Constitución del Proyecto

## Marketplace de Alojamientos

**Fecha de elaboración:** 14/10/2026  
**Estado:** Borrador para validación y aprobación. Las fechas de entrega, aprobaciones y firmas siguen pendientes.

## Historial de versiones

| Fecha | Versión | Autores | Organización | Descripción |
|---|---|---|---|---|
| 14/10/2026 | 1.0 | Julián Aguilera y Juan Sebastián Morales | Universidad El Bosque | Versión inicial del acta de constitución |

## Información del proyecto

| Dato | Descripción |
|---|---|
| Empresa u organización | Universidad El Bosque, Programa de Ingeniería de Sistemas, periodo académico 2026-2 |
| Nombre del proyecto | Marketplace de Alojamientos |
| Cliente | Inversiones LR |
| Patrocinador principal | Ing. David Esteban Rodríguez Gómez, docente de Ingeniería de Software I |
| Gerente del proyecto | Juan Sebastián Morales; rol de Scrum Master propuesto y pendiente de ratificación |
| Estado del acta | Borrador para validación y aprobación; no cuenta con firmas |

## Propósito y justificación

El proyecto propone desarrollar un marketplace web para Inversiones LR que conecte a anfitriones de alojamientos temporales con huéspedes que buscan descubrir, comparar y reservar opciones. La plataforma centralizará la publicación, búsqueda, disponibilidad, cotización, reserva y cancelación.

El crecimiento de huéspedes y propietarios que confían la gestión de sus inmuebles hace necesario reducir la dependencia de procesos manuales y mejorar la trazabilidad de publicaciones, reservas y cancelaciones. Una fuente central de disponibilidad ayudará a evitar reservas duplicadas y a coordinar la oferta con la demanda.

## Requerimientos del producto

El backlog de Entrega 1 documenta 19 historias del producto, pero no todas bloquean el MVP. El corte propuesto es 11 MUST (HU-01, HU-04–HU-12 y HU-19), 7 SHOULD (HU-02, HU-03, HU-13–HU-17) y 1 COULD (HU-18). Las prioridades identifican alcance planificado, no implementación actual. El levantamiento disponible es análisis documental de la guía oficial del proyecto y de sus artefactos; no se registran entrevistas ni aprobación del cliente. La relación conserva la cobertura funcional:

| Historia | Prioridad | Alcance |
|---|---|---|
| HU-01 | MUST | Registro e inicio de sesión para huéspedes, anfitriones y administradores, con permisos según rol |
| HU-02 | SHOULD | Recuperación simulada de contraseña |
| HU-03 | SHOULD | Consulta y actualización del perfil; activación o desactivación de cuenta |
| HU-04 | MUST | Creación y edición de publicaciones de alojamientos |
| HU-05 | MUST | Activación, pausa, consulta y moderación de publicaciones |
| HU-06 | MUST | Administración de disponibilidad y bloqueo de periodos reservados |
| HU-07 | MUST | Búsqueda y filtros por ciudad, fechas, capacidad, tipo, precio y servicios |
| HU-08 | MUST | Consulta del detalle del alojamiento |
| HU-09 | MUST | Cotización desglosada por noches, precio base, limpieza, tarifa de servicio, descuentos, extras y total |
| HU-10 | MUST | Creación inmediata de reservas, con control de disponibilidad y solicitudes repetidas |
| HU-11 | MUST | Consulta del estado e historial de reservas |
| HU-12 | MUST | Cancelación según la política aplicable y liberación del periodo cuando corresponda |
| HU-13 | SHOULD | Notificaciones dentro de la plataforma |
| HU-14 | SHOULD | Creación de reseñas y calificaciones tras una estancia completada |
| HU-15 | SHOULD | Consulta y moderación de reseñas |
| HU-16 | SHOULD | Restricción de cuentas y consulta de auditoría administrativa |
| HU-17 | SHOULD | Panel con usuarios, publicaciones activas, reservas, ingresos estimados y ciudades con mayor demanda |
| HU-18 | COULD | Exportación de reportes de gestión a PDF o Excel |
| HU-19 | MUST | Logs estructurados de autenticación, publicaciones, reservas, cancelaciones, reseñas, errores y acciones administrativas |

WON'T en Entrega 1: procesar pagos reales o usar credenciales de producción. La reserva MUST usa pagos simulados exclusivamente con proveedor en modo de pruebas.

## Requerimientos del proyecto

- El equipo propone Scrum + Lean, con épicas, historias y sprints registrados en Jira. La metodología, los roles operativos, la duración de los sprints y la capacidad requieren ratificación del equipo y del docente cuando aplique.
- La visión, el alcance, los interesados, los procesos, los requerimientos y el diseño se documentarán en Confluence.
- El código se gestionará en GitHub con ramas y Pull Requests. El flujo del repositorio propone integrar cambios en develop por Pull Request y promover versiones estables a main; la revisión por otro integrante y los checks configurados deben verificarse en cada cambio.
- GitHub Actions ejecutará compilación y pruebas. El análisis de SonarCloud queda condicionado a la configuración de la organización, el proyecto y sus credenciales.
- Los servicios se documentarán con contratos OpenAPI y se entregará el modelo de datos.
- La matriz de trazabilidad relacionará requerimientos con historias, criterios de aceptación, ramas o commits, Pull Requests, pruebas, evidencias y releases.
- Las entregas académicas se presentarán en el aula virtual. Los integrantes prepararán y realizarán la sustentación.

## Descripción del proyecto y entregables

La solución propuesta es un frontend web en HTML, CSS y JavaScript que consumirá servicios REST desarrollados con Java 21 y Spring Boot, con PostgreSQL para persistencia. Esta selección tecnológica es una propuesta inicial pendiente de aprobación del equipo.

| Etapa | Entregables | Criterio de aceptación |
|---|---|---|
| Análisis | Catálogo de requerimientos y reglas, actores e interesados, procesos BPMN y backlog de épicas e historias | El sistema debe contar con historias identificables, prioridad MoSCoW y trazabilidad; cada criterio empieza con «El sistema debe» y conserva Dado que / Cuando / Entonces |
| Diseño | Casos de uso, modelos de dominio y datos, arquitectura orientada a servicios, contrato OpenAPI, secuencias y prototipo | El sistema debe tener diagramas coherentes con requerimientos priorizados y servicios relacionados con las historias que atienden |
| Construcción | Código de backend y frontend versionado en GitHub | El sistema debe integrarse mediante Pull Request revisado y cumplir el Quality Gate de SonarCloud cuando el servicio esté configurado |
| Prueba | Pruebas automatizadas y evidencias de validación | El sistema debe superar la ejecución de pruebas en GitHub Actions y demostrar el flujo de publicación, búsqueda, disponibilidad, cotización y reserva inmediata |

Los grupos de procesos se relacionan con estos entregables: iniciación (acta), planeación (cronograma, WBS/EDT, riesgos y backlog), ejecución (Entregas 1 y 2), monitoreo y control (Jira, integración continua, análisis estático y trazabilidad), y cierre (acta de cierre, informe final y sustentación).

## Objetivos

**Objetivo general:** Analizar, diseñar e implementar un marketplace web de alojamientos temporales para Inversiones LR que conecte anfitriones con huéspedes mediante una arquitectura orientada a servicios y un enfoque ágil por validar.

**Objetivos específicos:**

- Identificar actores, interesados, procesos y requerimientos, y especificarlos en historias con criterios de aceptación.
- Diseñar los servicios, contratos OpenAPI, modelo de dominio y modelo de datos.
- Implementar un MVP para publicar alojamientos, buscar opciones, consultar disponibilidad, cotizar y reservar de forma inmediata.
- Evitar reservas duplicadas mediante una fuente central de disponibilidad.
- Preparar integración continua, pruebas automatizadas y análisis estático con SonarCloud, sujeto a su configuración.
- Mantener la trazabilidad desde cada requerimiento e historia hasta las pruebas y el release correspondiente.

## Supuestos y restricciones

**Supuestos**

- Los dos integrantes participarán durante el semestre.
- GitHub, GitHub Actions, Jira, Confluence, SonarCloud y Stripe en modo de pruebas estarán disponibles en planes gratuitos; su disponibilidad debe comprobarse durante el proyecto.
- Java 21 con Spring Boot, PostgreSQL y HTML, CSS y JavaScript son tecnologías propuestas y pendientes de aprobación.
- El enunciado del proyecto se mantendrá durante el semestre.

**Restricciones**

- El docente define las fechas oficiales de entrega. Todas las fechas del cronograma de esta acta están pendientes de confirmación.
- Los pagos se simularán con Stripe en modo de pruebas; no se realizarán cobros reales.
- El backend expondrá servicios REST consumidos por el frontend.
- El equipo usará Pull Requests y revisión de otro integrante según el flujo documentado en el repositorio; las reglas y checks deben verificarse.
- La evidencia del uso de IA debe incluir capturas auténticas con fecha, hora y prompts completos, conforme a la guía del curso.
- Las funcionalidades opcionales se abordarán solo si el MVP obligatorio está completo.

## Cronograma

La tabla conserva las actividades previstas en el acta fuente. El docente debe confirmar las fechas oficiales; los sprints, responsables y calendario definitivo también requieren acuerdo del equipo.

| Actividad | Fecha de entrega |
|---|---|
| Inicio y planeación: acta, backlog inicial, cronograma y riesgos | Pendiente |
| Ingeniería de requerimientos: actores, procesos BPMN e historias con criterios | Pendiente |
| Diseño orientado a servicios: casos de uso, modelos, OpenAPI y prototipo | Pendiente |
| Entrega 1: análisis de requerimientos y diseño | Pendiente |
| Sprint 1: identidad, perfil, publicaciones y disponibilidad (EP-01 a EP-03) | Pendiente |
| Sprint 2: búsqueda, cotización, reserva, cancelación y notificaciones (EP-04 a EP-07) | Pendiente |
| Sprint 3: reseñas, administración, panel, reportes y logs (EP-08 a EP-10) | Pendiente |
| Validación y cierre: pruebas, Quality Gate e informe final | Pendiente |
| Entrega 2: producto funcional, pruebas, integración continua y sustentación | Pendiente |

## Riesgos

| Riesgo | Respuesta prevista | Tipo |
|---|---|---|
| Un integrante no cumple sus tareas o se retira | Revisar y distribuir las tareas en Jira; redistribuir historias si cambia la conformación del equipo | Mitigar |
| Experiencia limitada con Spring Boot, GitHub Actions o SonarCloud | Hacer ejercicios pequeños al inicio y apoyarse entre integrantes | Mitigar |
| No completar el MVP durante el semestre | Priorizar el backlog obligatorio y dejar opciones para el final | Mitigar |
| Dos solicitudes confirman periodos superpuestos | Revalidar disponibilidad al confirmar y hacer idempotente la reserva | Evitar |
| Uso accidental de pagos reales o datos de tarjetas | Usar Stripe solo en modo de pruebas y no almacenar datos de tarjetas | Evitar |
| Pérdida de código o conflictos de integración | Trabajar en ramas, usar Pull Requests y verificar las reglas de main y develop | Mitigar |
| Evidencia insuficiente del uso de IA | Guardar desde el inicio capturas con fecha, hora y prompts completos | Mitigar |
| Fin de una prueba gratuita o falla de una herramienta | Revisar alternativas gratuitas y mantener exportado el backlog | Contingencia |

## Presupuesto estimado

Estimación inicial del proyecto en pesos colombianos (COP), pendiente de revisión por el equipo.

| Descripción | Costos directos | Costos indirectos | Reservas |
|---|---:|---:|---:|
| Trabajo del equipo: 2 integrantes × aprox. 60 horas × $10.000/h | $1.200.000 | — | — |
| GitHub, GitHub Actions, Jira, Confluence, SonarCloud y Stripe de pruebas | $0 (planes gratuitos) | — | — |
| Computador, internet y energía: 2 integrantes × $150.000 | — | $300.000 | — |
| Reserva para imprevistos (10 %) | — | — | $150.000 |
| **Total estimado** | **$1.200.000** | **$300.000** | **$150.000** |

**Presupuesto total estimado: COP $1.650.000.**

## Interesados

| Interesado | Rol o interés | Actitud | Poder |
|---|---|---|---|
| Inversiones LR | Cliente; requiere el marketplace | Positiva | Alto |
| Ing. David Esteban Rodríguez Gómez | Docente y patrocinador; define el enunciado y evalúa el proyecto | Positiva | Alto |
| Equipo del proyecto | Analiza, diseña, desarrolla y prueba la solución | Positiva | Medio |
| Anfitriones | Administran publicaciones, precios, disponibilidad y políticas | Positiva | Medio |
| Huéspedes | Buscan, cotizan, reservan, cancelan y reseñan | Positiva | Medio |
| Administradores de la plataforma | Moderan usuarios, publicaciones y reseñas; consultan auditoría e informes | Positiva | Medio |
| Propietarios de inmuebles | Confían a Inversiones LR la gestión de sus propiedades | Positiva | Bajo |
| Stripe en modo de pruebas | Servicio externo para simular pagos | Indiferente | Bajo |

## Equipo del proyecto

| Integrante | Rol |
|---|---|
| Julián Aguilera | Product Owner y analista (rol propuesto, pendiente de ratificación): backlog de Jira, requerimientos, documentación y casos de prueba |
| Juan Sebastián Morales | Scrum Master y gerente del proyecto (roles propuestos, pendientes de ratificación); backend, frontend, repositorio e integración continua |

## Aprobación y firmas

La aprobación formal del acta y las firmas están pendientes. Los espacios no representan una aprobación ya otorgada.

| Rol | Nombre | Fecha | Firma |
|---|---|---|---|
| Gerente del proyecto | Juan Sebastián Morales | Pendiente | Pendiente |
| Patrocinador | Ing. David Esteban Rodríguez Gómez | Pendiente | Pendiente |

---

El índice de los documentos relacionados se encuentra en [Entrega 1](README.md).

