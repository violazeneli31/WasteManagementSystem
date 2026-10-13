package com.greenbin.wms.model;

/** Used for role-based access: each role sees only its own screens (NFR_13). */
public enum Role {
    USER("Citizen / Institution"),
    MANAGER("Company Manager"),
    GARBAGE_COLLECTOR("Garbage Collection Employee"),
    FINANCE_OFFICER("Finance Officer"),
    RECYCLING_OPERATOR("Recycling Facility Operator");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
