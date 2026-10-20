# 11. Prototipo y estrategia de validación

## 11.1 Prototipo existente

La rama principal contiene un prototipo de baja fidelidad implementado en `frontend/index.html`, `frontend/src/styles.css` y `frontend/src/main.js`. Presenta la marca del Marketplace, navegación preliminar, formulario de búsqueda con ciudad, llegada, salida y número de huéspedes, área introductoria y una sección informativa para anfitriones.

[Abrir el prototipo/código en GitHub](https://github.com/Rocky72153/marketplace-alojamientos/tree/main/frontend)

**Alcance actual:** validación de campos en el navegador y mensaje informativo. Todavía no existe conexión real del formulario con búsquedas REST, resultados reales de alojamiento, autenticación ni reservas. Este prototipo sirve para obtener feedback visual/funcional inicial; no se debe mostrar como MVP terminado.

## 11.2 Objetivos de validación

- Verificar si huéspedes comprenden el formulario de búsqueda y los campos mínimos.
- Confirmar qué información necesitan comparar antes de cotizar o reservar.
- Validar si anfitriones comprenden las responsabilidades de publicación, precio y disponibilidad.
- Detectar ambigüedades en términos, mensajes de error, fechas y número de huéspedes.
- Verificar con el equipo que el flujo end-to-end de la guía queda cubierto por los diseños y las historias.

## 11.3 Escenarios de revisión propuestos

| ID | Participante objetivo | Tarea | Qué observar | Evidencia a recopilar |
|---|---|---|---|---|
| VAL-01 | Huésped | Encontrar alojamiento en ciudad para fechas y 2 huéspedes. | Comprensión del formulario y filtros faltantes. | Fecha, rol/código anonimizado, comentarios, cambios sugeridos. |
| VAL-02 | Huésped | Comparar dos alojamientos hipotéticos y revisar desglose de cotización. | Información necesaria, claridad del total y cargos. | Observaciones y decisiones sobre etiquetas/componentes. |
| VAL-03 | Anfitrión | Explicar cómo publicaría alojamiento y actualizaría disponibilidad. | Campos mínimos, estados y reglas que no entiende. | Campos/confusiones/errores encontrados. |
| VAL-04 | Administrador/equipo | Explicar cómo bloquear anuncio y conservar futuras reservas para revisión. | Comprensión del flujo y trazabilidad necesaria. | Reglas que requieren especificación. |

Estos escenarios son una propuesta. **No representan sesiones ya realizadas.** Registrar feedback real y decisiones derivadas antes de afirmar validación.

## 11.4 Plan de validación de requerimientos

| Técnica | Aplicación | Resultado esperado |
|---|---|---|
| Revisión basada en checklist | Revisar cada requisito con la guía y las reglas RN asociadas. | Ambigüedades, duplicados, omisiones y contradicciones registradas. |
| Walkthrough de historias | Recorrer historias con AC Dado/Cuando/Entonces desde actor hasta backend. | Historias comprensibles, comprobables y con dependencias identificadas. |
| Walkthrough de procesos | Simular escenarios de publicación, reserva, cancelación, bloqueo y exportación. | Procesos y excepciones consistentes con reglas. |
| Revisión del dominio/datos | Comprobar relaciones, cardinalidades, propiedad de recursos y estados. | Modelo de dominio y datos sin contradicciones importantes. |
| Revisión de API | Comparar contrato OpenAPI con historias/modelos y errores esperados. | Contratos entendibles, respuestas de error y seguridad por rol. |
| Prueba exploratoria del prototipo | Pedir a participantes que completen escenarios sin instrucciones adicionales. | Feedback concreto y lista priorizada de ajustes. |
