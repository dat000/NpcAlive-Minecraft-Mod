# NpcAlive Minecraft Mod

A modular, high-performance NPC framework designed for Minecraft, prioritizing full autonomy, automation, and maintainable code architecture.

## 🛠️ Architecture Overview

- **`ProfessionLogic` Interface**: Centralizes inventory and storage management using Java default methods.
- **Stable Station Rotation**: Prevents erratic NPC behavior by utilizing unique entity IDs (`UUID`) and game time calculations.
- **Automated Storage**: NPCs automatically transition to nearby chests to safely dump collected items when their inventory fills up or shifts end.

---

## 👥 Available Professions & Documentation

Explore the detailed guides and technical documentation for each implemented profession:

- **[SmithProfession Documentation & Guide](src/main/java/com/devdat/npcalive/entity/profession/documentation/SmithDoc.md)**  
  *Features automated workstation rotation (Blast Furnace, Anvil, Worktable), balanced reward drop tables, and autonomous resource gathering.*

- **[FarmerProfession Documentation & Guide](src/main/java/com/devdat/npcalive/entity/profession/documentation/FarmerDoc.md)**  
  *Features automated crop harvesting, pumpkin/melon collection, immediate replanting, and composting automation.*

---

## 🚀 Getting Started

1. Clone the repository.
2. Build the project using Gradle (`./gradlew build`).
3. Run the development environment (`./gradlew runClient`).