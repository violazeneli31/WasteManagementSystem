package com.greenbin.wms.finance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * FR_31: simple fraud rules that look at the transaction log.
 *  1. the same payment twice within 10 minutes (duplicate),
 *  2. more than 300 reward points in one day,
 *  3. three or more voucher redemptions in one day.
 */
public class FraudDetector {
    public static final int MAX_POINTS_PER_DAY = 300;
    public static final int MAX_VOUCHERS_PER_DAY = 3;

    /** Checks the new transactions and returns the ones that look suspicious. */
    public List<Transaction> scan(List<Transaction> all) {
        List<Transaction> suspicious = new ArrayList<>();
        for (Transaction t : all) {
            if (t.getFraudStatus() != Transaction.FraudStatus.NOT_CHECKED) continue;
            String reason = check(t, all);
            if (reason == null) {
                t.setFraudStatus(Transaction.FraudStatus.OK);
            } else {
                t.setFraudStatus(Transaction.FraudStatus.SUSPICIOUS);
                t.setAlertReason(reason);
                suspicious.add(t);
            }
        }
        return suspicious;
    }

    private String check(Transaction t, List<Transaction> all) {
        switch (t.getType()) {
            case PAYMENT:
                for (Transaction o : all) {
                    if (o != t && o.getType() == Transaction.Type.PAYMENT && o.getUser() == t.getUser()
                            && o.getAmount() == t.getAmount() && o.getTime().isBefore(t.getTime())
                            && Duration.between(o.getTime(), t.getTime()).toMinutes() < 10) {
                        return "Duplicate payment of " + t.getAmount() + " Lek";
                    }
                }
                return null;
            case POINTS_AWARD:
                // only the transaction that goes over the limit raises the alert (one alert per user per day)
                double pointsToday = sameDayTotal(t, all, false);
                boolean crossed = pointsToday > MAX_POINTS_PER_DAY && pointsToday - t.getAmount() <= MAX_POINTS_PER_DAY;
                return crossed ? "Unusually high reward points today (" + (int) pointsToday + ")" : null;
            case VOUCHER_REDEMPTION:
                double vouchersToday = sameDayTotal(t, all, true);
                return vouchersToday == MAX_VOUCHERS_PER_DAY ? "Too many voucher redemptions today (" + (int) vouchersToday + ")" : null;
            default:
                return null;
        }
    }

    /** Sum of amounts (or count, if countOnly) of this user's transactions of the same type on the same day, up to t. */
    private double sameDayTotal(Transaction t, List<Transaction> all, boolean countOnly) {
        double total = 0;
        for (Transaction o : all) {
            if (o.getType() == t.getType() && o.getUser() == t.getUser()
                    && o.getTime().toLocalDate().equals(t.getTime().toLocalDate()) && !o.getTime().isAfter(t.getTime())) {
                total += countOnly ? 1 : o.getAmount();
            }
        }
        return total;
    }
}
