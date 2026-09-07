# Documentación de FarmerProfession

**Descripción General**
`FarmerProfession` gestiona la inteligencia artificial y el comportamiento laboral de los NPCs agricultores en el mod. Se encarga de la siembra, cosecha, uso de compostadores y la transferencia automática de recursos a los cofres cercanos.

## Horarios de Trabajo
Los NPCs agricultores operan en dos turnos diarios para optimizar su rutina:

| Turno | Inicio (Ticks) | Fin (Ticks) | Descripción |
| :--- | :--- | :--- | :--- |
| **Primer Turno** | 1000 | 6000 | Jornada matutina principal. |
| **Segundo Turno** | 8000 | 11500 | Jornada vespertina. |

## Jerarquía de Prioridades (`getTargetPosition`)
El método de selección de posición evalúa el entorno de manera secuencial según las siguientes prioridades:

* **Ventana de Cierre / Almacenamiento:** Si faltan 400 ticks para terminar el turno o el inventario cuenta con 3 o más ítems acumulados, el NPC prioriza dirigirse al cofre adyacente a su mesa de trabajo.
* **Cosecha de Cultivos Maduros:** Busca en un radio de 8 bloques (`WORK_RADIUS`) cultivos estándar que hayan alcanzado su madurez máxima (`CropBlock`).
* **Recolección de Calabazas y Melones:** Detecta bloques de calabaza o melón maduros y busca una posición adyacente válida para cosecharlos.
* **Uso del Compostador:** Si el inventario contiene semillas aptas para compostar (`hasCompostableItems`), se dirige a la mesa de trabajo (Compostador).
* **Patrullaje Agrícola:** Si no hay tareas prioritarias, realiza un recorrido estacional calculado sobre las tierras de cultivo (`Farmland`) y zonas de siembra.

## Ejecución de Tareas (`tickWork` y `performWork`)
* **Animaciones de Rotura:** Durante la destrucción de calabazas o melones, ejecuta progreso visual de daño y sonidos de madera.
* **Uso del Compostador:** Emite partículas (`HAPPY_VILLAGER`) y sonidos característicos cuando alimenta el compostador o extrae harina de huesos (`Bone Meal`) al completarse el nivel 8.
* **Siembra Automática:** Tras cosechar un cultivo tradicional, verifica si la tierra inferior es `Farmland` y consume una semilla de la mochila para replantar inmediatamente de forma sincronizada.

## Gestión de Mochila y Cofres
Hereda los métodos de la interfaz `ProfessionLogic` para gestionar los slots de almacenamiento (del índice 6 en adelante):
* **Filtro de Mochila:** Identifica si existen elementos acumulados que deban ser resguardados.
* **Transferencia Segura:** Al llegar al cofre adyacente, vacía el contenido de la mochila del NPC hacia el contenedor del mundo, reproduciendo las animaciones de apertura y cierre de cofre.