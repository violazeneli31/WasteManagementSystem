package com.greenbin.wms.programs;

import com.greenbin.wms.model.ProgramType;

import java.time.LocalDateTime;

/** FR_15: a competitive or community event such as an Earth Day clean-up. */
public class CommunityEvent extends Program {
    private final String objective;

    public CommunityEvent(String name, String description, String location, LocalDateTime date, int capacity, String objective) {
        super(name, description, location, date, capacity);
        this.objective = objective;
    }

    public String getObjective() { return objective; }

    @Override public ProgramType getType() { return ProgramType.EVENT; }

    @Override
    public String generatePromotionalMaterials() {
        return "JOIN US: " + getName().toUpperCase() + "\n" + getDescription()
                + "\nGoal: " + objective + "\nWhen: " + formattedDate() + "\nWhere: " + getLocation()
                + "\nOnly " + getCapacity() + " places - sign up in the GreenBin app and earn points!";
    }
}
