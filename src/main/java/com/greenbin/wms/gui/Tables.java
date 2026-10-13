package com.greenbin.wms.gui;

import javax.swing.table.DefaultTableModel;

/** Small helper for read-only table models. */
public final class Tables {
    private Tables() { }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    public static String money(double lek) { return String.format("%,.2f Lek", lek); }
    public static String kg(double kg) { return String.format("%.1f kg", kg); }
}
