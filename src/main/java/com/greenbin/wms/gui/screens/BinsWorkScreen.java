package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.SmartBin;
import com.greenbin.wms.bins.Waste;
import com.greenbin.wms.collection.BinPlacementOrder;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** FR_11 placing new bins, FR_09 emptying full bins (their waste goes to the plant, flagged waste separately - FR_17). */
public class BinsWorkScreen extends Screen {
    private final DefaultTableModel orders = Tables.model("Order", "Bin type", "Location", "Assigned to", "Status", "Issue");
    private final JTable orderTable = Theme.newTable(orders);
    private final List<BinPlacementOrder> orderRows = new ArrayList<>();
    private final DefaultTableModel bins = Tables.model("Bin", "Location", "Fill", "Status", "Wrong items inside");
    private final JTable binTable = Theme.newTable(bins);
    private final List<SmartBin> binRows = new ArrayList<>();

    public BinsWorkScreen(Account me) {
        super(me, "Bins: Place & Empty", "Place the bins the manager ordered, and empty the fullest bins first", "FR_09", "FR_11", "FR_17");
        orderTable.getColumnModel().getColumn(4).setCellRenderer(new Theme.StatusRenderer());
        binTable.getColumnModel().getColumn(3).setCellRenderer(new Theme.StatusRenderer());
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 10));
        grid.setOpaque(false);

        JPanel o = new JPanel(new BorderLayout(0, 6));
        o.setOpaque(false);
        o.add(Theme.table(orderTable), BorderLayout.CENTER);
        o.add(row(FlatButton.primary("Bin is placed - confirm", e -> {
            BinPlacementOrder ord = selectedOrder();
            if (ord == null) return;
            SmartBin b = sys.confirmBinPlacement(ord);
            info("Confirmed. The new bin (barcode " + b.getBarcode() + ") is now on the map.");
            refresh();
        }), FlatButton.danger("Report a problem...", e -> {
            BinPlacementOrder ord = selectedOrder();
            if (ord == null) return;
            String issue = JOptionPane.showInputDialog(this, "What is the problem?", "Restricted area - no permission to place a bin");
            if (issue == null || issue.isBlank()) return;
            sys.reportPlacementIssue(ord, issue.trim());
            info("The manager was alerted.");
            refresh();
        })), BorderLayout.SOUTH);
        grid.add(Theme.card("Bin placement orders (FR_11)", o));

        JPanel b = new JPanel(new BorderLayout(0, 6));
        b.setOpaque(false);
        b.add(Theme.table(binTable), BorderLayout.CENTER);
        b.add(row(FlatButton.primary("Empty selected bin & take to plant", e -> {
            int r = binTable.getSelectedRow();
            if (r < 0) { error("Select a bin first."); return; }
            int flagged = 0;
            for (Waste w : sys.emptyBin(binRows.get(r))) if (w.isFlagged()) flagged++;
            info("Bin emptied." + (flagged > 0 ? " " + flagged + " wrong item(s) put in the truck's separate compartment." : ""));
            refresh();
        })), BorderLayout.SOUTH);
        grid.add(Theme.card("Smart bins, fullest first (FR_09)", b));
        body().add(grid, BorderLayout.CENTER);
    }

    private BinPlacementOrder selectedOrder() {
        int r = orderTable.getSelectedRow();
        if (r < 0) { error("Select an order first."); return null; }
        BinPlacementOrder o = orderRows.get(r);
        if (o.getStatus() != BinPlacementOrder.Status.ORDERED) { error("This order is already closed."); return null; }
        return o;
    }

    @Override
    public void refresh() {
        orderRows.clear();
        orderRows.addAll(sys.getBinPlacementMap().getOrders());
        orders.setRowCount(0);
        for (BinPlacementOrder o : orderRows) {
            orders.addRow(new Object[]{"#" + o.getOrderNumber(), o.getWasteType(), o.getLocation().getName(),
                    o.getAssignedTo() == null ? "-" : o.getAssignedTo(), o.getStatus(), o.getIssue()});
        }
        binRows.clear();
        binRows.addAll(sys.getBins());
        binRows.sort((x, y) -> Double.compare(y.getFillPercent(), x.getFillPercent()));
        bins.setRowCount(0);
        for (SmartBin s : binRows) {
            int wrong = 0;
            for (Waste w : s.getContents()) if (w.isFlagged()) wrong++;
            bins.addRow(new Object[]{s.getWasteType() + " bin", s.getLocation(),
                    s.getSensor().isWorking() ? Math.round(s.getFillPercent()) + "%" : "sensor broken", s.getStatus(), wrong});
        }
    }
}
