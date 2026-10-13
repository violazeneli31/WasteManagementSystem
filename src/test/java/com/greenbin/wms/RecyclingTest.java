package com.greenbin.wms;

import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.recycling.RecyclingFacility;
import com.greenbin.wms.recycling.RecyclingFacility.Batch;
import com.greenbin.wms.recycling.RecyclingFacility.Stage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** FR_17 flagged waste, FR_18 sorting, FR_19 tracking. */
class RecyclingTest {
    private final TestData d = new TestData();
    private final RecyclingFacility plant = d.sys.getFacility();

    private Waste thrown(WasteType real, WasteType bin, boolean flagged) {
        Waste w = new Waste(real, 2);
        w.setBinType(bin);
        w.setFlagged(flagged);
        return w;
    }

    @Test
    void flaggedWasteIsCheckedByAiThenByTheOperator() {
        Waste wrong = thrown(WasteType.PAPER, WasteType.PLASTIC, true);
        Waste falseAlarm = thrown(WasteType.PLASTIC, WasteType.PLASTIC, true);
        plant.receiveFromVehicle(Arrays.asList(wrong, falseAlarm, thrown(WasteType.PAPER, WasteType.PAPER, false)));
        assertEquals(2, plant.getFlaggedCompartment().size());

        assertEquals(1, plant.verifyFlaggedWaste(d.sys.getRecognition()));
        assertEquals(1, plant.getManualInspection().size());
        plant.correctMisclassifiedWaste(wrong, WasteType.PAPER, d.toni);
        assertTrue(plant.getManualInspection().isEmpty());
        assertEquals(1, plant.getMisclassifiedDetails().size());
        assertEquals(6, plant.getKgAtStage(Stage.RECEIVED), 0.001);
    }

    @Test
    void metalAndGlassMustBeSeparatedBeforeProcessing() {
        plant.addToBatch(RecyclingFacility.METAL_AND_GLASS, 100);
        Batch mixed = plant.getBatches().get(0);
        assertFalse(plant.moveToNextStage(mixed));            // not sorted yet
        plant.sortReceivedWaste();
        assertFalse(plant.moveToNextStage(mixed));            // not separated yet
        assertEquals(100, plant.separateMetalFromGlass(), 0.001);
        assertEquals(2, plant.getBatches().size());
        assertEquals(40, plant.getBatches().get(0).getKg(), 0.001);   // metal
        assertEquals(60, plant.getBatches().get(1).getKg(), 0.001);   // glass
    }

    @Test
    void recycledBatchesGiveConversionRatesAndRevenue() {
        plant.addToBatch("Paper", 1000);
        plant.sortReceivedWaste();
        Batch paper = plant.getBatches().get(0);
        assertTrue(plant.moveToNextStage(paper));
        assertTrue(plant.moveToNextStage(paper));
        assertEquals(Stage.RECYCLED, paper.getStage());
        assertEquals(850, paper.getOutputKg(), 0.001);
        assertEquals(85, plant.getEfficiency(), 0.001);
        assertEquals(850 * 8, plant.getRevenue(), 0.001);
        assertFalse(plant.moveToNextStage(paper));
        assertTrue(plant.generateReport(d.toni).showReport().contains("Paper"));
    }
}
