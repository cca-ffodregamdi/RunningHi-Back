package com.runninghi.runninghibackv2.common.dummy;

import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.dto.AccessTokenInfo;
import com.runninghi.runninghibackv2.common.dto.RefreshTokenInfo;
import com.runninghi.runninghibackv2.common.response.ApiResult;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.vo.RunDataVO;
import com.runninghi.runninghibackv2.domain.enumtype.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TestToken {

    private final JwtTokenProvider jwtTokenProvider;
    private final TestMemberRepository testMemberRepository;

    @PostMapping("/test/token")
    public ResponseEntity<ApiResult<TestReviewerResponse>> getTokens() {
        try {
            String userName = "유저 : 테스트 계정 이름";

            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // 4326 is the SRID for WGS84
            Point point = geometryFactory.createPoint(new Coordinate(127.543, 36.9876));

            Member user = testMemberRepository.findByName(userName)
                    .orElseGet(() -> {
                        Member newUser = Member.builder()
                                .alarmConsent(true)
                                .kakaoId("67890")
                                .name(userName)
                                .nickname("유저 : 테스트용 유저입니다.")
                                .isActive(true)
                                .role(Role.USER)
                                .runDataVO(new RunDataVO(0.0,0.0,2,1))
                                .geometry(point)
                                .build();
                        testMemberRepository.saveAndFlush(newUser);
                        return newUser;
                    });

            // 단일 유저 토큰만 생성
            AccessTokenInfo userTokenInfo = AccessTokenInfo.from(user);
            String userAccessToken = jwtTokenProvider.createAccessToken(userTokenInfo);

            RefreshTokenInfo userRefreshTokenInfo = RefreshTokenInfo.from(user);
            String userRefreshToken = jwtTokenProvider.createRefreshToken(userRefreshTokenInfo);

            user.updateRefreshToken(userRefreshToken);
            testMemberRepository.saveAndFlush(user);

            TokensAndInfo userTokensAndInfo = TokensAndInfo.from(user, userAccessToken, userRefreshToken);
            TestReviewerResponse response = TestReviewerResponse.from(true, userTokensAndInfo);

            return ResponseEntity.ok(ApiResult.success("테스터 토큰 발급 완료", response));
        } catch (IncorrectResultSizeDataAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.error(HttpStatus.BAD_REQUEST, "이미 테스트 유저가 존재함. 서버 에러."));
        }
    }

}