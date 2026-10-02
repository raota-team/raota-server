package com.raota.mobile.ramenlog.domain.model;

public enum RevisitIntention {

    OFTEN(5), SOMETIMES(3), ONCE_IS_ENOUGH(1);

    private final int score;

    RevisitIntention(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }

}
