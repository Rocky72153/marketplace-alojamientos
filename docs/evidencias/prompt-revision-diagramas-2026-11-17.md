# Prompt elaborado — revisión de diagramas del marketplace

**Preparado el 17/11/2026 para Juan Sebastian Morales Forero.** Es una ampliación de la solicitud real «todos los diagramas completos?», redactada con ayuda de IA durante esta interacción. No es una transcripción literal ni retrospectiva del mensaje del estudiante.

## Contexto y objetivo

Actúa como asistente de revisión técnica y documental de mi proyecto académico Marketplace de Alojamientos. El repositorio es `Rocky72153/marketplace-alojamientos`; su implementación usa Java 21, Spring Boot, PostgreSQL, una interfaz web con módulos JavaScript y pagos exclusivamente de prueba mediante Stripe. El equipo trabaja con Scrum y documenta su trabajo en Jira MARKET y Confluence.

Necesito determinar si los diagramas describen correctamente el sistema implementado y si los archivos que voy a presentar están completos, legibles y respaldados por sus fuentes. Evita concluir que un diagrama está correcto únicamente porque su tarea de Jira aparece terminada o porque existe una imagen.

## Alcance de la revisión

1. Inventaría los diagramas de procesos, BPMN, casos de uso, dominio, entidad-relación, modelo físico, contexto y contenedores C4, componentes, despliegue, secuencias, paquetes, actividad, estados y pipeline. Para cada familia, identifica la documentación y la fuente editable correspondiente.
2. Comprueba que el XML del diagrama de clases puede abrirse, que sus tipos corresponden al inventario del backend y que las relaciones principales tienen un significado explicado. Conserva el formato blanco y negro solicitado y no agregues asociaciones que el código no contiene.
3. Distingue los modelos conceptuales y las propuestas históricas de la Entrega 1 de los diagramas que describen la implementación actual. Una propuesta inicial puede conservarse como antecedente, pero debe estar rotulada claramente.
4. Contrasta los estados de reserva, publicación y pago con las enumeraciones, servicios y decisiones del backend. En Stripe, revisa la espera de pago, eventos asíncronos, cancelación, expiración y reembolso; evita mostrar siempre una reserva confirmada cuando el código contempla resultados distintos.
5. Revisa que el pipeline represente las comprobaciones de GitHub Actions, la protección vigente de las ramas, la publicación del release y la decisión del propietario de dispensar la revisión adicional de Julián para este cierre. Diferencia la integración continua del despliegue manual de la demostración.
6. Si encuentras una inconsistencia demostrable, corrige tanto la fuente como la imagen publicada. Mantén la coherencia entre el Mermaid incluido en Markdown y su archivo independiente. No cambies las reglas de negocio para hacerlas coincidir con un dibujo.

## Validación y forma de la respuesta

Comprueba los XML/BPMN, la integridad de las imágenes y los enlaces de la documentación. Regenera únicamente los diagramas afectados y revisa su resultado visual. Usa las pruebas documentales pertinentes; no repitas pruebas de la aplicación si no cambia su código.

Entrega un resumen con: inventario revisado, hallazgos concretos, archivos corregidos, comprobaciones ejecutadas y límites de la revisión. Relaciona las conclusiones con los archivos que las respaldan. No inventes ejecuciones, revisiones humanas, firmas, aprobaciones del profesor, mensajes históricos ni capturas de una conversación que no ocurrió. Conserva fuera del repositorio cualquier contraseña, token o dato privado.

## Nota sobre su utilización

Este texto formaliza el alcance del trabajo solicitado en el chat y puede reutilizarse en una interacción posterior. La evidencia del mensaje original y la respuesta de esta sesión se conserva por separado; esta versión elaborada no los reemplaza.
