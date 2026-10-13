package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;

/** Drives the trucks, empties bins, places new bins and answers pickup requests. */
public class GarbageCollectionEmployee extends Employee {
    private final String shiftTime;

    public GarbageCollectionEmployee(String name, String surname, String username, String password,
                                     String phone, String email, double wage, String shiftTime) {
        super(name, surname, username, password, phone, email, wage, Role.GARBAGE_COLLECTOR);
        this.shiftTime = shiftTime;
    }

    public String getShiftTime() { return shiftTime; }
}
