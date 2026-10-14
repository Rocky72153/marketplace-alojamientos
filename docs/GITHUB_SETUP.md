# Configuración inicial de GitHub y SonarCloud

Este documento distingue los archivos automatizables de las opciones que requieren permisos de administrador. No guardes secretos en archivos versionados.

## 1. Ramas y flujo

- `main`: versión estable y entregable.
- `develop`: integración del trabajo aprobado.
- `feature/US-123-descripcion`, `fix/US-123-descripcion` y `docs/US-123-descripcion`: cambios puntuales.
- Cada cambio debe asociarse a una historia/tarea de Jira, commits coherentes, Pull Request, pruebas y release.

Las ramas de funcionalidad deben partir de `develop`. Todo cambio entra mediante PR y requiere revisión de otro integrante.

## 2. Protección de ramas

En `Settings > Rules > Rulesets` o `Settings > Branches`, configura reglas para `main` y `develop` cuando tu plan lo permita. Exige PR, al menos una aprobación, descartar aprobaciones obsoletas, bloquear force-push y eliminación, y requerir CI.

Selecciona los status checks después de que los workflows hayan corrido al menos una vez. No marques SonarCloud como obligatorio hasta completar su configuración.

## 3. SonarQube Cloud (SonarCloud)

1. Entra a https://sonarcloud.io/ e inicia sesión con GitHub.
2. Importa `marketplace-alojamientos` y crea el proyecto.
3. Copia la clave de organización y la clave del proyecto.
4. En GitHub abre `Settings > Secrets and variables > Actions > Variables` y crea `SONAR_ORGANIZATION` y `SONAR_PROJECT_KEY` con esos valores.
5. En la pestaña `Secrets`, crea `SONAR_TOKEN` con un token generado desde SonarQube Cloud.
6. El workflow utiliza esas variables para construir el análisis; no introduzcas el token en archivos, issues, commits ni capturas compartidas.
7. Abre un PR de prueba y confirma que el análisis y el Quality Gate aparezcan en GitHub.

Hasta que existan ambas variables, el job de SonarCloud se omite intencionalmente. Cuando ya existan, también se necesita el secreto `SONAR_TOKEN` para que el análisis se ejecute correctamente. No marques el check como obligatorio antes de comprobarlo.

## 4. Colaboradores y visibilidad

Añade a cada compañero desde `Settings > Collaborators`. El repositorio debe tener la visibilidad acordada por el equipo y el docente; durante el desarrollo suele convenir privado si todos tienen acceso.

## 5. Verificar CI

En `Actions`, revisa que `Backend CI` compile y ejecute las pruebas Spring Boot y que `Frontend CI` ejecute `npm ci`, `npm run lint`, `npm test` y `npm run build`.

## 6. Releases y trazabilidad

Cuando `main` contenga una versión estable, crea un tag como `v0.1.0` desde `Releases > Draft a new release`, resume los cambios y referencia las historias de Jira entregadas. Mantén actualizada `docs/TRACEABILITY_TEMPLATE.csv` para relacionar requisito, historia, criterios, rama/commit, PR, prueba y release.

## 7. Script auxiliar

`scripts/configure-github-branch-protection.sh` ofrece una configuración inicial por GitHub CLI para quienes tengan `gh`, sesión autenticada y permisos administrativos. Ejecuta ese script solo cuando los checks requeridos estén publicados y sus nombres coincidan exactamente con los que muestra Actions.
