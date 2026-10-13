package com.greenbin.wms.bins;

import com.greenbin.wms.model.WasteType;

/** The sensor inside a container: it measures the fill level and checks the material thrown in. */
public class Sensor {
    private boolean working = true;

    /** 1.0 if the waste matches the bin, 0.0 if it does not. */
    public double getCorrectWastePercentage(WasteType binType, Waste waste) {
        return binType == waste.getType() ? 1.0 : 0.0;
    }

    public boolean isWorking() { return working; }
    public void setWorking(boolean working) { this.working = working; }
}
