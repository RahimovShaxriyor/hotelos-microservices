package com.hotelos.maintenance.domain;

public enum IssuePriority {
    CRITICAL(1), HIGH(2), NORMAL(3), LOW(4);

    private final int rank;
    IssuePriority(int rank) { this.rank = rank; }
    public int getRank() { return rank; }
}
