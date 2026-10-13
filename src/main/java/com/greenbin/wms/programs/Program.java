package com.greenbin.wms.programs;

import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.people.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * FACTORY PATTERN - the product. Events (FR_15) and educational programs (FR_16)
 * share everything here; the factories decide which one is created.
 */
public abstract class Program {
    private final UUID id = UUID.randomUUID();
    private final String name;
    private final String description;
    private final String location;
    private final LocalDateTime date;
    private final int capacity;
    private final Set<User> participants = new LinkedHashSet<>();
    private final Set<User> attended = new LinkedHashSet<>();
    private final List<String> feedbacks = new ArrayList<>();
    private final List<Integer> ratings = new ArrayList<>();
    private String insights = "";

    protected Program(String name, String description, String location, LocalDateTime date, int capacity) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.date = date;
        this.capacity = capacity;
    }

    public abstract ProgramType getType();

    /** Text for posters / social media (each type writes its own). */
    public abstract String generatePromotionalMaterials();

    public boolean addParticipant(User user) {
        if (isFull() || isProgramOver()) return false;
        return participants.add(user);
    }

    public boolean removeParticipant(User user) {
        attended.remove(user);
        return participants.remove(user);
    }

    /** Attendance is only recorded for people who signed up (FR_15 step 4, FR_16 step 3). */
    public boolean markAttendance(User user) {
        return participants.contains(user) && attended.add(user);
    }

    /** FR_16 step 4: feedback with a rating from 1 to 5. */
    public void addFeedback(User user, int rating, String text) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");
        ratings.add(rating);
        feedbacks.add(user.getDisplayName() + " (" + rating + "/5): " + text);
    }

    public double getAverageRating() {
        if (ratings.isEmpty()) return 0;
        double sum = 0;
        for (int r : ratings) sum += r;
        return sum / ratings.size();
    }

    /** Percentage of registered people who actually came. */
    public double getAttendanceRate() {
        return participants.isEmpty() ? 0 : 100.0 * attended.size() / participants.size();
    }

    public boolean isFull() { return participants.size() >= capacity; }
    public boolean isProgramUpcoming() { return date.isAfter(LocalDateTime.now()); }
    public boolean isProgramOver() { return date.isBefore(LocalDateTime.now()); }

    protected String formattedDate() { return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM, HH:mm")); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDateTime getDate() { return date; }
    public int getCapacity() { return capacity; }
    public Set<User> getParticipants() { return participants; }
    public int getNrOfParticipants() { return participants.size(); }
    public Set<User> getAttended() { return attended; }
    public List<String> getFeedbacks() { return feedbacks; }
    public String getInsights() { return insights; }
    public void setInsights(String insights) { this.insights = insights; }

    @Override
    public String toString() { return name; }
}
