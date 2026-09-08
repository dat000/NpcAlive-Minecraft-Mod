# Butcher Profession Documentation

The **Butcher** is an autonomous NPC profession focused on meat production, operating around smokers and managing automated food supply chains.

## **Overview & Mechanics**
* **Workstation**: Smoker (`BLOCKS.SMOKER`). The NPC positions itself directly in front of the smoker based on block orientation.
* **Assigned Tool**: Iron Axe (`Items.IRON_AXE`), held in the main hand while operating to simulate meat cutting.
* **Work Schedule**: Operates during two daily shifts:
    * **First Shift**: 02:00 – 06:00 (Minecraft Time)
    * **Second Shift**: 08:00 – 12:00 (Minecraft Time)

## **Production & Loot Table**
During active work cycles (triggered every 200 ticks/10 seconds of continuous operation), the butcher generates food items and stores them in its backpack:

| Item | Drop Chance | Description |
| :--- | :--- | :--- |
| **Raw Beef** | 60% | Basic meat yield from daily processing |
| **Raw Porkchop** | 30% | Common secondary meat harvest |
| **Cooked Beef** | 8% | Processed directly via smoker heat |
| **Rabbit Stew** | 2% | Rare high-tier culinary product |

## **Inventory & Chest Integration**
* **Backpack Capacity**: Accumulates produced meat items dynamically during work hours.
* **Automated Storing**: Prioritizes transferring inventory contents into any adjacent chest (`CHEST` or `TRAPPED_CHEST`) automatically when shifts end or when the backpack reaches capacity limits.
* **Visual Effects**: Emits subtle campfire smoke particles (`ParticleTypes.CAMPFIRE_COSY_SMOKE`) centered precisely above the smoker block accompanied by custom block interaction sounds.