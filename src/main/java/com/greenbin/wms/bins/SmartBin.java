package com.greenbin.wms.bins;

import com.greenbin.wms.model.BinStatus;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A smart bin for one type of waste.
 * FR_08 barcode scan, FR_12 reward points, FR_14 wrong waste detection.
 */
public class SmartBin extends SmartContainer {
    /** Wrong (but not hazardous) waste still gets a quarter of the points: "not rewarded fully" (FR_12). */
    public static final double WRONG_WASTE_POINTS = 0.25;

    private final WasteType wasteType;
    private final List<Waste> contents = new ArrayList<>();
    private final List<WasteReport> errors = new ArrayList<>();
    private String displayMessage = "Ready";
    private User currentUser;

    public SmartBin(String location, Coordinate coordinate, double capacity, long barcodeSerial, WasteType wasteType) {
        super(location, coordinate, capacity, barcodeSerial);
        this.wasteType = wasteType;
    }

    /** A citizen scans the barcode and throws something in. */
    public DisposalRecord dispose(User user, Waste waste) {
        getBarcode().scanBarcode();
        currentUser = user;
        waste.setBinType(wasteType);
        double correct = getSensor().getCorrectWastePercentage(wasteType, waste);
        boolean isCorrect = correct >= 1.0;

        int points;
        if (isCorrect) {
            points = addPointsToUser(waste.calculatePoints());
            displayMessage = "Thank you! +" + points + " points";
        } else {
            double share = waste.getType().isHazardous() ? 0 : WRONG_WASTE_POINTS;
            points = addPointsToUser(waste.calculatePoints(), share);
            waste.setFlagged(true);
            displayIncorrectWasteDisposed(waste);
        }
        contents.add(waste);
        updateFillLevel(waste.getWeight());

        DisposalRecord record = new DisposalRecord(user, this, waste, isCorrect, points, LocalDateTime.now());
        user.getDisposalHistory().add(record);
        return record;
    }

    public boolean verifyWaste(WasteType type) { return type == wasteType; }

    public int addPointsToUser(int points) {
        currentUser.addPoints(points);
        return points;
    }

    public int addPointsToUser(int points, double percentage) {
        return addPointsToUser((int) Math.round(points * percentage));
    }

    /** FR_14 step 5: the bin's screen shows that wrong waste is inside. */
    public void displayIncorrectWasteDisposed(Waste waste) {
        displayMessage = "Wrong bin! " + waste.getType() + " does not go in " + wasteType;
        errors.add(new WasteReport(getLocation() + " " + wasteType, "Incorrect waste",
                String.format("%.1f kg %s thrown into the %s bin", waste.getWeight(), waste.getType(), wasteType),
                LocalDateTime.now()));
    }

    /** How many wrong disposals in the last days (FR_14 step 4). */
    public int countRecentErrors(int days) {
        LocalDateTime limit = LocalDateTime.now().minusDays(days);
        int count = 0;
        for (WasteReport r : errors) if (r.getTimestamp().isAfter(limit)) count++;
        return count;
    }

    /** Emptying the bin gives its contents to the truck. */
    public List<Waste> empty() {
        List<Waste> taken = new ArrayList<>(contents);
        contents.clear();
        resetContainer();
        displayMessage = "Ready";
        return taken;
    }

    public BinStatus getStatus() { return BinStatus.fromPercent(getFillPercent()); }

    @Override public String getKind() { return wasteType + " bin"; }

    public WasteType getWasteType() { return wasteType; }
    public List<Waste> getContents() { return contents; }
    public List<WasteReport> getErrors() { return errors; }
    public String getDisplayMessage() { return displayMessage; }
    public User getCurrentUser() { return currentUser; }

    @Override
    public String toString() { return wasteType + " bin - " + getLocation(); }
}
