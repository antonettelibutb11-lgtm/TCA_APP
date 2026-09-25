package com.example.tca_app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingPoll {
    private String pollId;
    private String eventId;
    private String eventName;
    private String question;
    private List<String> options;
    private Map<String, Long> votesCount;
    private long totalVotes;
    private boolean active;
    private long createdAt;

    public VotingPoll() {
        this.options = new ArrayList<>();
        this.votesCount = new HashMap<>();
    }

    public VotingPoll(String pollId, String eventId, String eventName, String question, List<String> options) {
        this.pollId = pollId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.question = question;
        this.options = options != null ? options : new ArrayList<>();
        this.votesCount = new HashMap<>();
        for (String opt : this.options) {
            this.votesCount.put(opt, 0L);
        }
        this.totalVotes = 0;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
    }

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
        this.pollId = pollId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Map<String, Long> getVotesCount() {
        return votesCount;
    }

    public void setVotesCount(Map<String, Long> votesCount) {
        this.votesCount = votesCount;
    }

    public long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
