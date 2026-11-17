# EP-10: observabilidad y calidad

La épica [MARKET-29](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-29) comprende la historia HU-19 [MARKET-23](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-23) y sus subtareas MARKET-66 y MARKET-67. Su objetivo es hacer trazables las operaciones y los errores sin registrar credenciales.

## Diseño aprobado

El 12 de noviembre de 2026 el Product Owner aprobó generar en el servidor un identificador único para cada solicitud, devolverlo en la respuesta e incluirlo en los eventos y registros correspondientes. La decisión DEC-12 figura en [el registro de decisiones](DECISIONS.md).

La implementación integrada genera un UUID por petición y lo devuelve en la cabecera `X-Correlation-ID`; los errores de la API también lo incluyen en el cuerpo. No se usa como identificador el contenido de una cabecera proporcionada por quien hace la petición. Los eventos nuevos de auditoría guardan la referencia para que un administrador pueda consultarla; los eventos anteriores pueden carecer de ella. Las operaciones ejecutadas fuera de una petición reciben una referencia independiente.

El registro estructurado emite metadatos: fecha/hora, tipo de evento, actor y entidad cuando corresponda, resultado e identificador de correlación. Para no copiar información sensible, no incluye contraseñas, tokens, cuerpos de solicitudes ni texto libre de motivos. Los motivos administrativos siguen formando parte de la auditoría protegida de la aplicación, de acuerdo con los requisitos de moderación; el registro técnico no los reproduce. Los eventos transaccionales se emiten tras confirmar la operación, para no presentar como completada una acción revertida.

## Verificación histórica del 12 de noviembre de 2026

El 12 de noviembre de 2026 se ejecutaron 29 pruebas del servidor y 42 de la interfaz, además de la revisión de archivos, compilación del frontend y validación documental. Las pruebas incluyen autenticación, publicaciones, reservas, cancelaciones, reseñas, acciones administrativas, errores controlados y no previstos, una transacción revertida y búsqueda de credenciales ficticias en las respuestas y los registros.

Se realizó una prueba adicional con PostgreSQL 17 en una base local desechable: el registro de una cuenta ficticia y un inicio de sesión rechazado conservaron el mismo identificador en la respuesta y la auditoría; también se comprobó que una cabecera no fiable y la contraseña ficticia no se reflejaron en la respuesta ni en los registros inspeccionados. La [captura de auditoría](evidencias/ep10-correlacion-demo.jpg) muestra los eventos y sus referencias de solicitud con datos de prueba. Esta evidencia no acredita por sí sola la integración ni el cierre en Jira.

## Actualización del 16 de noviembre de 2026

HU-19 forma parte del código integrado. Los resultados de [CI en main](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/43980372836) y [SonarCloud](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/43980372963) para `d073598` finalizaron correctamente. Las cifras de la sección anterior pertenecen a la ejecución del 12 de noviembre y no se presentan como el recuento de la versión actual.

Los pendientes transversales mencionados en la nota original recibieron evidencia posterior:

- **MARKET-68:** la [validación de entrega](EVIDENCIA-ENTREGA-FINAL.md) documenta el arranque desde un clon limpio, con PostgreSQL aislado y el catálogo ficticio. También diferencia esa ejecución de la comprobación posterior con el volumen habitual de Compose.
- **MARKET-69:** DEC-08 registra el objetivo aprobado por Juan el 12 de noviembre: p95 de hasta 3 segundos sobre 50 búsquedas y 5 clientes simultáneos, con 10.000 alojamientos. La [medición local](evidencias/medicion-market-69-final.json) obtuvo p95 de 0,854151 s y la [medición EC2 del 14 de noviembre](evidencias/medicion-aws-t3-small-2026-11-14.json), 0,206649 s. El [procedimiento](PRUEBA-RENDIMIENTO.md) y la evidencia explican sus condiciones; la medición EC2 no incluye latencia de Internet.

Estas comprobaciones corresponden a los ambientes y versiones registrados. No constituyen un acuerdo de rendimiento de producción, una nueva ejecución desde clon limpio en cada commit ni una aceptación docente. El [informe actual de Entrega 2](INFORME-FINAL-ENTREGA-2.md) reúne el estado de integración y las verificaciones humanas restantes.

<details>
<summary>Nota original del 12 de noviembre, sustituida por la evidencia posterior</summary>

Las tareas transversales MARKET-68, reproducibilidad desde un clon limpio, y MARKET-69, medición de rendimiento con objetivos acordados, no se cierran automáticamente al completar HU-19. La [guía local](LOCAL_SETUP.md) describe el procedimiento, pero aún requiere ejecución y evidencia de clon limpio. Los objetivos de rendimiento siguen pendientes de acuerdo en DEC-08.

</details>
