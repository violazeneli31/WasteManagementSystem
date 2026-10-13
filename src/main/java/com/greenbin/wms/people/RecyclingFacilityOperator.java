package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;

/** Works at the recycling plant: checks flagged waste, sorts it and tracks the recycling stages. */
public class RecyclingFacilityOperator extends Employee {
    private final String assignedFacility;

    public RecyclingFacilityOperator(String name, String surname, String username, String password,
                                     String phone, String email, double wage, String assignedFacility) {
        super(name, surname, username, password, phone, email, wage, Role.RECYCLING_OPERATOR);
        this.assignedFacility = assignedFacility;
    }

    public String getAssignedFacility() { return assignedFacility; }
}
