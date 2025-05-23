package com.runninghi.runninghibackv2.common.dummy;


public record TestReviewerResponse (
        Boolean isReviewer,
        TokensAndInfo user
) {
    public static TestReviewerResponse from(Boolean isReviewer, TokensAndInfo user) {
        return new TestReviewerResponse(isReviewer, user);
    }
}