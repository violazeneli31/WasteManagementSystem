package com.greenbin.wms.gui.components;

import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.notification.EventReminderNotification;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationDecorator;
import com.greenbin.wms.notification.RewardNotification;
import com.greenbin.wms.notification.UrgentNotification;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** A list of notifications shown as cards. Decorated notifications show which decorators they have. */
public class NotificationList extends JList<INotification> {
    private final DefaultListModel<INotification> model = new DefaultListModel<>();

    public NotificationList() {
        setModel(model);
        setCellRenderer(new Renderer());
        setBackground(Theme.BG);
    }

    public void setNotifications(List<INotification> list) {
        model.clear();
        for (INotification n : list) model.addElement(n);
    }

    /** Names of the decorators wrapped around a notification, outermost first. */
    public static String decoratorsOf(INotification n) {
        StringBuilder sb = new StringBuilder();
        INotification current = n;
        while (current instanceof NotificationDecorator) {
            if (current instanceof RewardNotification) sb.append("+Reward ");
            else if (current instanceof EventReminderNotification) sb.append("+EventReminder ");
            else if (current instanceof UrgentNotification) sb.append("+Urgent ");
            current = ((NotificationDecorator) current).getWrapped();
        }
        return sb.toString().trim();
    }

    private static class Renderer implements ListCellRenderer<INotification> {
        @Override
        public Component getListCellRendererComponent(JList<? extends INotification> list, INotification n, int index,
                                                      boolean selected, boolean focus) {
            JPanel card = new JPanel(new BorderLayout(4, 2));
            boolean urgent = n.getSubject().startsWith("[URGENT]");
            card.setBackground(selected ? new Color(0xE8F5E9) : Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 6, 0, Theme.BG),
                    BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, urgent ? Theme.RED : Theme.GREEN),
                            BorderFactory.createEmptyBorder(8, 10, 8, 10))));
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(Theme.label(n.getSubject(), Theme.BOLD, urgent ? Theme.RED : Theme.TEXT), BorderLayout.WEST);
            JPanel tags = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            tags.setOpaque(false);
            String deco = decoratorsOf(n);
            if (!deco.isEmpty()) tags.add(Theme.label(deco, Theme.SMALL, Theme.PURPLE));
            tags.add(Theme.label(n.getCategory().getLabel() + "  " + n.getDate().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                    Theme.SMALL, Theme.MUTED));
            top.add(tags, BorderLayout.EAST);
            JTextArea text = new JTextArea(n.getDescription());
            text.setFont(Theme.NORMAL);
            text.setForeground(Theme.TEXT);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setOpaque(false);
            text.setBorder(null);
            text.setSize(Math.max(200, list.getWidth() - 40), Short.MAX_VALUE);
            card.add(top, BorderLayout.NORTH);
            card.add(text, BorderLayout.CENTER);
            return card;
        }
    }
}
