package com.runninghi.runninghibackv2.common.dummy;

import com.runninghi.runninghibackv2.domain.entity.Member;

public record TokensAndInfo(
        Long memberNo,
        String nickname,
        String accessToken,
        String refreshToken
) {
    public static TokensAndInfo from(Member member, String accessToken, String refreshToken) {
        return new TokensAndInfo(member.getMemberNo(), member.getNickname(), accessToken, refreshToken);
    }
}