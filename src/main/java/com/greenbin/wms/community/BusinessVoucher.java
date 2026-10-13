package com.greenbin.wms.community;

import com.greenbin.wms.people.User;

import java.time.LocalDate;
import java.util.UUID;

/** FR_25: a digital coupon with a code (shown as a QR code in the app). */
public class BusinessVoucher {
    public enum Status { ACTIVE, REDEEMED, EXPIRED }

    private final UUID voucherID = UUID.randomUUID();
    private final Business business;
    private final User user;
    private final String description;
    private final int pointsCost;
    private final String code;
    private final LocalDate expirationDate;
    private Status status = Status.ACTIVE;

    public BusinessVoucher(Business business, User user, String description, int pointsCost, String code, LocalDate expirationDate) {
        this.business = business;
        this.user = user;
        this.description = description;
        this.pointsCost = pointsCost;
        this.code = code;
        this.expirationDate = expirationDate;
    }

    /** Used at the shop counter. A voucher works only once and only before it expires. */
    public boolean redeem() {
        if (status != Status.ACTIVE) return false;
        if (LocalDate.now().isAfter(expirationDate)) {
            status = Status.EXPIRED;
            return false;
        }
        status = Status.REDEEMED;
        business.countRedemption();
        return true;
    }

    public String showVoucher() { return description + " at " + business.getName() + " - code " + code + " (" + status + ")"; }

    public UUID getVoucherID() { return voucherID; }
    public Business getBusiness() { return business; }
    public User getUser() { return user; }
    public String getDescription() { return description; }
    public int getPointsCost() { return pointsCost; }
    public String getCode() { return code; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Status getStatus() { return status; }
}
