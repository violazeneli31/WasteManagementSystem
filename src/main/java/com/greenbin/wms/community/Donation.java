package com.greenbin.wms.community;

import com.greenbin.wms.people.User;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR_27: a reusable item a citizen left at a donation container. */
public class Donation {
    public static final int DONATION_POINTS = 15;

    public enum Status { DROPPED_OFF, COLLECTED_BY_AGENCY, DISTRIBUTED }

    private final UUID donationID = UUID.randomUUID();
    private final User citizen;
    private final String itemCategory;
    private final LocalDateTime date = LocalDateTime.now();
    private Status status = Status.DROPPED_OFF;
    private DonationAgency agency;

    public Donation(User citizen, String itemCategory) {
        this.citizen = citizen;
        this.itemCategory = itemCategory;
    }

    /** Gives the donation points to the citizen and returns their new total. */
    public int updateTotalPoints() {
        citizen.addPoints(DONATION_POINTS);
        return citizen.getPoints();
    }

    public UUID getDonationID() { return donationID; }
    public User getCitizen() { return citizen; }
    public String getItemCategory() { return itemCategory; }
    public LocalDateTime getDate() { return date; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public DonationAgency getAgency() { return agency; }
    public void setAgency(DonationAgency agency) { this.agency = agency; }
}
