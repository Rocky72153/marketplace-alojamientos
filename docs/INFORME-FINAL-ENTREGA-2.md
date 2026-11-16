# Informe de preparación de la Entrega 2

**Corte:** 12 de noviembre de 2026, hora de Bogotá.
**Fuente de requisitos:** guía «Proyecto de Curso: Marketplace Alojamientos» facilitada por el equipo; las referencias a páginas corresponden a ese PDF. Este informe reúne lo comprobado y lo pendiente. No constituye aceptación del profesor ni de los integrantes del equipo.

## Alcance y estado de implementación

El proyecto contiene un backend REST, una interfaz web y persistencia PostgreSQL. La base local usa un catálogo de **10.000 alojamientos de demostración**, distribuidos por destinos colombianos. Estos anuncios y sus cuentas de muestra son ficticios; no prueban que exista disponibilidad u oferta comercial real. El backlog y los estados se consultan en [Jira MARKET](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2/backlog).

| Requisito de la guía | Estado y evidencia | Límite o pendiente |
| --- | --- | --- |
| Cuentas, roles, perfil y recuperación simulada (p. 3) | Existen registro e inicio de sesión, roles, consulta y edición de perfil, desactivación y recuperación simulada. El servidor y la interfaz cuentan con pruebas. | La recuperación no envía correo ni cambia la contraseña. La guía menciona activar o desactivar la cuenta; se debe aclarar y validar el procedimiento de reactivación de una cuenta ya desactivada. |
| Publicaciones y estados (pp. 3–4) | El anfitrión puede crear y editar anuncios, gestionar su estado y disponibilidad. La moderación puede bloquearlos; se conservan las reservas futuras para revisión. | Se detectó que el código integrado también permitía al administrador crear publicaciones. En la rama de depuración se corrigió para que solo HOST pueda crearlas; la prueba focalizada pasó y falta integrar el cambio mediante el flujo exigido. |
| Disponibilidad, búsqueda y detalle (p. 3) | Están implementados períodos, búsquedas por criterios, detalle público y bloqueo de períodos al reservar. La portada ofrece sugerencias sin iniciar sesión. | Los resultados de demostración no representan alojamientos reales. |
| Cotización, reserva y cancelación (pp. 3–4) | El backend calcula el desglose, confirma reservas inmediatas idempotentes, conserva historial y aplica políticas de cancelación con devolución simulada. El flujo fue probado sobre PostgreSQL aislado, incluyendo reintento idempotente y cancelación. | La guía exige un **servicio de pagos de prueba externo, como Stripe** (p. 4). El proyecto usa un simulador interno y no acredita todavía esa integración externa. No realizar cobros reales. |
| Notificaciones, reseñas y moderación (pp. 3–4) | Hay notificaciones internas, reseñas vinculadas a reservas completadas y moderación. Existen pruebas automáticas. | La guía enumera reseñas en el MVP (p. 3) y las llama opcionales en el glosario (p. 10); la función está implementada pese a esa inconsistencia. |
| Administración, auditoría, indicadores y reportes (pp. 3–4) | Existen panel administrativo, restricciones, auditoría y exportación a PDF y Excel. Los ingresos mostrados son estimaciones de pagos simulados. | Verificar con el equipo que los indicadores y filtros cumplen el alcance de su demostración. |

La guía también menciona reservas sujetas a aprobación (p. 3), pero define la **reserva inmediata** como obligatoria para el MVP (p. 10). El producto implementa la inmediata; la necesidad y alcance de una modalidad adicional deben aclararse con el equipo o el profesor, sin presentarla como ya implementada.

## Pruebas, integración y evidencia

