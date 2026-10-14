# Guía de ejecución local (base del proyecto)

Esta guía permite ejecutar la base técnica incluida en el repositorio. No implica que las funcionalidades del marketplace estén completas: por ahora el backend expone el endpoint de salud y el frontend ofrece un prototipo de búsqueda.

## Requisitos

- Git.
- Java JDK 21.
- Apache Maven 3.9 o compatible.
- Node.js 22 y npm.
- Docker con Docker Compose (solo si se desea levantar PostgreSQL localmente).

Verifica versiones:

```bash
git --version
java -version
mvn -version
node --version
npm --version
docker compose version
```

## Clonar y ejecutar backend

```bash
git clone https://github.com/Rocky72153/marketplace-alojamientos.git
cd marketplace-alojamientos
mvn -B -f backend/pom.xml verify
mvn -f backend/pom.xml spring-boot:run
```

El backend usa el puerto 8080 por defecto y expone `GET http://localhost:8080/api/health`. Respuesta esperada de la base inicial: JSON con `status: UP` y `application: marketplace-alojamientos`.

## Ejecutar base de datos local

En otra terminal, desde la raíz:

```bash
docker compose up -d db
docker compose ps
```

La configuración local de Compose crea una base de desarrollo llamada `marketplace` con usuario `marketplace` y contraseña de ejemplo `marketplace_local`. Son credenciales solo para entorno local; cámbialas en entornos persistentes/desplegados y evita exponer PostgreSQL públicamente.

Variables opcionales para backend:

- `DB_URL` — por defecto `jdbc:postgresql://localhost:5432/marketplace`.
- `DB_USERNAME` — por defecto `marketplace`.
- `DB_PASSWORD` — por defecto `marketplace_local` (solo desarrollo local).
- `PORT` — por defecto `8080`.

## Ejecutar frontend

En otra terminal:

```bash
cd frontend
npm ci
npm run lint
npm test
npm run build
npm run dev
```

Abre `http://localhost:4173`. La búsqueda todavía no consulta el backend; valida campos y muestra una explicación de la funcionalidad pendiente.

## Pruebas automatizadas

- Backend: `mvn -B -f backend/pom.xml verify`. Las pruebas usan H2 en perfil test, sin requerir PostgreSQL en ejecución.
- Frontend: `cd frontend && npm ci && npm run lint && npm test && npm run build`.

## Detener servicios

```bash
docker compose down
```

Para borrar también los datos locales de PostgreSQL (acción destructiva): `docker compose down -v`.

## Límite de esta guía

Este es un manual local, no un manual de despliegue de producción. Antes de desplegar fuera de desarrollo, definir secretos seguros, HTTPS, configuración de perfiles, migraciones de base de datos, backups, observabilidad, permisos, CORS y política de despliegue. La guía del curso pide documentar y evidenciar el despliegue del producto conforme evolucione.
