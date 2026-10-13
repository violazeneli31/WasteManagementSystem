package com.greenbin.wms.programs;

import com.greenbin.wms.model.ProgramType;

import java.time.LocalDateTime;

/** FR_16: an awareness program, for example a recycling lesson in a school. */
public class EducationalProgram extends Program {
    private final String targetAudience;

    public EducationalProgram(String name, String description, String location, LocalDateTime date, int capacity, String targetAudience) {
        super(name, description, location, date, capacity);
        this.targetAudience = targetAudience;
    }

    public String getTargetAudience() { return targetAudience; }

    @Override public ProgramType getType() { return ProgramType.EDUCATIONAL_PROGRAM; }

    @Override
    public String generatePromotionalMaterials() {
        return "LEARN WITH GREENBIN: " + getName() + "\n" + getDescription()
                + "\nFor: " + targetAudience + "\nWhen: " + formattedDate() + "\nWhere: " + getLocation()
                + "\nFree entry, " + getCapacity() + " seats.";
    }
}
