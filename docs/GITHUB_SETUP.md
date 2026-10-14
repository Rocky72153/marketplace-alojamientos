# Configuración inicial de GitHub y SonarCloud

Este documento distingue los archivos automatizables de las opciones que requieren permisos de administrador. No guardes secretos en archivos versionados.

## 1. Ramas y flujo

- `main`: versión estable y entregable.
- `develop`: integración del trabajo aprobado.
- `feature/US-123-descripcion`, `fix/US-123-descripcion` y `docs/US-123-descripcion`: cambios puntuales.
- Cada cambio debe asociarse a una historia/tarea de Jira, commits coherentes, Pull Request, pruebas y release.

Tras el commit inicial, crea `develop` desde `main`. Todos los cambios deben llegar mediante PR y contar con revisión de otro integrante.

## 2. Protección de ramas

En `Settings > Rules > Rulesets` o `Settings > Branches`, crea reglas para `main` y `develop` cuando tu plan lo permita. Exige PR, al menos una aprobación, descartar aprobaciones obsoletas, bloquear force-push y eliminación, y requerir CI.

Selecciona los status checks después de que los workflows ejecuten por lo menos una vez. No marques SonarCloud como obligatorio hasta completar su configuración.

## 3. SonarQube Cloud (SonarCloud)

1. Entra a https://sonarcloud.io/ e inicia sesión con GitHub.
2. Importa `marketplace-alojamientos` y crea el proyecto.
3. Sustituye `REEMPLAZAR_ORGANIZACION` y `REEMPLAZAR_CLAVE_PROYECTO` en `sonar-project.properties` por los valores reales.
4. Genera un token y guárdalo en GitHub: `Settings > Secrets and variables > Actions > New repository secret`, nombre `SONAR_TOKEN`.
5. Nunca pegues el token en commits, issues ni capturas compartidas.
6. Abre un PR de prueba y confirma que el análisis y el Quality Gate aparecen en GitHub.

El workflow de SonarCloud fallará hasta que el proyecto y el secreto estén configurados. No exijas ese check en las reglas antes de verificarlo.

## 4. Colaboradores y visibilidad

Añade a cada compañero desde `Settings > Collaborators`. El repositorio debe tener la visibilidad acordada por el equipo y las exigencias del docente; durante el desarrollo suele convenir privado si todos tienen acceso.

## 5. Verificar CI

En `Actions`, revisa que `Backend CI` compile y ejecute las pruebas Spring Boot y que `Frontend CI` ejecute `npm ci`, `npm run lint`, `npm test` y `npm run build`.

## 6. Releases y trazabilidad

Cuando `main` contenga una versión estable, crea un tag como `v0.1.0` desde `Releases > Draft a new release`, resume los cambios y referencia las historias Jira entregadas. Mantén actualizada `docs/TRACEABILITY_TEMPLATE.csv` para relacionar requisito, historia, criterios, rama/commit, PR, prueba y release.

## 7. Script auxiliar

`scripts/configure-github-branch-protection.sh` ofrece una configuración inicial por GitHub CLI para quienes tengan `gh`, sesión autenticada y permisos administrativos. Verifica que los nombres de status checks correspondan exactamente a los publicados por Actions antes de ejecutarlo.
