package com.greenbin.wms.gui.screens;

import com.greenbin.wms.community.App;
import com.greenbin.wms.community.Business;
import com.greenbin.wms.community.BusinessVoucher;
import com.greenbin.wms.community.Leaderboard;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.CodeViews;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** FR_24 leaderboard, FR_25 exchange points for vouchers, FR_26 ads from connected apps. */
public class RewardsScreen extends Screen {
    private final User user;
    private final JLabel points = Theme.label("", Theme.H1.deriveFont(34f), Theme.GREEN);
    private final BarChart communities = new BarChart();
    private final DefaultTableModel ranking = Tables.model("#", "Name", "Community", "Points");
    private final DefaultTableModel offers = Tables.model("Business", "Offer", "Points");
    private final JTable offerTable = Theme.newTable(offers);
    private final List<Object[]> offerRows = new ArrayList<>();
    private final DefaultTableModel myVouchers = Tables.model("Offer", "Business", "Code", "Status");
    private final JTable voucherTable = Theme.newTable(myVouchers);
    private final CodeViews.QrCodeView qr = new CodeViews.QrCodeView(null);
    private final JLabel qrText = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JPanel ads = new JPanel();

    public RewardsScreen(Account me) {
        super(me, "Rewards & Leaderboard", "Recycle correctly, climb the ranking, and spend your points at partner shops",
                "FR_24", "FR_25", "FR_26");
        user = (User) me;

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(420, 100));
        left.add(Theme.card("My points", points), BorderLayout.NORTH);
        communities.setPreferredSize(new Dimension(380, 200));
        left.add(Theme.card("Communities (top one gets a badge)", communities), BorderLayout.CENTER);
        JScrollPane rk = Theme.table(Theme.newTable(ranking));
        rk.setPreferredSize(new Dimension(380, 200));
        left.add(Theme.card("Top recyclers", rk), BorderLayout.SOUTH);
        body().add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        JPanel o = new JPanel(new BorderLayout(0, 6));
        o.setOpaque(false);
        o.add(Theme.table(offerTable), BorderLayout.CENTER);
        o.add(row(FlatButton.primary("Get voucher for selected offer", e -> getVoucher())), BorderLayout.SOUTH);
        center.add(Theme.card("Partner offers", o));
        JPanel v = new JPanel(new BorderLayout(8, 6));
        v.setOpaque(false);
        voucherTable.getColumnModel().getColumn(3).setCellRenderer(new Theme.StatusRenderer());
        voucherTable.getSelectionModel().addListSelectionListener(e -> showQr());
        v.add(Theme.table(voucherTable), BorderLayout.CENTER);
        JPanel qrBox = new JPanel(new BorderLayout());
        qrBox.setOpaque(false);
        qr.setPreferredSize(new Dimension(150, 150));
        qrBox.add(qr, BorderLayout.CENTER);
        qrBox.add(qrText, BorderLayout.SOUTH);
        qrBox.add(FlatButton.secondary("Use at the shop", e -> useVoucher()), BorderLayout.NORTH);
        v.add(qrBox, BorderLayout.EAST);
        center.add(Theme.card("My vouchers - show the QR code at the shop", v));
        body().add(center, BorderLayout.CENTER);

        ads.setLayout(new BoxLayout(ads, BoxLayout.Y_AXIS));
        ads.setOpaque(false);
        JScrollPane adScroll = new JScrollPane(ads);
        adScroll.setBorder(null);
        adScroll.getViewport().setOpaque(false);
        adScroll.setOpaque(false);
        JPanel adCard = Theme.card("For you (partner apps)", adScroll);
        adCard.setPreferredSize(new Dimension(250, 100));
        body().add(adCard, BorderLayout.EAST);
    }

    private void getVoucher() {
        int r = offerTable.getSelectedRow();
        if (r < 0) { error("Select an offer first."); return; }
        Business b = (Business) offerRows.get(r)[0];
        Business.Offer offer = (Business.Offer) offerRows.get(r)[1];
        BusinessVoucher v = sys.redeemPoints(user, b, offer);
        if (v == null) error("You need " + offer.getPointsCost() + " points (you have " + user.getPoints() + "). Keep recycling!");
        else info("Your voucher: " + v.getDescription() + " at " + b.getName() + "\nCode: " + v.getCode());
        refresh();
    }

    private BusinessVoucher selectedVoucher() {
        int r = voucherTable.getSelectedRow();
        return r < 0 ? null : user.getVouchers().get(r);
    }

    private void showQr() {
        BusinessVoucher v = selectedVoucher();
        qr.setCode(v == null ? null : v.getCode());
        qrText.setText(v == null ? " " : v.getCode() + " - valid until " + v.getExpirationDate());
    }

    private void useVoucher() {
        BusinessVoucher v = selectedVoucher();
        if (v == null) { error("Select a voucher first."); return; }
        if (v.redeem()) info("Voucher used at " + v.getBusiness().getName() + ". Enjoy!");
        else error("This voucher can't be used (" + v.getStatus() + ").");
        refresh();
    }

    @Override
    public void refresh() {
        points.setText(user.getPoints() + " points");
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (Leaderboard.CommunityScore c : sys.getLeaderboard().rankCommunities()) {
            labels.add(c.community);
            values.add(c.score);
            colors.add(c.community.equals(user.getCommunity()) ? Theme.ORANGE : new Color(0x7CB342));
        }
        communities.setData(labels, values, colors, "score (yours in orange)");
        ranking.setRowCount(0);
        int i = 1;
        for (User u : sys.getLeaderboard().rankUsers()) ranking.addRow(new Object[]{i++, u == user ? u.getDisplayName() + " (you)" : u.getDisplayName(), u.getCommunity(), u.getPoints()});

        offers.setRowCount(0);
        offerRows.clear();
        for (Business b : sys.getBusinesses()) {
            for (Business.Offer o : b.getOffers()) {
                offerRows.add(new Object[]{b, o});
                offers.addRow(new Object[]{b.getName(), o.getDescription(), o.getPointsCost()});
            }
        }
        myVouchers.setRowCount(0);
        for (BusinessVoucher v : user.getVouchers()) myVouchers.addRow(new Object[]{v.getDescription(), v.getBusiness().getName(), v.getCode(), v.getStatus()});
        showQr();

        ads.removeAll();
        for (App.Advertisement ad : sys.getAdsFor(user)) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(0xFFFDE7));
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xFFE082)), Theme.padding(8)));
            card.add(Theme.label(ad.getTitle(), Theme.BOLD, Theme.TEXT), BorderLayout.NORTH);
            card.add(Theme.label("<html>" + ad.getText() + "</html>", Theme.SMALL, Theme.MUTED), BorderLayout.CENTER);
            card.add(new FlatButton("Open", Theme.ORANGE, e -> { ad.click(); info(ad.getTitle() + "\n\n" + ad.getText()); }), BorderLayout.SOUTH);
            card.setMaximumSize(new Dimension(240, 110));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            ads.add(card);
            ads.add(javax.swing.Box.createVerticalStrut(8));
        }
        ads.revalidate();
        ads.repaint();
    }
}
