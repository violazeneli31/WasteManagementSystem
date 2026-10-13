package com.greenbin.wms.programs;

import java.time.LocalDateTime;

/** FACTORY - creates educational programs. "extra" is the target audience. */
public class EducationalProgramFactory extends ProgramFactory {
    @Override
    protected Program makeProgram(String name, String description, String location,
                                  LocalDateTime date, int capacity, String extra) {
        String audience = (extra == null || extra.isBlank()) ? "Everyone" : extra;
        return new EducationalProgram(name, description, location, date, capacity, audience);
    }
}
