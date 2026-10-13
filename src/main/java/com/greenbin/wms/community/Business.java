package com.greenbin.wms.community;

import com.greenbin.wms.people.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** FR_25: a partner business (supermarket, cafe...) that gives vouchers for points. */
public class Business {
    /** Something the business offers, for a number of points. */
    public static class Offer {
        private final String description;
        private final int pointsCost;

        public Offer(String description, int pointsCost) {
            this.description = description;
            this.pointsCost = pointsCost;
        }

        public String getDescription() { return description; }
        public int getPointsCost() { return pointsCost; }
        @Override public String toString() { return description + " (" + pointsCost + " pts)"; }
    }

    private static final Random RANDOM = new Random();

    private final UUID businessID = UUID.randomUUID();
    private final String name;
    private final String location;
    private final String phoneNumber;
    private final String email;
    private final List<Offer> offers = new ArrayList<>();
    private int vouchersRedeemed;

    public Business(String name, String location, String phoneNumber, String email) {
        this.name = name;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public void addOffer(String description, int pointsCost) { offers.add(new Offer(description, pointsCost)); }

    /** Turns the user's points into a voucher with a code. Returns null if they don't have enough points. */
    public BusinessVoucher generateVoucher(User user, Offer offer) {
        if (user.isBlocked() || !user.usePoints(offer.getPointsCost())) return null;
        String code = "GB-" + (1000 + RANDOM.nextInt(9000)) + "-" + (1000 + RANDOM.nextInt(9000));
        BusinessVoucher voucher = new BusinessVoucher(this, user, offer.getDescription(), offer.getPointsCost(),
                code, LocalDate.now().plusDays(30));
        user.getVouchers().add(voucher);
        return voucher;
    }

    void countRedemption() { vouchersRedeemed++; }

    public UUID getBusinessID() { return businessID; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public List<Offer> getOffers() { return offers; }
    public int getVouchersRedeemed() { return vouchersRedeemed; }

    @Override
    public String toString() { return name; }
}
