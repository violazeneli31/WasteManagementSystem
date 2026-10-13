package com.greenbin.wms;

import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.community.App;
import com.greenbin.wms.community.Business;
import com.greenbin.wms.community.BusinessVoucher;
import com.greenbin.wms.community.Donation;
import com.greenbin.wms.community.DonationAgency;
import com.greenbin.wms.community.Leaderboard;
import com.greenbin.wms.community.Report;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.recognition.ClassificationResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** FR_10, FR_20, FR_22-FR_27 and login (NFR_13/14). */
class CommunityTest {
    private final TestData d = new TestData();

    private LocalDate workingDay() {
        LocalDate day = LocalDate.now().plusDays(2);
        return day.getDayOfWeek() == DayOfWeek.SUNDAY ? day.plusDays(1) : day;
    }

    @Test
    void loginChecksPasswordAndRoleNeedsTwoFactor() {
        Account manager = d.sys.login("viola", "password");
        assertSame(d.manager, manager);
        assertTrue(d.sys.needsTwoFactor(manager));
        assertFalse(d.sys.needsTwoFactor(d.sys.login("ana", "password")));
        assertNull(d.sys.login("ana", "wrong"));
        String code = d.sys.getTwoFactorAuth().sendCode();
        assertFalse(d.sys.getTwoFactorAuth().verify("000000x"));
        assertTrue(d.sys.getTwoFactorAuth().verify(code));
    }

    @Test
    void profileCanBeEditedAndPasswordIsHashed() {
        d.ana.updateProfile("ana.m", "+355 69 1", "new@mail.al", "");
        assertEquals("ana.m", d.ana.getUsername());
        assertEquals("Rr. Kavajes 10", d.ana.getAddress());        // empty fields are ignored
        assertThrows(IllegalArgumentException.class, () -> d.ana.updateProfile("", "", "not-an-email", ""));
        assertTrue(d.ana.changePassword("password", "secret123"));
        assertNotNull(d.sys.login("ana.m", "secret123"));
        assertEquals("n***@mail.al", d.ana.getMaskedEmail());
    }

    @Test
    void reportsGoToTheRightDepartmentAndTheSenderIsUpdated() {
        Report r = d.sys.submitReport(d.ben, "Bin overflowing", "The bin on my street is full");
        assertEquals(Report.Category.BIN_ISSUE, r.getCategory());
        assertEquals(Report.Category.BILLING, Report.Category.categorize("My bill is wrong"));
        assertEquals(Report.Category.SUGGESTION, Report.Category.categorize("I suggest more bins"));
        d.sys.resolveReport(r, "Emptied today");
        assertEquals(Report.Status.RESOLVED, r.getStatus());
        assertEquals("Your report was resolved", d.ben.getNotifications().get(0).getSubject());
    }

    @Test
    void imageRecognitionFindsTheBin() {
        ClassificationResult bottle = d.sys.getRecognition().analyzeImage("plastic_bottle.jpg", null);
        assertEquals(WasteType.PLASTIC, bottle.getType());
        assertTrue(bottle.isRecyclable());
        assertTrue(d.sys.getRecognition().analyzeImage("old_battery.png", null).isHazardous());
        assertFalse(d.sys.getRecognition().analyzeImage("something.png", null).isRecognized());
    }

    @Test
    void pickupRequestIsConfirmedByACollector() {
        Pickup p = d.sys.requestPickup(d.ana, WasteType.HAZARDOUS, workingDay(), LocalTime.of(10, 0), "Rr. Kavajes 10", 8);
        assertEquals("New pickup request", d.ardi.getNotifications().get(0).getSubject());
        d.sys.respondToPickup(p, d.ardi, true, "");
        assertEquals(Pickup.Status.CONFIRMED, p.getStatus());
        assertNotNull(p.getBill());
        Schedule s = d.sys.getSchedulesOf(d.ardi).get(0);
        assertTrue(s.isSpecial());
        assertEquals("Pickup confirmed", d.ana.getNotifications().get(0).getSubject());
    }

    @Test
    void deniedOrInvalidPickups() {
        Pickup p = d.sys.requestPickup(d.ben, WasteType.PAPER, workingDay(), LocalTime.of(9, 0), "Rr. Durresit 5", 5);
        d.sys.respondToPickup(p, d.elira, false, "Truck full");
        assertEquals(Pickup.Status.DENIED, p.getStatus());
        assertNull(p.getBill());
        assertThrows(IllegalArgumentException.class,
                () -> d.sys.requestPickup(d.ben, WasteType.PAPER, LocalDate.now(), LocalTime.of(9, 0), "x", 5));
    }

    @Test
    void leaderboardRanksUsersAndGivesTheBadge() {
        d.ben.addPoints(500);
        d.ana.addPoints(100);
        Leaderboard lb = d.sys.getLeaderboard();
        assertSame(d.ben, lb.rankUsers().get(0));
        assertEquals("Kombinat", lb.updateLeaderboard());
        assertTrue(d.ben.getBadges().contains(Leaderboard.TOP_BADGE));
        assertFalse(d.ana.getBadges().contains(Leaderboard.TOP_BADGE));
    }

    @Test
    void vouchersCostPointsAndWorkOnce() {
        Business cafe = new Business("Green Cafe", "Blloku", "1", "c@c.al");
        cafe.addOffer("Free coffee", 50);
        assertNull(d.sys.redeemPoints(d.ana, cafe, cafe.getOffers().get(0)));    // 0 points
        d.ana.addPoints(60);
        BusinessVoucher v = d.sys.redeemPoints(d.ana, cafe, cafe.getOffers().get(0));
        assertNotNull(v);
        assertEquals(10, d.ana.getPoints());
        assertTrue(v.redeem());
        assertFalse(v.redeem());
        assertEquals(1, cafe.getVouchersRedeemed());
    }

    @Test
    void adsFollowActivityAndDisabledAppsShowNothing() {
        App shop = new App("EcoShop", true);
        shop.addAd(new App.Advertisement("Bottles", "", WasteType.PLASTIC));
        shop.addAd(new App.Advertisement("Notebooks", "", WasteType.PAPER));
        d.sys.getApps().add(shop);
        com.greenbin.wms.bins.SmartBin bin = new com.greenbin.wms.bins.SmartBin("X", new Coordinate(41.33, 19.81), 100, 1, WasteType.PLASTIC);
        d.sys.disposeWaste(d.ana, bin, WasteType.PLASTIC, 2);
        assertEquals("Bottles", d.sys.getAdsFor(d.ana).get(0).getTitle());
        assertEquals(1, d.sys.getAdsFor(d.ana).size());
        d.sys.setAppEnabled(shop, false);
        assertTrue(d.sys.getAdsFor(d.ana).isEmpty());
    }

    @Test
    void donationsGivePointsAndTheManagerIsToldAtTheThreshold() {
        DonationContainer c = new DonationContainer("Rr. Durresit", new Coordinate(41.329, 19.81), 100, 5);
        d.sys.getContainers().add(c);
        for (int i = 0; i < 10; i++) d.sys.donate(d.ana, c, "Books");
        assertEquals(10 * Donation.DONATION_POINTS, d.ana.getPoints());
        assertEquals("Donations ready", d.manager.getNotifications().get(0).getSubject());
        DonationAgency agency = new DonationAgency("Red Cross", "", "", "");
        assertEquals(10, d.sys.sendToAgency(c, agency));
        assertEquals(10, d.sys.markDistributed(agency));
        assertEquals(Donation.Status.DISTRIBUTED, c.getDonations().get(0).getStatus());
    }
}
