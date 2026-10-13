package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.NotificationList;
import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.notification.BaseNotification;
import com.greenbin.wms.notification.EventReminderNotification;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationCategory;
import com.greenbin.wms.notification.NotificationCenter;
import com.greenbin.wms.notification.RewardNotification;
import com.greenbin.wms.notification.UrgentNotification;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.programs.Program;
import com.greenbin.wms.programs.ProgramFactory;
import com.greenbin.wms.routing.RouteOptimizationCenter;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shows the design patterns of the project working live, with a button to try each one. */
public class PatternsScreen extends Screen {
    private final JCheckBox reward = new JCheckBox("+ Reward");
    private final JCheckBox reminder = new JCheckBox("+ Event reminder");
    private final JCheckBox urgent = new JCheckBox("+ Urgent");

    public PatternsScreen(Account me) {
        super(me, "Design Patterns", "The patterns from the project document, as they are used in the code. Press a button to see one work.",
                "Singleton", "Observer", "Factory", "Decorator");
        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);

        JTextArea singletonOut = output();
        grid.add(pattern("Singleton (Noel)", "Only ONE route optimizer exists, so two parts of the system can never plan different routes for the same truck."
                        + "\nCode: RouteOptimizationCenter.getInstance() (private constructor). GreenBinSystem.getInstance() is a singleton too.",
                singletonOut, FlatButton.primary("Ask for the optimizer twice", e -> {
                    RouteOptimizationCenter a = RouteOptimizationCenter.getInstance();
                    RouteOptimizationCenter b = RouteOptimizationCenter.getInstance();
                    singletonOut.setText("First call:  object #" + System.identityHashCode(a) + "\nSecond call: object #" + System.identityHashCode(b)
                            + "\nSame object? " + (a == b) + "\nRoutes it calculated so far: " + a.getRoutesCalculated());
                })));

        JTextArea observerOut = output();
        grid.add(pattern("Observer (Nikol)", "Users and employees are observers; the NotificationCenter is the subject. When something changes, "
                        + "it notifies them - but only if they did not turn that category off (FR_21).\nCode: NotificationCenter, NotificationObserver, User, Employee.",
                observerOut, FlatButton.primary("Send a test to everyone", e -> {
                    NotificationCenter c = sys.getNotificationCenter();
                    int before = c.getDeliveryLog().size();
                    int sent = c.broadcast(BaseNotification.forUser("Test from the Design Patterns screen", "Observer pattern demo.", NotificationCategory.REWARDS));
                    List<String> lines = new ArrayList<>(c.getDeliveryLog().subList(before, c.getDeliveryLog().size()));
                    observerOut.setText(c.getObservers().size() + " observers subscribed, " + sent + " received it:\n" + String.join("\n", lines));
                    observerOut.setCaretPosition(0);
                })));

        JTextArea factoryOut = output();
        grid.add(pattern("Factory Method (Sidrit)", "Events and educational programs share name, date, place, capacity. The factory checks them once "
                        + "and the subclass decides which object to make.\nCode: ProgramFactory, EventFactory, EducationalProgramFactory.",
                factoryOut, FlatButton.primary("Build one of each (not saved)", e -> {
                    StringBuilder sb = new StringBuilder();
                    for (ProgramType t : ProgramType.values()) {
                        ProgramFactory f = ProgramFactory.forType(t);
                        Program p = f.createProgram("Demo " + t, "", "Tirana", LocalDateTime.now().plusDays(3), 20, "");
                        sb.append(f.getClass().getSimpleName()).append(" -> ").append(p.getClass().getSimpleName())
                                .append(" (description default: '").append(p.getDescription()).append("')\n");
                    }
                    try {
                        ProgramFactory.forType(ProgramType.EVENT).createProgram("", "", "", LocalDateTime.now().plusDays(1), 0, "");
                    } catch (IllegalArgumentException ex) {
                        sb.append("\nInvalid data is refused: ").append(ex.getMessage());
                    }
                    factoryOut.setText(sb.toString());
                })));

        JTextArea decoOut = output();
        JPanel decoButtons = new Stack();
        decoButtons.setOpaque(false);
        for (JCheckBox cb : new JCheckBox[]{reward, reminder, urgent}) { cb.setOpaque(false); decoButtons.add(cb); }
        decoButtons.add(FlatButton.primary("Build the notification", e -> {
            INotification n = BaseNotification.forUser("Collection tomorrow", "Your street is collected tomorrow at 06:00.", NotificationCategory.SCHEDULE);
            String chain = "BaseNotification";
            if (reward.isSelected()) { n = new RewardNotification(n, 12, 192); chain = "Reward(" + chain + ")"; }
            if (reminder.isSelected() && !sys.getPrograms().isEmpty()) { n = new EventReminderNotification(n, sys.getPrograms().get(0)); chain = "EventReminder(" + chain + ")"; }
            if (urgent.isSelected()) { n = new UrgentNotification(n); chain = "Urgent(" + chain + ")"; }
            decoOut.setText("Wrapped as: " + chain + "\n\n" + n.showNotification());
        }));
        grid.add(pattern("Decorator (Viola)", "A notification can be extended with rewards, event reminders or an urgent mark without changing "
                + "the base class. Decorators wrap each other.\nCode: INotification, BaseNotification, NotificationDecorator + 3 decorators.", decoOut, decoButtons));

        NotificationList mine = new NotificationList();
        mine.setNotifications(me.getNotifications().subList(0, Math.min(5, me.getNotifications().size())));
        JScrollPane sp = new JScrollPane(mine);
        sp.setBorder(null);
        grid.add(Theme.card("Observer + Decorator in your inbox", sp));

        body().add(grid, BorderLayout.CENTER);
    }

    private static JTextArea output() {
        JTextArea a = new JTextArea(6, 20);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.MONO);
        a.setBackground(new java.awt.Color(0xF5F7F6));
        return a;
    }

    private JPanel pattern(String title, String text, JTextArea out, JComponent action) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JTextArea t = new JTextArea(text);
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setFont(Theme.NORMAL);
        p.add(t, BorderLayout.NORTH);
        p.add(new JScrollPane(out), BorderLayout.CENTER);
        p.add(action, BorderLayout.SOUTH);
        return Theme.card(title, p);
    }

    @Override
    public void refresh() { }
}
