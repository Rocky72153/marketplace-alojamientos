# Prueba de rendimiento de búsqueda

Objetivo inicial de entrega acordado para MARKET-69: percentil 95 de hasta 3 segundos en 50 búsquedas con un máximo de 5 usuarios simultáneos y un catálogo de al menos 10.000 alojamientos activos. Este objetivo acordado para el MVP no pretende representar un requisito aprobado por el profesor ni una garantía de producción.

Con el servidor iniciado y el catálogo local cargado, desde la raíz del repositorio ejecutar:

```powershell
python scripts/measure_search_performance.py --base-url http://127.0.0.1:8080 --output resultado-rendimiento.json
```

El script consulta el catálogo activo fuera de la medición, selecciona diez ciudades con resultados reales en ese catálogo y calienta la búsqueda antes de medir. Las búsquedas incluyen ciudad, llegada, salida y dos huéspedes. Se mide desde el cliente la respuesta completa, incluyendo red, servidor y lectura JSON. El percentil usa la posición `ceil(0,95 × 50)` de los tiempos ordenados; todos los fallos o respuestas sin resultados hacen fallar la prueba. El archivo JSON conserva las 50 mediciones, errores, versión y sistema donde corrió el cliente. Registrar por separado el procesador, la memoria disponible, versiones de Java y PostgreSQL, configuración y cualquier diferencia entre el servidor y el cliente: estos datos no se suponen a partir del resultado.

El comando solo consulta el servidor, pero la consulta inicial sin filtros devuelve el catálogo activo completo y puede consumir recursos. Ejecutar sobre una base de demostración, nunca inferir que los datos ficticios representan alojamientos reales ni confundir esta medición local con una prueba de carga de producción. Código de salida `0` indica que se cumplieron todos los criterios; `1` indica fallo o preparación incompleta. Guardar la evidencia real generada y adjuntarla a MARKET-69 antes de cambiar el estado.
