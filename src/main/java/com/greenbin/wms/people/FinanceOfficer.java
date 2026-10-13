package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;

/** Manages prices, bills, expenses, fraud cases, disputes, tax reports and the budget. */
public class FinanceOfficer extends Employee {

    public FinanceOfficer(String name, String surname, String username, String password,
                          String phone, String email, double wage) {
        super(name, surname, username, password, phone, email, wage, Role.FINANCE_OFFICER);
    }
}
