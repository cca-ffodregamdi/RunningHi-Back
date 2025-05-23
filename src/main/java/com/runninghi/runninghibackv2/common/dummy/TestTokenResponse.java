package com.runninghi.runninghibackv2.common.dummy;

public record TestTokenResponse(
        TokensAndInfo admin,
        TokensAndInfo user
) {
    public static TestTokenResponse from(TokensAndInfo admin, TokensAndInfo user) {
        return new TestTokenResponse(admin, user);
    }

    public static TestTokenResponse from(TokensAndInfo user) {
        return new TestTokenResponse(null, user);
    }
}