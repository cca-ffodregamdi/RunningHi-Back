package com.runninghi.runninghibackv2.common.dummy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestReviewerResponse {
    private boolean isReviewer;
    private TokensAndInfo user;

    public static TestReviewerResponse from(boolean isReviewer, TokensAndInfo user) {
        return TestReviewerResponse.builder()
                .isReviewer(isReviewer)
                .user(user)
                .build();
    }
}