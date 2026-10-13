package com.greenbin.wms.programs;

import com.greenbin.wms.model.ProgramType;

import java.time.LocalDateTime;

/**
 * FACTORY METHOD PATTERN - the creator.
 * createProgram() checks the common data once; each subclass only decides
 * WHICH kind of program to build in makeProgram(). New program types can be added
 * with a new factory, without changing this code.
 */
public abstract class ProgramFactory {

    public Program createProgram(String name, String description, String location,
                                 LocalDateTime date, int capacity, String extra) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("The program needs a name");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("The program needs a location");
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be more than 0");
        if (date.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("The date must be in the future");
        String desc = (description == null || description.isBlank()) ? "More details soon." : description.trim();
        return makeProgram(name.trim(), desc, location.trim(), date, capacity, extra);
    }

    /** The factory method. */
    protected abstract Program makeProgram(String name, String description, String location,
                                           LocalDateTime date, int capacity, String extra);

    /** Gives the right factory for a program type. */
    public static ProgramFactory forType(ProgramType type) {
        return type == ProgramType.EVENT ? new EventFactory() : new EducationalProgramFactory();
    }
}
