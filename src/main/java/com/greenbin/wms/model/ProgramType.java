package com.greenbin.wms.model;

public enum ProgramType {
    EVENT("Community event"),
    EDUCATIONAL_PROGRAM("Educational program");

    private final String label;
    ProgramType(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
