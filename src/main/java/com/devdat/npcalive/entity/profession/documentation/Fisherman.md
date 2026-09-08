# Documentación de FishermanProfession

## Guía del Pescador: Tu Maestro Pescador

El pescador es un NPC trabajador que se encarga de buscar fuentes de agua cercanas, lanzar su caña, pescar diferentes tipos de peces, manejar herramientas y almacenar los recursos de forma totalmente autónoma.

*   **Horarios de trabajo:** El pescador cumple con dos turnos de trabajo diarios (por la mañana y por la tarde) para realizar sus labores junto a la orilla.
*   **Rutina junto al agua:** Detecta fuentes de agua cercanas a su barril, desplazándose para posicionarse de pie en la orilla mientras interactúa con el entorno.
*   **Uso de la caña de pescar:** Equipa automáticamente una caña en su mano principal, ejecutando animaciones de balanceo, efectos de partículas de salpicadura (`SPLASH`) y sonidos de pesca.
*   **Depósito automático en cofres:** No tienes que vaciarle los bolsillos a mano. Tan pronto como termina su turno de trabajo o su inventario se acumula, el pescador camina solo hasta el cofre adyacente para guardar todo lo recolectado.

---

## Documentación Técnica

**Descripción General**
`FishermanProfession` gestiona la inteligencia artificial y el comportamiento laboral de los NPCs pescadores en el mod. Se encarga de la búsqueda inteligente de agua, la pesca con recompensas ponderadas, la manipulación de herramientas y la transferencia automática de recursos a los cofres cercanos.

## Horarios de Trabajo
Los NPCs pescadores operan en dos turnos diarios para optimizar su rutina:

| Turno | Inicio (Ticks) | Fin (Ticks) | Descripción |
| :--- | :--- | :--- | :--- |
| **Primer Turno** | 2000L | 6000L | Jornada matutina principal de pesca. |
| **Segundo Turno** | 8000L | 12000L | Jornada vespertina de pesca. |

## Jerarquía de Prioridades (`getTargetPosition`)
El método de selección de posición evalúa el entorno de manera secuencial según las siguientes prioridades:

* **Ventana de Cierre / Almacenamiento:** Si faltan 400 ticks para terminar el turno o el inventario requiere almacenamiento con ítems en la mochila, el NPC prioriza dirigirse al cofre adyacente.
* **Búsqueda de Agua Cercana:** Escanea en un radio de 4 bloques alrededor de su estación (barril) para localizar bloques de agua y posicionarse en un bloque sólido adyacente (en la orilla).
* **Estación Predeterminada (Fallback):** Si no encuentra agua cerca, utiliza por defecto el frente de su bloque de trabajo asignado (barril).

## Ejecución de Tareas (`tickWork` y `performWork`)
* **Equipamiento de Herramienta:** Mantiene equipada una caña de pescar (`FISHING_ROD`) en la mano principal durante las horas de trabajo.
* **Efectos Visuales y Sonoros:** Cada 20 ticks ejecuta un balanceo de brazos, generando partículas de salpicadura (`ParticleTypes.SPLASH`) sobre el agua y sonidos de lanzamiento de anzuelo.
* **Recompensas de Pesca:** Cada 200 ticks otorga recompensas aleatorias basadas en una tabla ponderada (60% Bacalao, 25% Salmón, 10% Pez tropical, 5% Pez globo) acompañadas de sonido de salpicadura de boya.

## Gestión de Mochila y Cofres
Hereda los métodos de la interfaz `ProfessionLogic` para gestionar los slots de almacenamiento:
* **Filtro de Mochila:** Identifica si existen elementos acumulados en la mochila del NPC que deban ser resguardados.
* **Transferencia Segura:** Al llegar al cofre adyacente, vacía el contenido de la mochila del NPC hacia el contenedor del mundo, reproduciendo los eventos de bloque y el sonido de cierre de cofre.
