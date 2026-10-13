package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.community.Donation;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/** FR_27: leave reusable items at a donation container, earn points, follow where they go. */
public class DonateScreen extends Screen {
    private final User user;
    private final JComboBox<DonationContainer> container = new JComboBox<>();
    private final JComboBox<String> item = new JComboBox<>(new String[]{"Clothes", "Shoes", "Books", "Toys", "Blankets", "Kitchen items", "School supplies", "Small furniture"});
    private final DefaultTableModel model = Tables.model("Date", "Item", "Container", "Status", "Agency");

    public DonateScreen(Account me) {
        super(me, "Donate Items", "Give things a second life. Each donation = " + Donation.DONATION_POINTS + " points.", "FR_27");
        user = (User) me;
        JPanel form = new Stack();
        form.setOpaque(false);
        form.add(muted("Collection center"));
        form.add(container);
        form.add(muted("What are you donating?"));
        form.add(item);
        form.add(FlatButton.primary("Scan item barcode & donate", e -> {
            sys.donate(user, (DonationContainer) container.getSelectedItem(), (String) item.getSelectedItem());
            info("Thank you! +" + Donation.DONATION_POINTS + " points. We will tell you when an agency collects it.");
            refresh();
        }));
        JPanel card = Theme.card("Donate", form);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(320, 100));
        left.add(card, BorderLayout.NORTH);
        body().add(left, BorderLayout.WEST);
        JTable t = Theme.newTable(model);
        t.getColumnModel().getColumn(3).setCellRenderer(new Theme.StatusRenderer());
        body().add(Theme.card("My donations and their impact", Theme.table(t)), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Object sel = container.getSelectedItem();
        container.removeAllItems();
        for (DonationContainer c : sys.getDonationContainers()) container.addItem(c);
        if (sel != null) container.setSelectedItem(sel);
        model.setRowCount(0);
        for (DonationContainer c : sys.getDonationContainers()) {
            for (Donation d : c.getDonations()) {
                if (d.getCitizen() != user) continue;
                model.addRow(new Object[]{d.getDate().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), d.getItemCategory(), c.getLocation(),
                        d.getStatus() == Donation.Status.DROPPED_OFF ? "WAITING FOR AGENCY" : d.getStatus() == Donation.Status.DISTRIBUTED ? "DONE - GIVEN TO A FAMILY" : "COLLECTED - ACTIVE",
                        d.getAgency() == null ? "-" : d.getAgency()});
            }
        }
    }
}
