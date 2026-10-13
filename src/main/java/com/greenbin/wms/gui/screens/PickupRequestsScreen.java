package com.greenbin.wms.gui.screens;

import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Inputs;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** FR_23: collectors see pickup requests and confirm or deny them. Confirming adds a shift and creates the bill. */
public class PickupRequestsScreen extends Screen {
    private final DefaultTableModel model = Tables.model("Requested by", "Waste", "Date", "Time", "Address", "Weight", "Status", "Handled by");
    private final JTable table = Theme.newTable(model);
    private final List<Pickup> rows = new ArrayList<>();

    public PickupRequestsScreen(Account me) {
        super(me, "Pickup Requests", "Special pickups for people who can't bring their waste to a bin", "FR_23");
        table.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        body().add(Theme.card("Requests", Theme.table(table)), BorderLayout.CENTER);
        body().add(row(FlatButton.primary("Confirm selected (I will collect it)", e -> answer(true)),
                FlatButton.danger("Deny selected...", e -> answer(false))), BorderLayout.SOUTH);
    }

    private void answer(boolean accept) {
        int r = table.getSelectedRow();
        if (r < 0) { error("Select a request first."); return; }
        Pickup p = rows.get(r);
        if (p.getStatus() != Pickup.Status.REQUESTED) { error("This request was already answered."); return; }
        String reason = "";
        if (!accept) {
            reason = JOptionPane.showInputDialog(this, "Why can't it be collected?", "Truck is full that day");
            if (reason == null) return;
        } else if (!((GarbageCollectionEmployee) me).isAvailable(p.getDate())) {
            error("You are on leave that day.");
            return;
        }
        sys.respondToPickup(p, (GarbageCollectionEmployee) me, accept, reason);
        info(accept ? "Confirmed. It is in your shifts and the citizen was notified." : "Denied. The citizen was notified.");
        refresh();
    }

    @Override
    public void refresh() {
        rows.clear();
        for (Pickup p : sys.getPickups()) if (p.getStatus() == Pickup.Status.REQUESTED || p.getHandledBy() == me) rows.add(p);
        model.setRowCount(0);
        for (Pickup p : rows) {
            model.addRow(new Object[]{p.getUser().getDisplayName(), p.getTypeOfWaste(), Inputs.day(p.getDate()), p.getTime(), p.getLocation(),
                    Tables.kg(p.getEstimatedWeightKg()), p.getStatus(), p.getHandledBy() == null ? "-" : p.getHandledBy()});
        }
    }
}
