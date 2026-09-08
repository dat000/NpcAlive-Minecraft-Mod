# Fisherman Profession Documentation (`FishermanProfession.java`)

**Overview**
The `FishermanProfession` class implements custom AI logic for NPC fishermen, replacing vanilla mechanics by combining workstation interaction (Barrels), intelligent shoreline water-source pathfinding, tool handling, particle effects, weighted loot tables, and automated chest storage.

**Core Components & Features**

* **Target Positioning (`getTargetPosition`)**:
    * **Chest Priority**: Checks if the work shift is ending or backpacks are full, directing the NPC to adjacent chests for inventory offloading.
    * **Shoreline Detection**: Scans for water blocks within a 4-block radius of the barrel, positioning the NPC on an adjacent solid block at the water's edge.
    * **Fallback Logic**: Defaults to the front face of the barrel workstation if no water source is found nearby.

* **Work Cycle Animation & Effects (`tickWork`)**:
    * **Tool Equipping**: Automatically equips a `FISHING_ROD` in the NPC's main hand during work hours.
    * **Visuals & Audio**: Executes arm swings, generates `ParticleTypes.SPLASH` particles over the water surface, and plays `FISHING_BOBBER_THROW` sounds every 20 ticks.

* **Reward Production & Storage (`performWork`)**:
    * **Inventory Transfer**: Automatically transfers backpack contents into adjacent chests or trapped chests with container close audio and block events.
    * **Timed Rewards**: Grants weighted fish drops every 200 ticks (10 seconds) accompanied by `FISHING_BOBBER_SPLASH` sounds.

* **Loot Distribution Table**:
    * **Cod**: 60% probability.
    * **Salmon**: 25% probability.
    * **Tropical Fish**: 10% probability.
    * **Pufferfish**: 5% probability.

* **Work Schedule**:
    * **First Shift**: 2000L to 6000L (In-game ticks).
    * **Second Shift**: 8000L to 12000L (In-game ticks).