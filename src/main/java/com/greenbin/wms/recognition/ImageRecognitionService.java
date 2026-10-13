package com.greenbin.wms.recognition;

import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.model.WasteType;

import java.awt.image.BufferedImage;

/** FR_22 and FR_17: analyses a photo (or a flagged piece of waste) with the classifier model. */
public class ImageRecognitionService {
    private final WasteClassifierModel classifier;

    public ImageRecognitionService(WasteClassifierModel classifier) {
        this.classifier = classifier;
        classifier.loadModel();
    }

    public ClassificationResult analyzeImage(String fileName, BufferedImage image) {
        WasteType type = classifier.predictImage(fileName, image);
        return new ClassificationResult(type, classifier.getLastConfidence());
    }

    /** FR_17: at the plant the cameras look at a flagged piece of waste. Here the camera "sees" its real type. */
    public WasteType analyzeWaste(Waste waste) {
        return classifier.predictImage(waste.getType().name().toLowerCase().replace("metal_and_glass", "glass jar")
                .replace("organic_waste", "food").replace("hazardous", "battery"), null);
    }
}
