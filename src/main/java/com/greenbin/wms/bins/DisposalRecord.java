package com.greenbin.wms.bins;

import com.greenbin.wms.people.User;

import java.time.LocalDateTime;

/** One barcode scan + disposal (FR_08). Used for trends, points and the leaderboard. */
public class DisposalRecord {
    private final User user;
    private final SmartBin bin;
    private final Waste waste;
    private final boolean correct;
    private final int points;
    private final LocalDateTime time;

    public DisposalRecord(User user, SmartBin bin, Waste waste, boolean correct, int points, LocalDateTime time) {
        this.user = user;
        this.bin = bin;
        this.waste = waste;
        this.correct = correct;
        this.points = points;
        this.time = time;
    }

    public User getUser() { return user; }
    public SmartBin getBin() { return bin; }
    public Waste getWaste() { return waste; }
    public boolean isCorrect() { return correct; }
    public int getPoints() { return points; }
    public LocalDateTime getTime() { return time; }
}
