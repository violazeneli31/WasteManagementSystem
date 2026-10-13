package com.greenbin.wms.gui.screens;

import com.greenbin.wms.bins.DonationContainer;
import com.greenbin.wms.community.App;
import com.greenbin.wms.community.Business;
import com.greenbin.wms.community.BusinessVoucher;
import com.greenbin.wms.community.Donation;
import com.greenbin.wms.community.DonationAgency;
import com.greenbin.wms.community.Leaderboard;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** The manager's view of FR_24 leaderboard, FR_25 business partners, FR_26 third-party apps and FR_27 donations. */
public class CommunityScreen extends Screen {
    private final BarChart communityChart = new BarChart();
    private final DefaultTableModel communities = Tables.model("Rank", "Community", "Members", "Points", "Correct disposals", "Score");
    private final DefaultTableModel users = Tables.model("Rank", "User", "Community", "Points", "Badges");

    private final DefaultTableModel businesses = Tables.model("Business", "Location", "Offers", "Vouchers redeemed");
    private final JTable businessTable = Theme.newTable(businesses);
    private final DefaultTableModel vouchers = Tables.model("Code", "User", "Business", "Offer", "Points", "Expires", "Status");
    private final JTextField offerText = new JTextField(16);
    private final JSpinner offerPoints = new JSpinner(new SpinnerNumberModel(100, 10, 5000, 10));

    private final DefaultTableModel apps = Tables.model("App", "Status", "Ads", "Impressions", "Clicks");
    private final JTable appTable = Theme.newTable(apps);
    private final DefaultTableModel ads = Tables.model("App", "Advertisement", "Target (waste type)", "Shown", "Clicked", "Click rate");

    private final DefaultTableModel containers = Tables.model("Container", "Items waiting", "Fill", "Collected", "Distributed");
    private final JTable containerTable = Theme.newTable(containers);
    private final JComboBox<DonationAgency> agency = new JComboBox<>();

    public CommunityScreen(Account me) {
        super(me, "Community & Partners", "Leaderboard, partner businesses, connected apps and donation agencies",
                "FR_24", "FR_25", "FR_26", "FR_27");
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);

        JPanel lb = new JPanel(new GridLayout(1, 2, 10, 0));
        lb.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(Theme.card("Community score = points per member + % correct disposals", communityChart), BorderLayout.CENTER);
        JPanel commCard = Theme.card(null, Theme.table(Theme.newTable(communities)));
        commCard.setPreferredSize(new java.awt.Dimension(100, 190));
        left.add(commCard, BorderLayout.SOUTH);
        lb.add(left);
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(Theme.card("Users", Theme.table(Theme.newTable(users))), BorderLayout.CENTER);
        right.add(row(FlatButton.primary("Update leaderboard & give badges", e -> {
            String winner = sys.getLeaderboard().updateLeaderboard();
            info("Top community: " + winner + "\nEvery member got the badge '" + Leaderboard.TOP_BADGE + "'.");
            refresh();
        })), BorderLayout.SOUTH);
        lb.add(right);
        tabs.addTab("Leaderboard (FR_24)", lb);

        JPanel biz = new JPanel(new BorderLayout(0, 8));
        biz.setOpaque(false);
        JPanel bizTop = new JPanel(new BorderLayout());
        bizTop.setOpaque(false);
        bizTop.add(Theme.table(businessTable), BorderLayout.CENTER);
        bizTop.add(row(FlatButton.primary("Register a business...", e -> addBusiness()),
                muted("New offer for selected business:"), offerText, offerPoints, muted("points"),
                FlatButton.secondary("Add offer", e -> addOffer())), BorderLayout.SOUTH);
        bizTop.setPreferredSize(new java.awt.Dimension(100, 230));
        biz.add(Theme.card("Partner businesses", bizTop), BorderLayout.NORTH);
        JTable vt = Theme.newTable(vouchers);
        vt.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        biz.add(Theme.card("Vouchers given to citizens (QR codes)", Theme.table(vt)), BorderLayout.CENTER);
        tabs.addTab("Businesses & vouchers (FR_25)", biz);

        JPanel ap = new JPanel(new BorderLayout(0, 8));
        ap.setOpaque(false);
        JPanel appTop = new JPanel(new BorderLayout());
        appTop.setOpaque(false);
        appTable.getColumnModel().getColumn(1).setCellRenderer(new Theme.StatusRenderer());
        appTop.add(Theme.table(appTable), BorderLayout.CENTER);
        appTop.add(row(FlatButton.primary("Enable / disable selected app", e -> toggleApp()),
                muted("Ads are shown to citizens based on the waste type they recycle most.")), BorderLayout.SOUTH);
        appTop.setPreferredSize(new java.awt.Dimension(100, 200));
        ap.add(Theme.card("Connected third-party apps", appTop), BorderLayout.NORTH);
        ap.add(Theme.card("Advertisement engagement", Theme.table(Theme.newTable(ads))), BorderLayout.CENTER);
        tabs.addTab("Apps & ads (FR_26)", ap);

