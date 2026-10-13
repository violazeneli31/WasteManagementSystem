package com.greenbin.wms.collection;

import java.util.ArrayList;
import java.util.List;

/** FR_11: keeps all the bin placement orders and their status. */
public class BinPlacementMap {
    private final List<BinPlacementOrder> orders = new ArrayList<>();

    public void addOrder(BinPlacementOrder order) { orders.add(order); }

    public List<BinPlacementOrder> getOrders() { return orders; }

    public List<BinPlacementOrder> getOrders(BinPlacementOrder.Status status) {
        List<BinPlacementOrder> result = new ArrayList<>();
        for (BinPlacementOrder o : orders) if (o.getStatus() == status) result.add(o);
        return result;
    }

    public String generateBinPlacementList() {
        StringBuilder sb = new StringBuilder();
        for (BinPlacementOrder o : orders) sb.append(o).append(" - ").append(o.getStatus()).append('\n');
        return sb.toString();
    }
}
