# Evidencia de validación local de la entrega

Registro de trabajo del 12 de noviembre de 2026. Esta validación local no constituye una aprobación del profesor ni garantiza el comportamiento de un despliegue de producción.

## Equipo y herramientas observadas

| Componente | Versión o dato observado |
| --- | --- |
| Equipo | HP Laptop 14-cf2xxx, Windows 11, 8,4 GB de memoria |
| Git | 2.56.0 |
| Java | Eclipse Temurin 21.0.12.1 |
| Maven | 3.10.0 |
| Node.js y npm | 20.19.0 y 10.8.2 |
| Docker y Compose | 29.9.2 y 5.6.1 |
| PostgreSQL | 17.11 |

La ejecución local utilizó Node.js 20.19.0. La [guía local](LOCAL_SETUP.md) también admite Node.js 22, utilizado en integración continua; esta ejecución local no se hizo con Node.js 22.

## Reproducción y pruebas

Se repitió la comprobación desde un clon limpio de GitHub en el commit `1824b3b`, siguiendo la alternativa de contenedor aislado de la guía actualizada. Se creó una base vacía en un contenedor PostgreSQL temporal por el puerto 15433 y se arrancaron el backend y el frontend. La carga generó 10.000 alojamientos ficticios de demostración.

En el commit `2a35047` se completaron correctamente `mvn verify` con **33 pruebas y 0 fallos**, la revisión de estilo del frontend, sus pruebas y su compilación. Los cambios posteriores hasta `1824b3b` corresponden a documentación y una captura. Las pruebas automáticas no reemplazan la comprobación manual en navegador ni una prueba completa de despliegue.

Se usó la alternativa aislada documentada con `docker run`: **no se probó el comando predeterminado `docker compose up -d db`** ni se utilizó el volumen habitual de Compose en esta comprobación.

En una comprobación posterior, el 12 de noviembre de 2026, se ejecutó también `docker compose up -d db` con el volumen habitual ya existente. El servicio quedó saludable, conservó 10.000 anuncios activos y 1.001 cuentas de muestra y publicó PostgreSQL solo en `127.0.0.1:5432`. Esta comprobación adicional no es un segundo clon limpio ni una prueba de despliegue de producción.

## Rendimiento de búsqueda

La medición final del escenario de búsqueda con 10.000 alojamientos activos registró **50 solicitudes exitosas y 0 fallos**, con 5 clientes concurrentes. El percentil 50 fue de **0,069932 segundos**, el percentil 95 de **0,854151 segundos** y el máximo de **1,201424 segundos**. Se cumplió el objetivo inicial para MARKET-69 aprobado por Juan: percentil 95 de hasta 3 segundos con 50 solicitudes y 5 clientes simultáneos. No se presenta ese objetivo como una aprobación del profesor ni como un acuerdo de servicio de producción.

El detalle de la medición se conserva en [medicion-market-69-final.json](evidencias/medicion-market-69-final.json); el procedimiento está documentado en la [prueba reproducible](PRUEBA-RENDIMIENTO.md). La medición anterior sobre el commit `6f9194b` fue preliminar: 50 de 50 solicitudes exitosas y percentil 95 de 0,971388 segundos.

## Capturas

Las siguientes imágenes corresponden al entorno de demostración local y se conservan en el repositorio:

- [Portada y catálogo](evidencias/portada-catalogo-2026-11-12.jpg).
- [Formulario de publicación de anfitrión](evidencias/anfitrion-formulario-2026-11-12.jpg).
- [Publicación de anfitrión](evidencias/anfitrion-publicacion-2026-11-12.jpg).
- [Perfil de anfitrión](evidencias/perfil-anfitrion-2026-11-12.jpg).
- [Recuperación simulada](evidencias/recuperacion-simulada-2026-11-12.jpg).

Los alojamientos de muestra son ficticios. El pago y la recuperación de contraseña son simulaciones; las capturas no demuestran cobros ni envío de correos reales.
