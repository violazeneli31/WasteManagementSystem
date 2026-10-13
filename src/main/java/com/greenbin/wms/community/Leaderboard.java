package com.greenbin.wms.community;

import com.greenbin.wms.bins.DisposalRecord;
import com.greenbin.wms.people.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FR_24: ranks users by points and communities (neighbourhoods) by performance.
 * Community score = points per member + bonus for correct disposals.
 */
public class Leaderboard {
    public static final String TOP_BADGE = "Top Recycling Community";

    /** One row of the community ranking. */
    public static class CommunityScore {
        public final String community;
        public final int members;
        public final int totalPoints;
        public final double correctRate;      // % of disposals in the right bin
        public final double score;

        CommunityScore(String community, int members, int totalPoints, double correctRate) {
            this.community = community;
            this.members = members;
            this.totalPoints = totalPoints;
            this.correctRate = correctRate;
            this.score = (double) totalPoints / members + correctRate;
        }
    }

    private final List<User> users;

    public Leaderboard(List<User> users) { this.users = users; }

    public List<User> rankUsers() {
        List<User> ranked = new ArrayList<>(users);
        ranked.sort((a, b) -> b.getPoints() - a.getPoints());
        return ranked;
    }

    public List<CommunityScore> rankCommunities() {
        Map<String, List<User>> groups = new LinkedHashMap<>();
        for (User u : users) groups.computeIfAbsent(u.getCommunity(), c -> new ArrayList<>()).add(u);

        List<CommunityScore> scores = new ArrayList<>();
        for (Map.Entry<String, List<User>> g : groups.entrySet()) {
            int points = 0, disposals = 0, correct = 0;
            for (User u : g.getValue()) {
                points += u.getPoints();
                for (DisposalRecord r : u.getDisposalHistory()) {
                    disposals++;
                    if (r.isCorrect()) correct++;
                }
            }
            double rate = disposals == 0 ? 0 : 100.0 * correct / disposals;
            scores.add(new CommunityScore(g.getKey(), g.getValue().size(), points, rate));
        }
        scores.sort((a, b) -> Double.compare(b.score, a.score));
        return scores;
    }

    /** Gives the badge to every member of the best community. Returns that community. */
    public String updateLeaderboard() {
        List<CommunityScore> ranking = rankCommunities();
        if (ranking.isEmpty()) return null;
        String winner = ranking.get(0).community;
        for (User u : users) {
            u.getBadges().remove(TOP_BADGE);
            if (u.getCommunity().equals(winner)) u.getBadges().add(TOP_BADGE);
        }
        return winner;
    }

    public String showLeaderboard() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (User u : rankUsers()) sb.append(i++).append(". ").append(u.getDisplayName()).append(" - ").append(u.getPoints()).append('\n');
        return sb.toString();
    }
}
