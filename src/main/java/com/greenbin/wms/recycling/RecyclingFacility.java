package com.greenbin.wms.recycling;

import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.RecyclingFacilityOperator;
import com.greenbin.wms.recognition.ImageRecognitionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The recycling plant.
 * FR_17: flagged waste arrives in a separate compartment, AI checks it, the operator corrects it.
 * FR_18: waste is sorted by the bin it came from; metal and glass are separated here.
 * FR_19: every batch goes through stages and its conversion rate is tracked.
 */
public class RecyclingFacility {
    public enum Stage { RECEIVED, SORTED, PROCESSING, RECYCLED }

    /** A quantity of one material at one stage. */
    public static class Batch {
        private final String material;
        private double kg;
        private Stage stage = Stage.RECEIVED;
        private double outputKg;

        Batch(String material, double kg) {
            this.material = material;
            this.kg = kg;
        }

        public String getMaterial() { return material; }
        public double getKg() { return kg; }
        public Stage getStage() { return stage; }
        public double getOutputKg() { return outputKg; }
        @Override public String toString() { return String.format("%s %.0f kg", material, kg); }
    }

    public static final String METAL_AND_GLASS = "Metal & Glass";
    /** How much of each material becomes new material (the rest is lost in processing). */
    public static final Map<String, Double> CONVERSION_RATE = new LinkedHashMap<>();
    /** Sale price of recycled material, Lek per kg. */
    public static final Map<String, Double> SALE_PRICE = new LinkedHashMap<>();

    static {
        CONVERSION_RATE.put("Paper", 0.85);   SALE_PRICE.put("Paper", 8.0);
        CONVERSION_RATE.put("Plastic", 0.70); SALE_PRICE.put("Plastic", 15.0);
        CONVERSION_RATE.put("Metal", 0.95);   SALE_PRICE.put("Metal", 40.0);
        CONVERSION_RATE.put("Glass", 0.90);   SALE_PRICE.put("Glass", 5.0);
        CONVERSION_RATE.put("Organic", 0.60); SALE_PRICE.put("Organic", 3.0);
        CONVERSION_RATE.put("Hazardous", 0.30); SALE_PRICE.put("Hazardous", 0.0);
    }

    private final UUID facilityID = UUID.randomUUID();
    private final String name;
    private final String address;
    private final List<RecyclingFacilityOperator> operators = new ArrayList<>();
    private final List<Batch> batches = new ArrayList<>();
    private final List<Waste> flaggedCompartment = new ArrayList<>();
    private final List<Waste> manualInspection = new ArrayList<>();
    private final List<String> misclassifiedDetails = new ArrayList<>();
    private final List<String> issues = new ArrayList<>();
    private final List<RecyclingReport> reports = new ArrayList<>();

