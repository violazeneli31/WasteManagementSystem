package com.greenbin.wms;

import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.Dispute;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.finance.Transaction;
import com.greenbin.wms.model.BillStatus;
import com.greenbin.wms.model.WasteType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_13 pricing, FR_28 billing, FR_29 payment, FR_31 fraud, FR_34 disputes. */
class BillingAndFraudTest {
    private final TestData d = new TestData();

    private LocalDate workingDay() {
        LocalDate day = LocalDate.now().plusDays(2);
        return day.getDayOfWeek() == DayOfWeek.SUNDAY ? day.plusDays(1) : day;
    }

    private Pickup confirmedPickup(WasteType type, double kg) {
        Pickup p = d.sys.requestPickup(d.ana, type, workingDay(), LocalTime.of(10, 0), "Rr. Kavajes 10", kg);
        d.sys.respondToPickup(p, d.ardi, true, "");
        return p;
    }

    @Test
    void priceIsFeePlusWeightTimesPriceAndTaxIs20Percent() {
        assertEquals(300 + 10 * 25, d.sys.getPricing().calculateCost(WasteType.PLASTIC, 10), 0.001);
        Bill b = new Bill(d.ana, "test", 1000, LocalDate.now(), LocalDate.now().plusDays(30));
        assertEquals(200, b.getTaxAmount(), 0.001);
        assertEquals(1200, b.getAmount(), 0.001);
    }

    @Test
    void confirmedPickupCreatesABillThatMustBeApprovedBeforePaying() {
        Pickup p = confirmedPickup(WasteType.PLASTIC, 10);
        Bill b = p.getBill();
        assertNotNull(b);
        assertEquals(550, b.getBaseAmount(), 0.001);
        assertNull(d.sys.payBill(b, PaymentMethod.CASH));          // not approved yet
        assertTrue(d.sys.approveBill(b));
        assertNotNull(d.sys.payBill(b, PaymentMethod.CREDIT_CARD));
        assertEquals(BillStatus.PAID, b.getStatus());
        assertEquals("Payment receipt", d.ana.getNotifications().get(0).getSubject());
    }

    @Test
    void changingAPriceUpdatesBillsNotApprovedYet() {
        Pickup p = confirmedPickup(WasteType.PLASTIC, 10);
        assertEquals(1, d.sys.changePrice(WasteType.PLASTIC, 30, d.mira));
        assertEquals(600, p.getBill().getBaseAmount(), 0.001);
        assertEquals(1, d.sys.getPricing().getChangeLog().size());
    }

    @Test
    void overdueBillsAreMarked() {
        Bill b = new Bill(d.ana, "old", 100, LocalDate.now().minusDays(40), LocalDate.now().minusDays(10));
        d.sys.getBills().add(b);
        d.sys.updateBillStatuses();
        assertEquals(BillStatus.OVERDUE, b.getStatus());
    }

    @Test
    void duplicatePaymentIsSuspiciousAndConfirmingBlocksTheUser() {
        d.sys.getTransactions().add(new Transaction(d.ben, Transaction.Type.PAYMENT, 540, "x", LocalDateTime.now().minusMinutes(5)));
        d.sys.getTransactions().add(new Transaction(d.ben, Transaction.Type.PAYMENT, 540, "x", LocalDateTime.now().minusMinutes(2)));
        List<Transaction> suspicious = d.sys.runFraudCheck();
        assertEquals(1, suspicious.size());
        assertTrue(d.mira.getNotifications().get(0).getSubject().contains("fraud"));
        d.sys.confirmFraud(suspicious.get(0));
        assertTrue(d.ben.isBlocked());
        assertNull(d.sys.login("ben", "password"));
        assertEquals(Transaction.Type.REFUND, d.sys.getTransactions().get(d.sys.getTransactions().size() - 1).getType());
    }

    @Test
    void tooManyPointsInOneDayIsSuspicious() {
        d.sys.getTransactions().add(new Transaction(d.ana, Transaction.Type.POINTS_AWARD, 200, "x", LocalDateTime.now().minusMinutes(30)));
        d.sys.getTransactions().add(new Transaction(d.ana, Transaction.Type.POINTS_AWARD, 150, "x", LocalDateTime.now()));
        assertEquals(1, d.sys.runFraudCheck().size());
    }

    @Test
    void disputesAreAssignedAndCorrected() {
        Dispute points = d.sys.submitDispute(d.ana, Dispute.Type.REWARD_POINTS, null, "No points", "", 20);
        assertSame(d.mira, points.getAssignedOfficer());
        d.sys.resolveDispute(points, true, "Fixed");
        assertEquals(20, d.ana.getPoints());

        Bill b = new Bill(d.ana, "wrong", 1000, LocalDate.now(), LocalDate.now().plusDays(30));
        Dispute billing = d.sys.submitDispute(d.ana, Dispute.Type.BILLING_ERROR, b, "Too high", "photo.png", 300);
        d.sys.resolveDispute(billing, true, "Corrected");
        assertEquals(700, b.getBaseAmount(), 0.001);
        assertEquals(Dispute.Status.APPROVED, billing.getStatus());
    }
}
