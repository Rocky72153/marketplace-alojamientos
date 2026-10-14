# Contribuir

1. Escoge una historia/tarea validada en Jira y verifica sus criterios de aceptación.
2. Actualiza develop y crea una rama corta: feature/US-123-descripcion, fix/US-123-descripcion o docs/US-123-descripcion.
3. Realiza cambios pequeños; los mensajes de commit deben describir el cambio y referenciar Jira cuando exista.
4. Ejecuta localmente las pruebas pertinentes antes de abrir el PR.
5. Abre el PR hacia develop, completa la plantilla y solicita revisión a otro integrante.
6. Resuelve comentarios y espera a que las verificaciones requeridas estén verdes antes del merge.
7. No hagas push directo a main ni develop una vez activadas sus reglas de protección.

## Comprobaciones locales

```bash
mvn -B -f backend/pom.xml verify
cd frontend && npm ci && npm run lint && npm test && npm run build
python -m pip install PyYAML
python scripts/validate_delivery_docs.py
```

La validación documental comprueba OpenAPI, fuentes BPMN, matriz CSV y enlaces Markdown internos.

No incluyas tokens, contraseñas reales, archivos .env, datos personales ni credenciales en commits.