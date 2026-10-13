package com.greenbin.wms;

import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.Expense;
import com.greenbin.wms.finance.FinancialPlan;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.finance.TaxReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_32 expenses, FR_33 tax report, FR_35 forecasting. */
class FinanceForecastTest {
    private final TestData d = new TestData();

    @Test
    void expensesAreTotalledAndRisingCostsAreFound() {
        LocalDate today = LocalDate.now();
        d.sys.addExpense(Expense.Category.VEHICLE_MAINTENANCE, "repair", 100, today.minusMonths(1));
        d.sys.addExpense(Expense.Category.VEHICLE_MAINTENANCE, "repair", 150, today);
        d.sys.addExpense(Expense.Category.SALARIES, "pay", 1000, today);
        assertEquals(250, d.sys.getExpenses().totalByCategory().get(Expense.Category.VEHICLE_MAINTENANCE), 0.001);
        List<String> tips = d.sys.getExpenses().findSavingTips(today);
        assertTrue(tips.get(0).contains("50%"));
        assertThrows(IllegalArgumentException.class, () -> new Expense(Expense.Category.SALARIES, "x", -1, today));
    }

    @Test
    void taxReportUsesPaidBillsAndExpensesAndFollowsTheSteps() {
        Bill b = new Bill(d.ana, "x", 1000, LocalDate.now(), LocalDate.now().plusDays(30));
        b.approve();
        b.payBill(PaymentMethod.CASH);
        d.sys.getBills().add(b);
        d.sys.addExpense(Expense.Category.SALARIES, "pay", 300, LocalDate.now());
        d.sys.addExpense(Expense.Category.RECYCLING_INCENTIVES, "rewards", 100, LocalDate.now());
        TaxReport r = d.sys.generateTaxReport(LocalDate.now().getYear());
        assertEquals(1000, r.getRevenue(), 0.001);
        assertEquals(200, r.getVatCollected(), 0.001);
        assertEquals(600, r.getProfit(), 0.001);
        assertEquals(90, r.getProfitTax(), 0.001);
        assertFalse(r.submit());              // must be verified first
        assertTrue(r.verify());
        assertTrue(r.submit());
        assertTrue(r.approveByAuditor());
        assertEquals(TaxReport.Status.APPROVED_BY_AUDITOR, r.getStatus());
    }

    @Test
    void forecastFollowsTheAverageChangeAndFindsFundingNeeds() {
        FinancialPlan plan = new FinancialPlan(2026, LocalDate.now(), 1000);
        plan.addMonth(100, 100);
        plan.addMonth(110, 130);
        plan.addMonth(120, 160);
        assertEquals(List.of(130.0, 140.0), plan.forecastRevenue(2));
        assertEquals(List.of(190.0, 220.0), plan.forecastExpenses(2));
        assertTrue(plan.getFundingNeeded() > 0);
        assertFalse(plan.isBudgetEnough());
        plan.updateBudget(10_000, "Mira");
        assertTrue(plan.isBudgetEnough());
    }
}
