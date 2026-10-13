package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;

/**
 * The company manager. Most of the work the manager does (maps, schedules, programs...)
 * is done through the screens, which call the objects that own the data.
 * "present" tells the system if the manager is available to decide on changes (FR_03).
 */
public class CompanyManager extends Employee {
    private boolean present = true;

    public CompanyManager(String name, String surname, String username, String password,
                          String phone, String email, double wage) {
        super(name, surname, username, password, phone, email, wage, Role.MANAGER);
    }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
