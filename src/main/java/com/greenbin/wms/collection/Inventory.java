package com.greenbin.wms.collection;

import com.greenbin.wms.model.WasteType;

import java.util.EnumMap;
import java.util.Map;

/** How many new smart bins of each type are in the warehouse (FR_11). */
public class Inventory {
    private final Map<WasteType, Integer> inventory = new EnumMap<>(WasteType.class);

    public Inventory() {
        for (WasteType t : WasteType.values()) inventory.put(t, 0);
    }

    public void updateInventory(WasteType type, int amount) {
        inventory.put(type, Math.max(0, inventory.get(type) + amount));
    }

    /** Takes one bin out of the warehouse. Returns false if there are none left. */
    public boolean takeBin(WasteType type) {
        if (inventory.get(type) == 0) return false;
        updateInventory(type, -1);
        return true;
    }

    public int getStock(WasteType type) { return inventory.get(type); }
    public boolean isOutOfStock(WasteType type) { return inventory.get(type) == 0; }
    public Map<WasteType, Integer> getInventory() { return inventory; }
}
