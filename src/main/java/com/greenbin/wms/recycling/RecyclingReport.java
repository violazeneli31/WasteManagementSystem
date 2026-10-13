package com.greenbin.wms.recycling;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FR_19: a snapshot of the plant that the operator confirms and the manager reviews. */
public class RecyclingReport {
    private final String reportID = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final String facilityName;
    private final String operatorName;
    private final LocalDate reportDate = LocalDate.now();
    private final Map<String, Double> wasteSummary;
    private final List<String> misclassifiedWasteDetails;
    private final List<String> issuesReported;
    private final double efficiency;
    private final double revenue;
    private boolean processingConfirmed;

    public RecyclingReport(String facilityName, String operatorName, Map<String, Double> wasteSummary,
                           List<String> misclassified, List<String> issues, double efficiency, double revenue) {
        this.facilityName = facilityName;
        this.operatorName = operatorName;
        this.wasteSummary = wasteSummary;
        this.misclassifiedWasteDetails = misclassified;
        this.issuesReported = issues;
        this.efficiency = efficiency;
        this.revenue = revenue;
    }

    public void confirm() { processingConfirmed = true; }

    public String showReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("RECYCLING REPORT ").append(reportID).append(" - ").append(facilityName).append('\n');
        sb.append("Date: ").append(reportDate).append("   Operator: ").append(operatorName).append("\n\n");
        sb.append("Waste by material and stage:\n");
        for (Map.Entry<String, Double> e : wasteSummary.entrySet()) {
            sb.append(String.format("  %-28s %8.1f kg%n", e.getKey(), e.getValue()));
        }
        sb.append(String.format("%nEfficiency: %.1f%%   Revenue: %,.0f Lek%n", efficiency, revenue));
        sb.append("\nMisclassified waste:\n");
        if (misclassifiedWasteDetails.isEmpty()) sb.append("  none\n");
        for (String m : misclassifiedWasteDetails) sb.append("  - ").append(m).append('\n');
        sb.append("\nIssues:\n");
        if (issuesReported.isEmpty()) sb.append("  none\n");
        for (String i : issuesReported) sb.append("  - ").append(i).append('\n');
        sb.append("\nConfirmed by operator: ").append(processingConfirmed ? "YES" : "not yet");
        return sb.toString();
    }

    public String getReportID() { return reportID; }
    public LocalDate getReportDate() { return reportDate; }
    public boolean isProcessingConfirmed() { return processingConfirmed; }
    public double getEfficiency() { return efficiency; }
}
