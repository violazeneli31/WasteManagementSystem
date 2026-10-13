package com.greenbin.wms;

import com.greenbin.wms.gui.AppFrame;
import com.greenbin.wms.gui.Theme;

import javax.swing.SwingUtilities;

/** Starts the GreenBin desktop application. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new AppFrame().setVisible(true);
        });
    }
}
