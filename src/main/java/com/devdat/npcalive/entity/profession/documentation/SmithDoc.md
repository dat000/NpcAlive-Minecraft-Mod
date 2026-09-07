# Documentación de SmithProfession

## Guía del Herrero: Tu Maestro Artesano

El herrero es un NPC trabajador que se encarga de abastecer tu aldea con recursos de hierro, carbón y herramientas útiles de forma totalmente autónoma.

*   **Horarios de trabajo:** El herrero cumple con dos turnos de trabajo diarios (por la mañana y por la tarde). Fuera de esos horarios, descansa o se mueve por la zona.
*   **Rutina en el taller:** No se queda quieto en un solo bloque. Si le dejas cerca una Mesa de Herrería, un Alto Horno y un Yunque, el NPC rotará entre estas máquinas de forma natural para simular que funde, golpea y repara.
*   **Lo que produce:** Mientras trabaja, acumula elementos en su mochila. La mayor parte del tiempo fabricará recursos básicos como carbón o pepitas de hierro, pero con menor probabilidad puede conseguir lingotes de hierro, picos o incluso armaduras de caballo.
*   **Depósito automático en cofres:** No tienes que vaciarle los bolsillos a mano. Tan pronto como termina su turno de trabajo o su mochila se llena, el herrero caminará solo hasta el cofre más cercano para guardar todo lo que recolectó.

---

## Documentación Técnica

**Descripción General**
`SmithProfession` gestiona la inteligencia artificial y la rutina laboral de los NPCs herreros. Combina la producción de recursos mediante una tabla de probabilidades balanceada, el uso alternado de estaciones de trabajo (mesa principal, alto horno y yunque) y la gestión automática de almacenamiento en cofres.

## Horarios de Trabajo
Los NPCs herreros operan en dos turnos diarios distribuidos para maximizar su productividad:

| Turno | Inicio (Ticks) | Fin (Ticks) | Descripción |
| :--- | :--- | :--- | :--- |
| **Primer Turno** | 2000 | 6000 | Jornada matutina principal de forja. |
| **Segundo Turno** | 8000 | 12000 | Jornada vespertina de forja. |

## Sistema de Rotación Estable de Estaciones
Para evitar comportamientos erráticos o indecisiones frente a múltiples máquinas adyacentes, el NPC utiliza una lista rotativa basada en el identificador único (`UUID`) y el tiempo de juego (`gameTime / 200`). Cada 10 segundos, el herrero alterna de forma fluida entre:
* **Mesa de Trabajo Principal:** Su estación de anclaje base (`workPos`).
* **Alto Horno (`Blast Furnace`):** Estación secundaria situada en el perímetro.
* **Yunque (`Anvil`):** Estación secundaria de golpeo y refinamiento.

## Jerarquía de Prioridades y Almacenamiento
* **Ventana de Cierre / Guardado:** Si faltan 400 ticks para finalizar cualquiera de los turnos de trabajo o la mochila cuenta con suficientes elementos acumulados, el NPC prioriza dirigirse al cofre adyacente (`chestPos.north()`) para vaciar su inventario.
* **Ejecución de Rutina:** Fuera de la ventana de almacenamiento, evalúa las estaciones disponibles mediante la rotación temporal y procede a trabajar en la que corresponda.

## Ejecución de Tareas (`tickWork` y `performWork`)
* **Alto Horno:** Emite partículas de humo denso (`ParticleTypes.LARGE_SMOKE`) y reproduce sonidos crujientes de fuego (`SoundEvents.BLASTFURNACE_FIRE_CRACKLE`) cada 20 ticks.
* **Yunque y Mesa Principal:** Cada 40 ticks, ejecuta la animación de golpe con la herramienta (`InteractionHand.MAIN_HAND`), emite partículas de lava y aplica sonidos de yunque (`SoundEvents.ANVIL_USE`).
* **Tabla de Recompensas Balanceada:** Al completar un ciclo de trabajo en el yunque o la mesa principal, otorga ítems a la mochila del NPC siguiendo una distribución equilibrada para evitar el exceso de progreso:
    * **90.0% de probabilidad:** Materiales básicos (Pepitas de hierro `Iron Nugget` o Carbón `Coal`).
    * **8.0% de probabilidad:** Material intermedio (Lingote de hierro `Iron Ingot`).
    * **1.8% de probabilidad:** Herramienta rara (Pico de hierro `Iron Pickaxe`).
    * **0.2% de probabilidad:** Objeto excepcional (Armadura de caballo de hierro `Iron Horse Armor`).

## Gestión de Mochila y Cofres
Hereda los métodos de la interfaz `ProfessionLogic` a partir del índice de inventario asignado:
* **Filtro de Inventario:** Monitorea la acumulación interna para activar la rutina de guardado.
* **Transferencia Segura:** Al llegar frente al cofre adyacente, transfiere de forma sincronizada los ítems recolectados hacia el contenedor del mundo y ejecuta las animaciones de apertura y cierre.