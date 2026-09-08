# Documentación de LibrarianProfession

## Guía del Bibliotecario: Tu Maestro de las Letras

El bibliotecario es un NPC dedicado al estudio y la organización del conocimiento, interactuando con su atril, gestionando libros y almacenando documentos o recursos de forma autónoma.

* **Horarios de trabajo:** El bibliotecario cumple con dos turnos diarios estructurados para desempeñar sus tareas intelectuales y de archivo.
* **Rutina en la biblioteca:** Se desplaza hacia su atril asignado para realizar estudios, revisar textos y mantener el orden en su estación de trabajo.
* **Interacción con el atril:** Utiliza su atril para realizar gestos de lectura, generar efectos visuales y procesar documentos o recompensas ligadas a su labor.
* **Depósito automático en cofres:** Al finalizar sus turnos o cuando su mochila alcanza el límite de capacidad, el bibliotecario camina de forma independiente hacia el cofre adyacente para vaciar su inventario.

---

## Documentación Técnica

**Descripción General**
`LibrarianProfession` gestiona la inteligencia artificial y el comportamiento laboral de los NPCs bibliotecarios en el mod. Controla la interacción con los atriles (`Lectern`), el manejo de herramientas de estudio, y la transferencia automática de recursos hacia los cofres cercanos.

## Horarios de Trabajo
Los bibliotecarios operan bajo los siguientes turnos establecidos en el sistema:

| Turno | Inicio (Ticks) | Fin (Ticks) | Descripción |
| :--- | :--- | :--- | :--- |
| **Primer Turno** | 2000L | 6000L | Jornada matutina de estudio y archivo. |
| **Segundo Turno** | 8000L | 12000L | Jornada vespertina de gestión bibliotecaria. |

## Jerarquía de Prioridades (`getTargetPosition`)
El método de selección de posición analiza el entorno de trabajo según el siguiente orden:

* **Ventana de Cierre / Almacenamiento:** Si faltan 400 ticks para concluir el turno o el inventario requiere almacenamiento, el NPC prioriza dirigirse al cofre adyacente.
* **Posicionamiento en Estación:** Evalúa las orientaciones y bloques adyacentes al atril principal o atriles suplementarios para ubicarse correctamente frente a ellos.
* **Patrullaje de Estudio:** Selecciona dinámicamente posiciones alrededor del área de trabajo para mantener un flujo de movimiento constante y natural.

## Ejecución de Tareas (`tickWork` y `performWork`)
* **Uso de Herramientas:** Equipa y maneja libros o herramientas de escritura en la mano principal del NPC durante sus horas activas.
* **Animaciones y Sonidos:** Ejecuta balanceos de brazos e interacciones sincronizadas con efectos visuales y sonoros característicos de la lectura y el manejo de papel o atril.
* **Recompensas e Intercambios:** Procesa de manera periódica la generación de recursos o elementos literarios que se introducen directamente en la mochila del NPC.

## Gestión de Mochila y Cofres
Utiliza los métodos de la interfaz `ProfessionLogic` para el control de inventario:
* **Filtro de Mochila:** Detecta los objetos acumulados que deben ser resguardados tras las jornadas de trabajo.
* **Transferencia Segura:** Al alcanzar el contenedor adyacente, transfiere el contenido de la mochila del NPC hacia el cofre del mundo, reproduciendo las animaciones de cierre y los sonidos correspondientes.