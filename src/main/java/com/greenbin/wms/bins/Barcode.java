package com.greenbin.wms.bins;

/** FR_08: every smart container has a unique barcode that citizens scan with the app. */
public class Barcode {
    private final long serialNumber;
    private int scanCount;

    public Barcode(long serialNumber) { this.serialNumber = serialNumber; }

    public long scanBarcode() {
        scanCount++;
        return serialNumber;
    }

    public long getSerialNumber() { return serialNumber; }
    public int getScanCount() { return scanCount; }

    @Override
    public String toString() { return String.valueOf(serialNumber); }
}
