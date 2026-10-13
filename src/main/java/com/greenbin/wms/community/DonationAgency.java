package com.greenbin.wms.community;

import java.util.List;
import java.util.UUID;

/** FR_27: a charity that collects the donated items and gives them to people in need. */
public class DonationAgency {
    private final UUID agencyID = UUID.randomUUID();
    private final String name;
    private final String address;
    private final String phoneNumber;
    private final String email;
    private int itemsReceived;

    public DonationAgency(String name, String address, String phoneNumber, String email) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String receiveContainerOverflowAlert(String containerName) {
        return name + " was told that '" + containerName + "' is ready for collection.";
    }

    public boolean collectDonations(List<Donation> donations) {
        if (donations.isEmpty()) return false;
        for (Donation d : donations) {
            d.setStatus(Donation.Status.COLLECTED_BY_AGENCY);
            d.setAgency(this);
            itemsReceived++;
        }
        return true;
    }

    public UUID getAgencyID() { return agencyID; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public int getItemsReceived() { return itemsReceived; }

    @Override
    public String toString() { return name; }
}
