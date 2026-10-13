package com.greenbin.wms.bins;

import com.greenbin.wms.community.Donation;
import com.greenbin.wms.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

/** FR_27: a collection center container for reusable items (clothes, books, toys...). */
public class DonationContainer extends SmartContainer {
    public static final double KG_PER_ITEM = 2;

    private final List<Donation> donations = new ArrayList<>();

    public DonationContainer(String location, Coordinate coordinate, double capacity, long barcodeSerial) {
        super(location, coordinate, capacity, barcodeSerial);
    }

    public void addDonation(Donation donation) {
        getBarcode().scanBarcode();
        donations.add(donation);
        updateFillLevel(KG_PER_ITEM);
    }

    public List<Donation> getDonations() { return donations; }

    @Override public String getKind() { return "Donation container"; }

    @Override
    public String toString() { return "Donation container - " + getLocation(); }
}