    public RecyclingFacility(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public static String materialOf(WasteType type) {
        switch (type) {
            case PAPER: return "Paper";
            case PLASTIC: return "Plastic";
            case METAL_AND_GLASS: return METAL_AND_GLASS;
            case ORGANIC_WASTE: return "Organic";
            default: return "Hazardous";
        }
    }

    /** A truck unloads. Flagged waste goes to its own compartment (FR_17), the rest by bin type (FR_18). */
    public void receiveFromVehicle(List<Waste> load) {
        for (Waste w : load) {
            w.setRecyclingStage("At plant");
            if (w.isFlagged()) flaggedCompartment.add(w);
            else addToBatch(materialOf(w.getBinType()), w.getWeight());
        }
    }

    /** Adds kg to the RECEIVED batch of a material (or starts a new one). */
    public void addToBatch(String material, double kg) {
        for (Batch b : batches) {
            if (b.material.equals(material) && b.stage == Stage.RECEIVED) {
                b.kg += kg;
                return;
            }
        }
        batches.add(new Batch(material, kg));
    }

    /** FR_17 step 2-3: AI checks every flagged item. Confirmed errors go to manual inspection. Returns how many. */
    public int verifyFlaggedWaste(ImageRecognitionService ai) {
        int confirmed = 0;
        for (Waste w : new ArrayList<>(flaggedCompartment)) {
            flaggedCompartment.remove(w);
            WasteType seen = ai.analyzeWaste(w);
            if (seen != w.getBinType()) {
                w.setRecyclingStage("Manual inspection");
                manualInspection.add(w);
                confirmed++;
            } else {
                addToBatch(materialOf(w.getBinType()), w.getWeight());   // false alarm
            }
        }
        return confirmed;
    }

    /** FR_17 step 4: the operator puts the item in the right category. */
    public void correctMisclassifiedWaste(Waste w, WasteType correctType, RecyclingFacilityOperator operator) {
        if (!manualInspection.remove(w)) return;
        w.setRecyclingStage("Reclassified");
        misclassifiedDetails.add(String.format("%.1f kg %s found in %s bin, moved to %s by %s",
                w.getWeight(), w.getType(), w.getBinType(), correctType, operator.getDisplayName()));
        addToBatch(materialOf(correctType), w.getWeight());
    }

    /** FR_18: received waste is sorted by the bin it came from. Returns how many batches moved. */
    public int sortReceivedWaste() {
        int moved = 0;
        for (Batch b : batches) {
            if (b.stage == Stage.RECEIVED) { b.stage = Stage.SORTED; moved++; }
        }
        return moved;
    }

    /** FR_18 step 4: separate sorted metal & glass, about 40% metal and 60% glass. Returns kg separated. */
    public double separateMetalFromGlass() {
        double total = 0;
        for (Batch b : new ArrayList<>(batches)) {
            if (b.material.equals(METAL_AND_GLASS) && b.stage == Stage.SORTED) {
                batches.remove(b);
                Batch metal = new Batch("Metal", b.kg * 0.4);
                Batch glass = new Batch("Glass", b.kg * 0.6);
                metal.stage = Stage.SORTED;
                glass.stage = Stage.SORTED;
                batches.add(metal);
                batches.add(glass);
                total += b.kg;
            }
        }
        return total;
    }

    /** FR_19: moves a batch one stage forward. Metal & glass must be separated first. */
    public boolean moveToNextStage(Batch b) {
        if (b.stage == Stage.RECYCLED) return false;
        if (b.stage == Stage.RECEIVED) return false;                         // sort first
        if (b.material.equals(METAL_AND_GLASS)) return false;                // separate first
        if (b.stage == Stage.SORTED) {
            b.stage = Stage.PROCESSING;
        } else {
            b.stage = Stage.RECYCLED;
            b.outputKg = b.kg * CONVERSION_RATE.get(b.material);
        }
        return true;
    }

    public List<Batch> getBatches(Stage stage) {
        List<Batch> result = new ArrayList<>();
        for (Batch b : batches) if (b.stage == stage) result.add(b);
        return result;
    }

    public double getKgAtStage(Stage stage) {
        double sum = 0;
        for (Batch b : getBatches(stage)) sum += b.kg;
        return sum;
    }

    /** FR_19: kg in and kg out of recycled batches, per material. */
    public Map<String, double[]> getConversionData() {
        Map<String, double[]> data = new LinkedHashMap<>();
        for (Batch b : getBatches(Stage.RECYCLED)) {
            double[] inOut = data.computeIfAbsent(b.material, m -> new double[2]);
            inOut[0] += b.kg;
            inOut[1] += b.outputKg;
        }
        return data;
    }

    /** Overall efficiency (output / input) of everything recycled so far, in %. */
    public double getEfficiency() {
        double in = 0, out = 0;
        for (double[] v : getConversionData().values()) { in += v[0]; out += v[1]; }
        return in == 0 ? 0 : 100 * out / in;
    }

    /** Money earned by selling the recycled material. */
    public double getRevenue() {
        double sum = 0;
        for (Map.Entry<String, double[]> e : getConversionData().entrySet()) sum += e.getValue()[1] * SALE_PRICE.get(e.getKey());
        return sum;
    }

    public void reportIssue(String issue) { issues.add(issue); }

    public RecyclingReport generateReport(RecyclingFacilityOperator operator) {
        Map<String, Double> summary = new LinkedHashMap<>();
        for (Batch b : batches) summary.merge(b.material + " (" + b.stage + ")", b.kg, Double::sum);
        RecyclingReport report = new RecyclingReport(name, operator.getDisplayName(), summary,
                new ArrayList<>(misclassifiedDetails), new ArrayList<>(issues), getEfficiency(), getRevenue());
        reports.add(0, report);
        return report;
    }

    public UUID getFacilityID() { return facilityID; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public List<RecyclingFacilityOperator> getOperators() { return operators; }
    public List<Batch> getBatches() { return batches; }
    public List<Waste> getFlaggedCompartment() { return flaggedCompartment; }
    public List<Waste> getManualInspection() { return manualInspection; }
    public List<String> getMisclassifiedDetails() { return misclassifiedDetails; }
    public List<String> getIssues() { return issues; }
    public List<RecyclingReport> getReports() { return reports; }
}