        JPanel don = new JPanel(new BorderLayout(0, 8));
        don.setOpaque(false);
        don.add(Theme.card("Donation containers (alert at " + com.greenbin.wms.system.GreenBinSystem.DONATION_THRESHOLD + " items)",
                Theme.table(containerTable)), BorderLayout.CENTER);
        don.add(row(muted("Agency:"), agency,
                FlatButton.primary("Send selected container's items to agency", e -> sendToAgency()),
                FlatButton.secondary("Agency distributed the items", e -> {
                    int n = sys.markDistributed((DonationAgency) agency.getSelectedItem());
                    info(n + " donors received an impact report.");
                    refresh();
                })), BorderLayout.SOUTH);
        tabs.addTab("Donations (FR_27)", don);

        body().add(tabs, BorderLayout.CENTER);
    }

    private void addBusiness() {
        JTextField n = new JTextField(), l = new JTextField(), p = new JTextField(), e = new JTextField();
        Object[] form = {"Name", n, "Location", l, "Phone", p, "Email", e};
        if (javax.swing.JOptionPane.showConfirmDialog(this, form, "Register business", javax.swing.JOptionPane.OK_CANCEL_OPTION) != 0) return;
        if (n.getText().isBlank()) { error("The business needs a name."); return; }
        sys.getBusinesses().add(new Business(n.getText().trim(), l.getText().trim(), p.getText().trim(), e.getText().trim()));
        sys.getLog().add("BUSINESS", n.getText().trim() + " joined the reward program");
        refresh();
    }

    private void addOffer() {
        int r = businessTable.getSelectedRow();
        if (r < 0) { error("Select a business first."); return; }
        if (offerText.getText().isBlank()) { error("Describe the offer."); return; }
        sys.getBusinesses().get(r).addOffer(offerText.getText().trim(), (Integer) offerPoints.getValue());
        offerText.setText("");
        refresh();
    }

    private void toggleApp() {
        int r = appTable.getSelectedRow();
        if (r < 0) { error("Select an app first."); return; }
        App a = sys.getApps().get(r);
        sys.setAppEnabled(a, !a.isEnabled());
        refresh();
    }

    private void sendToAgency() {
        int r = containerTable.getSelectedRow();
        if (r < 0) { error("Select a donation container first."); return; }
        int n = sys.sendToAgency(sys.getDonationContainers().get(r), (DonationAgency) agency.getSelectedItem());
        info(n == 0 ? "There are no items waiting in this container." : n + " items collected. The donors were notified.");
        refresh();
    }

    @Override
    public void refresh() {
        List<Leaderboard.CommunityScore> ranking = sys.getLeaderboard().rankCommunities();
        communities.setRowCount(0);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        int i = 1;
        for (Leaderboard.CommunityScore c : ranking) {
            communities.addRow(new Object[]{i++, c.community, c.members, c.totalPoints, Math.round(c.correctRate) + "%", Math.round(c.score)});
            labels.add(c.community);
            values.add(c.score);
        }
        communityChart.setData(labels, values, java.util.Collections.singletonList(new Color(0x7CB342)), "score");
        users.setRowCount(0);
        i = 1;
        for (User u : sys.getLeaderboard().rankUsers()) {
            users.addRow(new Object[]{i++, u.getDisplayName(), u.getCommunity(), u.getPoints(), String.join(", ", u.getBadges())});
        }

        businesses.setRowCount(0);
        vouchers.setRowCount(0);
        for (Business b : sys.getBusinesses()) {
            List<String> offers = new ArrayList<>();
            for (Business.Offer o : b.getOffers()) offers.add(o.toString());
            businesses.addRow(new Object[]{b.getName(), b.getLocation(), String.join("; ", offers), b.getVouchersRedeemed()});
        }
        for (User u : sys.getUsers()) {
            for (BusinessVoucher v : u.getVouchers()) {
                vouchers.addRow(new Object[]{v.getCode(), u.getDisplayName(), v.getBusiness().getName(), v.getDescription(),
                        v.getPointsCost(), v.getExpirationDate(), v.getStatus()});
            }
        }

        apps.setRowCount(0);
        ads.setRowCount(0);
        for (App a : sys.getApps()) {
            apps.addRow(new Object[]{a.getAppName(), a.isEnabled() ? "ACTIVE" : "DISABLED", a.getAds().size(), a.getTotalImpressions(), a.getTotalClicks()});
            for (App.Advertisement ad : a.getAds()) {
                ads.addRow(new Object[]{a.getAppName(), ad.getTitle(), ad.getTargetType() == null ? "everyone" : ad.getTargetType(),
                        ad.getImpressions(), ad.getClicks(), String.format("%.0f%%", ad.getClickRate())});
            }
        }

        containers.setRowCount(0);
        for (DonationContainer c : sys.getDonationContainers()) {
            int collected = 0, distributed = 0;
            for (Donation d : c.getDonations()) {
                if (d.getStatus() == Donation.Status.COLLECTED_BY_AGENCY) collected++;
                if (d.getStatus() == Donation.Status.DISTRIBUTED) distributed++;
            }
            containers.addRow(new Object[]{c.getLocation(), sys.countWaiting(c), Math.round(c.getFillPercent()) + "%", collected, distributed});
        }
        Object a = agency.getSelectedItem();
        agency.removeAllItems();
        for (DonationAgency d : sys.getAgencies()) agency.addItem(d);
        if (a != null) agency.setSelectedItem(a);
    }
}
