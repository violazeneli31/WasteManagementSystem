package com.greenbin.wms.collection;

import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.GarbageCollectionEmployee;

/** An order to place a new smart bin at a location (FR_11). */
public class BinPlacementOrder {
    public enum Status { ORDERED, PLACED, ISSUE }

    private static int counter = 1;

    private final int orderNumber = counter++;
    private final ZoneLocation location;
    private final WasteType wasteType;
    private GarbageCollectionEmployee assignedTo;
    private Status status = Status.ORDERED;
    private String issue = "";

    public BinPlacementOrder(ZoneLocation location, WasteType wasteType, GarbageCollectionEmployee assignedTo) {
        this.location = location;
        this.wasteType = wasteType;
        this.assignedTo = assignedTo;
    }

    public int getOrderNumber() { return orderNumber; }
    public ZoneLocation getLocation() { return location; }
    public WasteType getWasteType() { return wasteType; }
    public GarbageCollectionEmployee getAssignedTo() { return assignedTo; }
    public void setAssignedTo(GarbageCollectionEmployee e) { this.assignedTo = e; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    @Override
    public String toString() { return "Order #" + orderNumber + ": " + wasteType + " bin at " + location.getName(); }
}
