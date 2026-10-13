package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.LineChart;
import com.greenbin.wms.gui.components.StatCard;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.planning.Statistics;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** FR_02 statistics for the schedules and FR_30 prediction of future waste per area. */
public class StatisticsScreen extends Screen {
    private final JComboBox<String> area = new JComboBox<>();
    private final StatCard mean = new StatCard("mean kg / day", Theme.GREEN);
    private final StatCard median = new StatCard("median kg / day", Theme.GREEN);
    private final StatCard mode = new StatCard("mode (rounded)", Theme.GREEN);
    private final StatCard std = new StatCard("standard deviation", Theme.BLUE);
    private final StatCard pickups = new StatCard("optimal pickups / week", Theme.ORANGE);
    private final StatCard trend = new StatCard("trend (kg per day)", Theme.PURPLE);
    private final LineChart history = new LineChart();
    private final BarChart areas = new BarChart();
    private final DefaultTableModel table = Tables.model("Area", "Mean", "Median", "Std dev", "Pickups / week", "Trend", "Next week (predicted)");
    private final JSpinner newValue = new JSpinner(new SpinnerNumberModel(500, 0, 5000, 10));

    public StatisticsScreen(Account me) {
        super(me, "Statistics & Forecast", "Waste collected per area: the numbers behind the schedules and the prediction for next week",
                "FR_02", "FR_30");
        area.addActionListener(e -> showArea());
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(row(muted("Area:"), area, muted("   add today's amount (manual input):"), newValue,
                FlatButton.secondary("Add data", e -> addData())), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 6, 8, 0));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{mean, median, mode, std, pickups, trend}) cards.add(c);
        top.add(cards, BorderLayout.CENTER);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        charts.setOpaque(false);
        charts.add(Theme.card("Last 14 days + 7-day prediction (FR_30)", history));
        charts.add(Theme.card("Average per day now vs predicted next week, all areas", areas));

        JTable t = Theme.newTable(table);
        JPanel tableCard = Theme.card("Optimal number of pickups per area (FR_02): weekly kg / truck capacity "
                + (int) Statistics.TRUCK_CAPACITY_KG + " kg", Theme.table(t));
        tableCard.setPreferredSize(new Dimension(100, 210));

        body().add(top, BorderLayout.NORTH);
        body().add(charts, BorderLayout.CENTER);
        body().add(tableCard, BorderLayout.SOUTH);
    }

    private void addData() {
        String a = (String) area.getSelectedItem();
        if (a == null) return;
        sys.getStatistics().addData(a, ((Number) newValue.getValue()).doubleValue());
        sys.getLog().add("STATISTICS", "Manual data for " + a + ": " + newValue.getValue() + " kg");
        refresh();
        area.setSelectedItem(a);
    }

    @Override
    public void refresh() {
        Statistics st = sys.getStatistics();
        Object selected = area.getSelectedItem();
        area.removeAllItems();
        for (String a : st.getAreas()) area.addItem(a);
        if (selected != null) area.setSelectedItem(selected);

        table.setRowCount(0);
        List<String> labels = new ArrayList<>();
        List<Double> now = new ArrayList<>();
        List<Double> next = new ArrayList<>();
        for (String a : st.getAreas()) {
            double predicted = st.predictNextWeekTotal(a);
            table.addRow(new Object[]{a, Tables.kg(st.calculateMean(a)), Tables.kg(st.calculateMedian(a)),
                    Tables.kg(st.calculateStandardDeviation(a)), st.calculateOptimalPickups(a),
                    String.format("%+.1f kg/day", st.getTrend(a)), Tables.kg(predicted)});
            labels.add(a);
            now.add(st.calculateMean(a));
            next.add(predicted / 7);
        }
        areas.setData(labels, now, Collections.singletonList(Theme.GREEN), "kg / day");
        areas.setSecondSeries(next, "now", "predicted");
        showArea();
    }

    private void showArea() {
        String a = (String) area.getSelectedItem();
        if (a == null) return;
        Statistics st = sys.getStatistics();
        mean.setValue(String.format("%.0f", st.calculateMean(a)));
        median.setValue(String.format("%.0f", st.calculateMedian(a)));
        mode.setValue(String.format("%.0f", st.calculateMode(a)));
        std.setValue(String.format("%.1f", st.calculateStandardDeviation(a)));
        pickups.setValue(String.valueOf(st.calculateOptimalPickups(a)));
        trend.setValue(String.format("%+.1f", st.getTrend(a)));

        List<Double> data = new ArrayList<>(st.getData(a));
        int past = data.size();
        List<Double> predicted = st.predictNextDays(a, 7);
        List<Double> line = new ArrayList<>(data);
        line.addAll(predicted);
        List<String> x = new ArrayList<>();
        for (int i = 0; i < line.size(); i++) x.add(i < past ? "-" + (past - i) + "d" : "+" + (i - past + 1) + "d");
        List<LineChart.Series> series = new ArrayList<>();
        series.add(new LineChart.Series(a + " (kg)", Theme.GREEN, line));
        List<Double> avg = new ArrayList<>();
        for (int i = 0; i < line.size(); i++) avg.add(st.calculateMean(a));
        series.add(new LineChart.Series("mean", new Color(0x90A4AE), avg));
        history.setData(x, series, past, "kg / day");
    }
}
