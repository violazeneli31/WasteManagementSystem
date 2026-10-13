package com.greenbin.wms.recognition;

import com.greenbin.wms.model.WasteType;

import java.awt.image.BufferedImage;

/**
 * FR_22: the image recognition model. In the design it is a CNN trained on waste images
 * (TensorFlow). Here it is an interface, so a real model can be plugged in later.
 */
public interface WasteClassifierModel {
    void loadModel();

    /** Returns the predicted waste type, or null if it can't tell. */
    WasteType predictImage(String fileName, BufferedImage image);

    /** How sure the model is about its last prediction (0..1). */
    double getLastConfidence();
}