- **Servidor:** la batería `mvn verify` terminó con 36 pruebas, cero fallos y cero errores en la rama de depuración. Entre ellas se probaron validaciones de solicitudes, contraseñas Unicode y concurrencia entre edición y moderación.
- **Permisos de publicación:** después de esa batería se añadió la restricción de creación a anfitriones; la prueba focalizada verificó el rechazo de invitados, huéspedes y administradores y la asignación de propiedad al anfitrión. Queda repetir la batería sobre el commit final.
- **Interfaz:** la instalación desde el archivo de dependencias, verificaciones, pruebas y compilación pasaron. La auditoría de dependencias del frontend no mostró vulnerabilidades al momento de la revisión.
- **Prueba de extremo a extremo:** se utilizó una base PostgreSQL temporal aislada con datos ficticios. Se registraron anfitrión y huésped, se creó y editó una publicación, se configuró disponibilidad, se activó el anuncio, se cotizó, se confirmó y reintentó la misma reserva y se canceló. Se comprobó que una solicitud de disponibilidad incompleta se rechazara sin borrar el período. El pago usado en esta prueba fue el simulador interno; **no demuestra un pago en un servicio externo**.
- **Rendimiento:** la prueba previa sobre el catálogo ficticio de 10.000 alojamientos registró 50 búsquedas exitosas con cinco clientes concurrentes y p95 de 0,854151 s, por debajo del objetivo inicial de 3 s aprobado por Juan. La metodología y los datos están en [evidencia de entrega](EVIDENCIA-ENTREGA-FINAL.md) y [resultado detallado](evidencias/medicion-market-69-final.json). No implica aprobación del profesor ni un compromiso de producción.
- **Capturas:** hay evidencias de [portada](evidencias/portada-catalogo-2026-11-12.jpg), [publicación](evidencias/anfitrion-publicacion-2026-11-12.jpg), [perfil](evidencias/perfil-anfitrion-2026-11-12.jpg) y [recuperación simulada](evidencias/recuperacion-simulada-2026-11-12.jpg). La [portada](evidencias/depuracion-catalogo-2026-11-12.jpg), [cotización](evidencias/depuracion-cotizacion-2026-11-12.jpg) y [resultados de la prueba de extremo a extremo](evidencias/depuracion-flujo-2026-11-12.json) de depuración corresponden a la base temporal; no sustituyen las pruebas automatizadas ni la evidencia requerida de uso de IA.
- **CI y análisis estático:** existen GitHub Actions y SonarCloud en el flujo de PR. Los resultados de una versión concreta deben consultarse en sus comprobaciones antes de integrarla; las pruebas locales no reemplazan esos resultados.

El [inventario de API implementada](API-IMPLEMENTADA.md) identifica rutas y permisos; el [OpenAPI de Entrega 1](entrega-1/10-openapi.yaml) es una propuesta histórica con diferencias respecto al código. Se debe completar o validar un contrato equivalente que describa los cuerpos, respuestas y errores de los servicios reales, junto con su colección o evidencia de validación (pp. 4 y 6).

## Gestión, trazabilidad y entrega

1. **Integración de pagos:** implementar y comprobar la conexión a un proveedor externo de pagos en modo de prueba, con credenciales de prueba del equipo y sin procesar pagos reales (p. 4). Hasta entonces el criterio no está acreditado.
2. **Revisión de PR:** la guía exige que cada PR tenga al menos una revisión de otro integrante y que la integración a la rama principal ocurra mediante PR con CI, pruebas y Quality Gate satisfactorios (pp. 5–7). Una revisión automatizada o de un agente no acredita esa revisión humana.
3. **Release y trazabilidad:** crear la versión identificable cuando el incremento esté validado y completar la relación requisito ↔ Jira ↔ criterio ↔ rama/commit ↔ PR ↔ pruebas/evidencia ↔ release (pp. 6 y 8). La [matriz](TRACEABILITY_TEMPLATE.csv) contiene referencias históricas y requiere revisión integral; no debe interpretarse que todas las filas ya llegan a un release verificado.
4. **Evidencia de IA:** preparar el documento con capturas auténticas que muestren fecha, hora y prompts completos de entrada y salida (p. 1). No recrear interacciones ni atribuir resultados inexistentes.
5. **Equipo y aceptación:** completar y acreditar la revisión de otro integrante, la validación docente, las firmas o aprobaciones del acta cuando correspondan y la retroalimentación real de participantes. No se han verificado como completadas por el mero estado de Jira.
6. **Despliegue:** [LOCAL_SETUP.md](LOCAL_SETUP.md) permite ejecutar el proyecto localmente y explica una base de prueba aislada. Debe definirse y comprobarse el ambiente de entrega y su manual de despliegue conforme a la guía (pp. 5 y 8); no se afirma aquí que exista un despliegue productivo.
7. **Entrega y sustentación:** la entrega se realiza por aula virtual y el resultado depende de la sustentación de los integrantes (p. 1). El PDF no fija en su texto una fecha calendario concreta; cualquier fecha debe confirmarse con la información del curso.

## Lecciones y evolución

La revisión técnica identificó riesgos al editar simultáneamente un anuncio y moderarlo, al recibir datos inválidos y al mostrar respuestas antiguas de búsqueda, autenticación o cotización. Se prepararon correcciones y pruebas para estos casos. Esas observaciones técnicas no reemplazan una retrospectiva real del equipo.

Como evolución técnica, una vez acreditados los requisitos obligatorios, se recomienda completar la integración externa de prueba, formalizar el contrato vigente, ejecutar una revisión humana y validar el despliegue en el ambiente acordado. El equipo debe registrar sus propias lecciones y decisiones de evolución; no se atribuyen aquí retrospectivas ni aprobaciones no realizadas.
