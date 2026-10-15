# Registro de inspección del prototipo y ejecución parcial de pruebas

**Fecha:** 14 de octubre de 2026 (America/Bogota)  
**Proyecto:** Marketplace de Alojamientos  
**Alcance:** evidencia técnica parcial. Esta inspección no sustituye las sesiones con huéspedes, anfitriones, administradores, el cliente ni el equipo.

## Entorno y versión

La inspección interactiva se realizó en el frontend local en `http://localhost:4173/`. Se usó el checkout local `feature/entrega-1-analisis-diseno`, commit `e6ae75f`. Los archivos del frontend y su prueba automatizada coinciden por hash con los archivos equivalentes del HEAD `83da7ff` del PR #5.

## Verificaciones ejecutadas

| Verificación | Resultado observado | Estado |
|---|---|---|
| Pruebas automáticas del frontend | 3 pruebas: ciudad vacía rechazada; salida igual a llegada rechazada; búsqueda válida acepta los campos. 3 pasaron, 0 fallaron. | Aprobado para validación de entradas del frontend |
| Verificación de archivos del frontend | La estructura y los archivos requeridos se verificaron sin errores. | Aprobado |
| Construcción del frontend | Se generó la versión estática en `frontend/dist`. | Aprobado |
| Pruebas locales del backend | No se ejecutaron localmente: Maven no está instalado en este entorno. | No ejecutado localmente |
| Backend CI en el HEAD `83da7ff` del PR #5 | El job compiló y ejecutó las pruebas de backend. | Aprobado en [run 38671271700](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38671271700) |
| Frontend y Documentation CI en el HEAD `83da7ff` del PR #5 | Los jobs de pruebas/build del frontend y de validación de documentos, BPMN, OpenAPI, trazabilidad y enlaces finalizaron correctamente. | Aprobado en [run 38671271700](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38671271700) |
| SonarQube Cloud en el HEAD `b275941` del PR #8 | Quality Gate aprobado; 0 incidencias nuevas y 0 hotspots nuevos. La cobertura de código nuevo reportada fue 0.0 %. | Aprobado en [run 38673786517](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38673786517); la cobertura sigue pendiente |
| CI en main, commit `e4d192e` | Backend, Frontend y Documentation CI finalizaron correctamente. | Aprobado en [run 38678644799](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644799) |
| SonarCloud en main, commit `e4d192e` | Quality Gate aprobado. | Aprobado en [run 38678644828](https://github.com/Rocky72153/marketplace-alojamientos/actions/runs/38678644828) |

Las pruebas automáticas están en [`frontend/tests/search.test.js`](../../frontend/tests/search.test.js). La historia relacionada es [MARKET-11 en Jira](https://unbosque-team-zeutvqmy.atlassian.net/browse/MARKET-11); el diseño de los casos está en [17. Diseño de casos de prueba](../entrega-1/17-casos-de-prueba.md). También se pueden consultar la [PR #5](https://github.com/Rocky72153/marketplace-alojamientos/pull/5) que actualiza la trazabilidad, la [PR #8](https://github.com/Rocky72153/marketplace-alojamientos/pull/8) de SonarCloud y la [auditoría de PR #1–#9](revision-pull-requests-2026-10.md). Son verificaciones parciales de entrada; no equivalen a ejecutar los 44 casos de negocio.

## Inspección interactiva del prototipo

Se completaron estas interacciones en la página local:

| Escenario | Resultado observado | Evaluación |
|---|---|---|
| Enviar el formulario sin ciudad, con fechas válidas y dos huéspedes | Muestra “Escribe la ciudad a la que quieres viajar.” | La validación de ciudad vacía funciona |
| Enviar una búsqueda con Bogotá, llegada 10/11/2026, salida 12/11/2026 y dos huéspedes | Muestra “La búsqueda estará disponible cuando se integre el servicio de alojamientos.” No se consulta una API ni se muestran resultados. | El flujo de búsqueda está bloqueado después de validar campos |
| Abrir “Iniciar sesión” | El enlace apunta a `#iniciar-sesion`, pero no existe una sección con ese identificador. | Hallazgo de navegación; el enlace no lleva a una pantalla |
| Revisar el encabezado a 753 × 884 px | Los enlaces “Descubrir” y “Soy anfitrión” están ocultos por el breakpoint de 800 px; no aparece un menú alternativo. | Hallazgo de navegación adaptable |

La identidad visual, el propósito y los campos de búsqueda se entienden al cargar la página. Sin embargo, los escenarios VAL-02 (comparar alojamientos y cotizar), VAL-03 (publicar como anfitrión) y VAL-04 (administrar/moderar) no se pueden completar en el prototipo actual porque esas pantallas o funciones no existen.

## Estado de los casos diseñados

El documento de diseño contiene 44 casos. Esta inspección solo aporta resultados parciales alrededor de HU-07:

- **CP-HU07-01:** bloqueado en el resultado funcional. El formulario acepta los datos, pero no hay servicio de búsqueda ni resultados para comprobar filtros.
- **CP-HU07-02:** parcial. Las pruebas automáticas comprueban ciudad vacía y fecha de salida igual a llegada; no se pudo comprobar el mensaje de búsqueda sin coincidencias ni una respuesta de API para fechas invertidas.
- **CP-ENV-01 y CP-INT-05:** no ejecutados como casos completos. La CI verde no demuestra una prueba desde clon limpio ni la inyección deliberada de un error en documentación.
- Los casos de registro, perfiles, publicaciones, disponibilidad, reservas, pagos, cancelaciones, reseñas, administración, reportes y auditoría siguen sin ejecutarse. El backend actualmente expone únicamente `GET /api/health`; no hay endpoints de negocio que permitan correr esos casos.

Por tanto, **no se marcan como aprobados los 44 casos**. Registrar cada ejecución posterior con commit, ambiente, ejecutor, resultado observado, estado, enlace de evidencia e incidencia, como indica [17. Diseño de casos de prueba](../entrega-1/17-casos-de-prueba.md).

## Retroalimentación de participantes

Esta es una inspección técnica de interfaz realizada por Codex, no una sesión con usuarios ni retroalimentación del cliente. La validación real con participantes sigue pendiente. Para cerrarla, el equipo debe ejecutar VAL-01 a VAL-04 con participantes reales, anonimizar sus datos y registrar comentarios y decisiones sin atribuirles resultados de esta inspección.

