package com.greenbin.wms.programs;

import java.time.LocalDateTime;

/** FACTORY - creates community events. "extra" is the objective of the event. */
public class EventFactory extends ProgramFactory {
    @Override
    protected Program makeProgram(String name, String description, String location,
                                  LocalDateTime date, int capacity, String extra) {
        String objective = (extra == null || extra.isBlank()) ? "Keep our city clean" : extra;
        return new CommunityEvent(name, description, location, date, capacity, objective);
    }
}
