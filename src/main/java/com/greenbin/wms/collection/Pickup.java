package com.greenbin.wms.collection;

import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.people.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A special pickup requested by a citizen or institution (FR_23). */
public class Pickup {
    public enum Status { REQUESTED, CONFIRMED, DENIED, COMPLETED }

    private final UUID requestID = UUID.randomUUID();
    private final User user;
    private final WasteType typeOfWaste;
    private final LocalDate date;
    private final LocalTime time;
    private final String location;
    private final Coordinate coordinate;
    private final double estimatedWeightKg;
    private Status status = Status.REQUESTED;
    private GarbageCollectionEmployee handledBy;
    private String answer = "";
    private Bill bill;

    public Pickup(User user, WasteType typeOfWaste, LocalDate date, LocalTime time,
                  String location, Coordinate coordinate, double estimatedWeightKg) {
        if (estimatedWeightKg <= 0) throw new IllegalArgumentException("The weight must be more than 0 kg");
        this.user = user;
        this.typeOfWaste = typeOfWaste;
        this.date = date;
        this.time = time;
        this.location = location;
        this.coordinate = coordinate;
        this.estimatedWeightKg = estimatedWeightKg;
    }

    public String showPickup() {
        return typeOfWaste + " pickup at " + location + " on " + date + " " + time + " - " + status;
    }

    public UUID getRequestID() { return requestID; }
    public User getUser() { return user; }
    public WasteType getTypeOfWaste() { return typeOfWaste; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getLocation() { return location; }
    public Coordinate getCoordinate() { return coordinate; }
    public double getEstimatedWeightKg() { return estimatedWeightKg; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public GarbageCollectionEmployee getHandledBy() { return handledBy; }
    public void setHandledBy(GarbageCollectionEmployee e) { this.handledBy = e; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Bill getBill() { return bill; }
    public void setBill(Bill bill) { this.bill = bill; }

    @Override
    public String toString() { return showPickup(); }
}
