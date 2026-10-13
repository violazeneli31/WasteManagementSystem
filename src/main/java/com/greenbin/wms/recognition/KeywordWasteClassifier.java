package com.greenbin.wms.recognition;

import com.greenbin.wms.model.WasteType;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple stand-in for the CNN model so the project runs without TensorFlow.
 * 1) it looks for known words in the file name (bottle, can, banana, battery...);
 * 2) if there are none, it looks at the main colour of the picture.
 */
public class KeywordWasteClassifier implements WasteClassifierModel {
    private final Map<String, WasteType> keywords = new LinkedHashMap<>();
    private double lastConfidence;

    @Override
    public void loadModel() {
        String[][] words = {
                {"battery", "HAZARDOUS"}, {"paint", "HAZARDOUS"}, {"medicine", "HAZARDOUS"}, {"bulb", "HAZARDOUS"}, {"chemical", "HAZARDOUS"},
                {"bottle", "PLASTIC"}, {"plastic", "PLASTIC"}, {"bag", "PLASTIC"}, {"cup", "PLASTIC"}, {"straw", "PLASTIC"},
                {"can", "METAL_AND_GLASS"}, {"tin", "METAL_AND_GLASS"}, {"jar", "METAL_AND_GLASS"}, {"glass", "METAL_AND_GLASS"}, {"foil", "METAL_AND_GLASS"},
                {"paper", "PAPER"}, {"newspaper", "PAPER"}, {"box", "PAPER"}, {"carton", "PAPER"}, {"cardboard", "PAPER"}, {"book", "PAPER"},
                {"banana", "ORGANIC_WASTE"}, {"apple", "ORGANIC_WASTE"}, {"food", "ORGANIC_WASTE"}, {"peel", "ORGANIC_WASTE"},
                {"leaf", "ORGANIC_WASTE"}, {"coffee", "ORGANIC_WASTE"}, {"egg", "ORGANIC_WASTE"}
        };
        for (String[] w : words) keywords.put(w[0], WasteType.valueOf(w[1]));
    }

    @Override
    public WasteType predictImage(String fileName, BufferedImage image) {
        if (keywords.isEmpty()) loadModel();
        String name = fileName == null ? "" : fileName.toLowerCase();
        for (Map.Entry<String, WasteType> k : keywords.entrySet()) {
            if (name.contains(k.getKey())) {
                lastConfidence = 0.94;
                return k.getValue();
            }
        }
        if (image != null) {
            lastConfidence = 0.61;
            return byColour(image);
        }
        lastConfidence = 0;
        return null;
    }

    /** Very rough guess from the average colour of the picture. */
    private WasteType byColour(BufferedImage image) {
        long r = 0, g = 0, b = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int rgb = image.getRGB(x, y);
                r += (rgb >> 16) & 255; g += (rgb >> 8) & 255; b += rgb & 255;
                count++;
            }
        }
        r /= count; g /= count; b /= count;
        if (g > r && g > b) return WasteType.ORGANIC_WASTE;           // green
        if (r > 150 && g < 100 && b < 100) return WasteType.HAZARDOUS; // strong red
        if (b > r && b > g) return WasteType.PLASTIC;                  // blue
        if (r > 200 && g > 200 && b > 200) return WasteType.PAPER;     // white
        if (Math.abs(r - g) < 20 && Math.abs(g - b) < 20) return WasteType.METAL_AND_GLASS; // grey
        return WasteType.PAPER;                                        // brown / beige: cardboard
    }

    @Override
    public double getLastConfidence() { return lastConfidence; }
}
