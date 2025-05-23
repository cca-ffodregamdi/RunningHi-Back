package com.runninghi.runninghibackv2.common.dummy;

import com.runninghi.runninghibackv2.domain.entity.Member;

public record TokensAndInfo(
        Long memberNo,
        String nickname,
        String accessToken,
        String refreshToken
) {
    public static TokensAndInfo from(Member member, String accessToken, String refreshToken) {
        // iOS가 기대하는 형태로 Bearer 접두사 추가
        String formattedAccessToken = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
        return new TokensAndInfo(member.getMemberNo(), member.getNickname(), formattedAccessToken, refreshToken);
    }
}