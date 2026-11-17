# Marketplace de Alojamientos

Aplicación web para descubrir, publicar y reservar alojamientos temporales. Proyecto de Ingeniería de Software I de la Universidad El Bosque, periodo 2026-2, para Inversiones LR.

**[Abrir demostración](http://moralesforero.lol/)** · **[Documentación](docs/README.md)** · **[Release v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0)**

La demostración contiene un catálogo de **10.000 alojamientos ficticios de Colombia**, con búsqueda en 1.122 destinos territoriales. Usa **Stripe en modo de prueba**: se documentaron un pago, su confirmación por evento firmado y un reembolso, sin dinero real. Al funcionar por HTTP, debe probarse únicamente con cuentas y datos ficticios.

## Qué permite hacer

| Rol | Funciones principales |
|---|---|
| Visitante | Ver sugerencias, buscar por ciudad y fechas, filtrar, consultar fichas y cotizar sin iniciar sesión. |
| Huésped | Registrarse, gestionar su perfil, reservar y pagar en modo de prueba, consultar su historial, cancelar y reseñar una estancia completada. |
| Anfitrión | Crear y editar publicaciones propias, gestionar estados, precios, extras y disponibilidad. |
| Administrador | Moderar cuentas, publicaciones y reseñas; consultar auditoría, indicadores y revisiones manuales; exportar PDF y Excel. |

El sistema también incluye notificaciones internas, control de disponibilidad, reservas idempotentes e identificadores de correlación para investigar errores. Las reglas del MVP se explican en la [documentación funcional](docs/README.md#documentación-vigente).

## Probarlo en local

Necesitas **Git, JDK 21, Maven, Node.js 20.19 o 22 con npm y Docker Compose**. La [guía local](docs/LOCAL_SETUP.md) explica la configuración, las cuentas de muestra, una base aislada para verificar un clon limpio y cómo detener el entorno sin perder datos.

Desde una terminal PowerShell, descarga el proyecto y arranca PostgreSQL:

```powershell
git clone https://github.com/Rocky72153/marketplace-alojamientos.git
cd marketplace-alojamientos
docker compose up -d db
```

Cuando la base esté saludable, abre otra terminal en la raíz y arranca la API:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

En una tercera terminal, también desde la raíz, arranca la interfaz:

```powershell
cd frontend
npm ci
npm run dev
```

Abre [localhost:4173](http://localhost:4173). La API usa [localhost:8080/api/health](http://localhost:8080/api/health). Espera a que termine la primera carga del catálogo.

Para conservar las sesiones entre reinicios, configura `AUTH_TOKEN_SECRET` como indica la guía. Si quieres usar las cuentas demo, define tu propio `DEMO_SEED_PASSWORD` **antes de la primera carga**; no hay una contraseña universal y configurarla después no reemplaza claves existentes. También puedes registrar tu propia cuenta. El registro de administradores requiere la configuración privada de `ADMIN_BOOTSTRAP_KEY`.

El entorno local usa el simulador interno de pagos por defecto. La integración externa necesita la [configuración de Stripe de prueba](docs/STRIPE-PRUEBAS.md); las credenciales permanecen fuera del repositorio.

## Arquitectura y tecnologías

Una interfaz estática consume una API REST modular; PostgreSQL conserva usuarios, publicaciones, reservas y auditoría. Los paquetes del backend separan responsabilidades dentro de una aplicación Spring Boot: no se presentan como microservicios desplegados por separado.

| Parte | Implementación |
|---|---|
| Interfaz | HTML, CSS y módulos JavaScript; diseño adaptable y animaciones que respetan movimiento reducido. |
| API y permisos | Java 21, Spring Boot, Spring Security, JWT y Maven. |
| Persistencia | PostgreSQL con Spring Data JPA; H2 aislada en las pruebas del backend. |
| Pagos y reportes | Stripe sandbox; exportaciones PDF y XLSX. |
| Calidad | GitHub Actions, JUnit, pruebas de JavaScript, JaCoCo y SonarCloud. |
| Demostración | AWS EC2, Nginx y systemd; entrega de eventos Stripe por loopback con Stripe CLI. |

Consulta la [arquitectura C4 y el despliegue](docs/entrega-1/09-arquitectura-servicios.md), el [diagrama de clases XML editable](docs/diagramas/diagrama-clases-marketplace.xml) y el [inventario de diagramas](docs/entrega-1/diagramas/README.md).

## Verificar el proyecto

Desde la raíz, compila y prueba el backend:

```powershell
mvn -B -ntp -f backend/pom.xml verify
```

Para la interfaz:

```powershell
cd frontend
npm ci
npm run lint
npm test
npm run build
```

El validador documental necesita Python y PyYAML. Desde la raíz:

```powershell
python -m pip install PyYAML
python scripts/validate_delivery_docs.py
```

Los workflows de [GitHub Actions](https://github.com/Rocky72153/marketplace-alojamientos/actions) ejecutan las comprobaciones del backend, frontend y documentación; el análisis de [SonarCloud](https://sonarcloud.io/dashboard?id=Rocky72153_marketplace-alojamientos) evalúa el Quality Gate. Las [evidencias de ejecución](docs/README.md#evidencias) indican la versión y las condiciones verificadas.

## Encontrar la información

| Necesitas | Empieza aquí |
|---|---|
| Preparar la sustentación y conocer el estado real | [Informe de Entrega 2](docs/INFORME-FINAL-ENTREGA-2.md) y [acta de cierre](docs/ACTA-CIERRE.md). |
| Consumir o revisar la API | [Contrato OpenAPI vigente](docs/openapi.yaml) e [inventario de la API](docs/API-IMPLEMENTADA.md). |
| Entender el diseño | [Clases y guía de lectura](docs/DIAGRAMA-CLASES.md), [arquitectura](docs/entrega-1/09-arquitectura-servicios.md) y [diseño detallado](docs/entrega-1/20-diseno-detallado.md). |
| Ejecutar o desplegar | [Guía local](docs/LOCAL_SETUP.md), [despliegue EC2](docs/DESPLIEGUE-EC2.md) y [Stripe de prueba](docs/STRIPE-PRUEBAS.md). |
| Revisar historias, criterios y versiones | [Jira MARKET](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2), [trazabilidad](docs/TRACEABILITY_TEMPLATE.csv) y [release v1.0](https://github.com/Rocky72153/marketplace-alojamientos/releases/tag/v1.0). |
| Consultar el apoyo de IA en la revisión de diagramas | [Registro textual](docs/evidencias/uso-ia-2026-11-17.md) y [prompt elaborado](docs/evidencias/prompt-revision-diagramas-2026-11-17.md), sin captura conservada de la revisión técnica. |
| Navegar el resto de documentos | [Índice de documentación](docs/README.md). |

## Estructura del repositorio

- `backend/`: API, persistencia, seguridad, migraciones y pruebas.
- `frontend/`: interfaz, recursos, construcción y pruebas.
- `deploy/`: plantillas del entorno de demostración.
- `docs/`: manuales, decisiones, diagramas, evidencias y entregas.
- `scripts/`: validación documental, medición y configuración auxiliar.
- `.github/`: integración continua y plantillas para colaborar.

## Equipo y alcance de la entrega

**Juan Sebastián Morales Forero:** Product Owner y Developer. **Julián David Aguilera Párraga:** Scrum Master y Developer. El equipo se organiza con Scrum; el registro de trabajo está en [Jira](https://unbosque-team-zeutvqmy.atlassian.net/jira/software/projects/MARKET/boards/2) y la documentación por fases, en [Confluence](https://unbosque-team-zeutvqmy.atlassian.net/wiki/spaces/MARKET/overview).

La recuperación de contraseña es simulada y no envía correo. Los alojamientos, las cuentas de muestra y los pagos de prueba no representan operaciones comerciales reales. Las firmas, la revisión independiente, la validación con personas y la aceptación docente deben respaldarse con evidencia humana; una comprobación automática o un estado de Jira no las sustituye. El [acta de cierre](docs/ACTA-CIERRE.md) recoge estas condiciones y la disponibilidad prevista de la demostración para la entrega del **26 de noviembre de 2026**.

Para contribuir, consulta [CONTRIBUTING.md](CONTRIBUTING.md) y vincula los cambios con su tarea, criterios, PR y evidencia.
