package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.recognition.ClassificationResult;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;

/** FR_22: upload a photo of an item and the image recognition says which bin it goes in. */
public class ClassifyScreen extends Screen {
    private BufferedImage image;
    private String fileName = "";
    private final Preview preview = new Preview();
    private final JLabel answer = Theme.label("Upload a photo or pick an example", Theme.H1, Theme.TEXT);
    private final JLabel details = Theme.label(" ", Theme.NORMAL, Theme.TEXT);
    private final JProgressBar confidence = new JProgressBar(0, 100);
    private final BinBadge badge = new BinBadge();

    public ClassifyScreen(Account me) {
        super(me, "Which Bin?", "Not sure where something goes? Show it to the camera.", "FR_22");
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(preview, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 3, 6, 6));
        buttons.setOpaque(false);
        buttons.add(FlatButton.primary("Upload photo...", e -> upload()));
        String[][] samples = {{"plastic_bottle.jpg", "Plastic bottle"}, {"banana_peel.jpg", "Banana peel"}, {"old_battery.jpg", "Battery"},
                {"newspaper.jpg", "Newspaper"}, {"glass_jar.jpg", "Glass jar"}};
        for (String[] s : samples) buttons.add(FlatButton.secondary("Example: " + s[1], e -> { image = drawSample(s[0]); fileName = s[0]; analyze(); }));
        left.add(buttons, BorderLayout.SOUTH);
        body().add(Theme.card("Photo", left), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(420, 100));
        JPanel res = new Stack();
        res.setOpaque(false);
        res.add(answer);
        confidence.setStringPainted(true);
        res.add(confidence);
        res.add(details);
        right.add(Theme.card("Result", res), BorderLayout.NORTH);
        right.add(Theme.card("Put it in this bin", badge), BorderLayout.CENTER);
        body().add(right, BorderLayout.EAST);
    }

    private void upload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            image = ImageIO.read(f);
            if (image == null) throw new IllegalArgumentException();
            fileName = f.getName();
            analyze();
        } catch (Exception ex) {
            error("This file is not an image the app can read.");
        }
    }

    private void analyze() {
        preview.repaint();
        ClassificationResult r = sys.getRecognition().analyzeImage(fileName, image);
        confidence.setValue((int) Math.round(r.getConfidence() * 100));
        confidence.setString("confidence " + confidence.getValue() + "%");
        badge.result = r;
        badge.repaint();
        if (!r.isRecognized()) {
            answer.setText("Not recognised");
            answer.setForeground(Theme.MUTED);
            details.setText("<html>" + r.getAdvice() + "</html>");
            return;
        }
        answer.setText(r.getType().getLabel() + " bin");
        answer.setForeground(Theme.colorOf(r.getType()).darker());
        details.setText("<html>Recyclable: <b>" + (r.isRecyclable() ? "yes" : "no") + "</b> &nbsp; Hazardous: <b>"
                + (r.isHazardous() ? "<font color=red>YES</font>" : "no") + "</b><br><br>" + r.getAdvice() + "</html>");
    }

    @Override
    public void refresh() { }

    /** Simple drawn example photos, so the screen works without real pictures. */
    private static BufferedImage drawSample(String name) {
        BufferedImage img = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Theme.smooth(g);
        g.setColor(new Color(0xF5F5F5));
        g.fillRect(0, 0, 320, 240);
        if (name.startsWith("plastic")) {
            g.setColor(new Color(0x64B5F6));
            g.fillRoundRect(130, 60, 60, 150, 30, 30);
            g.fillRect(148, 30, 24, 35);
            g.setColor(new Color(0x1565C0));
            g.fillRect(146, 22, 28, 12);
        } else if (name.startsWith("banana")) {
            g.setColor(new Color(0xFDD835));
            g.setStroke(new BasicStroke(26, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(70, 20, 180, 170, 200, 140);
            g.setColor(new Color(0x6D4C41));
            g.fillOval(76, 120, 14, 14);
        } else if (name.contains("battery")) {
            g.setColor(new Color(0x212121));
            g.fillRoundRect(110, 70, 100, 140, 14, 14);
            g.setColor(new Color(0xE53935));
            g.fillRoundRect(110, 70, 100, 50, 14, 14);
            g.setColor(new Color(0x9E9E9E));
            g.fillRect(145, 55, 30, 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Theme.FONT, Font.BOLD, 26));
            g.drawString("+", 151, 104);
        } else if (name.startsWith("newspaper")) {
            g.setColor(new Color(0xEEEEEE));
            g.fillRect(60, 40, 200, 160);
            g.setColor(new Color(0x424242));
            g.setFont(new Font("Serif", Font.BOLD, 20));
            g.drawString("DAILY NEWS", 95, 70);
            for (int y = 85; y < 190; y += 12) g.fillRect(75, y, 170, 4);
        } else {
            g.setColor(new Color(0xB2DFDB));
            g.fillRoundRect(110, 70, 100, 130, 24, 24);
            g.setColor(new Color(0x9E9E9E));
            g.fillRoundRect(105, 52, 110, 24, 8, 8);
        }
        g.dispose();
        return img;
    }

    private class Preview extends JComponent {
        Preview() { setPreferredSize(new Dimension(420, 320)); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            g.setColor(new Color(0xFAFAFA));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (image == null) {
                g.setColor(Theme.MUTED);
                g.drawString("No photo yet", getWidth() / 2 - 35, getHeight() / 2);
            } else {
                double s = Math.min((getWidth() - 20.0) / image.getWidth(), (getHeight() - 40.0) / image.getHeight());
                int w = (int) (image.getWidth() * s), h = (int) (image.getHeight() * s);
                g.drawImage(image, (getWidth() - w) / 2, 10, w, h, null);
                g.setColor(Theme.MUTED);
                g.drawString(fileName, (getWidth() - g.getFontMetrics().stringWidth(fileName)) / 2, getHeight() - 10);
            }
            g.dispose();
        }
    }

    /** A big bin in the colour of the answer. */
    private static class BinBadge extends JComponent {
        ClassificationResult result;

        @Override
        protected void paintComponent(Graphics g0) {
            if (result == null || !result.isRecognized()) return;
            Graphics2D g = (Graphics2D) g0.create();
            Theme.smooth(g);
            Color c = Theme.colorOf(result.getType());
            int cx = getWidth() / 2, top = 30;
            g.setColor(c.darker());
            g.fillRoundRect(cx - 80, top, 160, 20, 10, 10);
            g.setColor(c);
            g.fillPolygon(new int[]{cx - 70, cx + 70, cx + 58, cx - 58}, new int[]{top + 26, top + 26, top + 200, top + 200}, 4);
            g.setColor(Color.WHITE);
            g.setFont(Theme.H1);
            String t = result.getType().getLabel();
            g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, top + 120);
            g.dispose();
        }
    }
}
