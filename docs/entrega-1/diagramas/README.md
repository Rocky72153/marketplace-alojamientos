# Diagramas renderizados

Imágenes generadas a partir de las fuentes de la documentación de Entrega 1 para poder verlas en Confluence y en la sustentación sin herramientas adicionales.

| Imagen | Fuente |
|---|---|
| `03-pb01-publicacion.png` a `03-pb04-moderacion-reportes.png` | Diagramas Mermaid de [03. Procesos de negocio](../03-procesos-negocio.md) |
| `bpmn-*.png` | Archivos BPMN 2.0 de [bpmn/](../bpmn/), con carriles por participante |
| `06-casos-de-uso-uml.png` / `.svg` | Diagrama UML de casos de uso de [06. Casos de uso](../06-casos-de-uso.md) |
| `06-casos-de-uso-flujo.png` | Diagrama Mermaid de [06. Casos de uso](../06-casos-de-uso.md) |
| `07-modelo-dominio.png` | Diagrama de clases de [07. Modelo de dominio](../07-modelo-dominio.md) |
| `08-modelo-datos-er.png` | Diagrama entidad-relación del diseño (sección 8.2) de [08. Modelo de datos](../08-modelo-datos.md) |
| `08-modelo-fisico-er.png` | Modelo físico implementado (sección 8.1), fuente [fuentes/08-modelo-fisico-er.mmd](fuentes/08-modelo-fisico-er.mmd) |
| `09-contexto.png`, `09-solucion-servicios.png` | Diagramas de diseño de la Entrega 1 en [09. Arquitectura de servicios](../09-arquitectura-servicios.md); el contexto quedó reemplazado por la versión C4 |
| `09-c4-contexto.png`, `09-c4-contenedores.png`, `09-componentes.png`, `09-despliegue.png` | C4 niveles 1 y 2, componentes UML y despliegue en AWS de [09. Arquitectura](../09-arquitectura-servicios.md); fuentes `.dot` en [fuentes/](fuentes/) |
| `bpmn-moderacion-reportes.png` | BPMN 2.0 de [bpmn/moderacion-reportes.bpmn](../bpmn/moderacion-reportes.bpmn) |
| `14-sd04-pago-stripe.png` | Secuencia implementada del pago con Stripe en [14. Secuencias](../14-secuencias.md), fuente [fuentes/14-sd04-pago-stripe.mmd](fuentes/14-sd04-pago-stripe.mmd) |
| `20-paquetes.png`, `20-actividad-reserva.png`, `20-estados-*.png`, `20-pipeline-ci-cd.png` | Paquetes, actividad, máquinas de estado y pipeline de [20. Diseño detallado](../20-diseno-detallado.md) |
| `14-sd01-*.png` a `14-sd03-*.png` | Diagramas de secuencia de [14. Secuencias](../14-secuencias.md) |
| `16-ciclo-por-sprint.png` | Ciclo por sprint de [16. Ciclo de vida del software](../16-ciclo-vida-software.md) |

Si cambia una fuente, se debe regenerar su imagen. Los `.dot` se generan con Graphviz (`dot -Tpng -Gdpi=110 archivo.dot -o ../archivo.png`) y los `.mmd` con Mermaid CLI (`mmdc -i archivo.mmd -o ../archivo.png -s 2 -b white`).
