package com.runninghi.runninghibackv2.application.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.runninghi.runninghibackv2.application.dto.alarm.request.CreateAlarmRequest;
import com.runninghi.runninghibackv2.application.dto.member.request.AppleLoginRequest;
import com.runninghi.runninghibackv2.application.dto.member.response.AppleTokenResponse;
import com.runninghi.runninghibackv2.auth.apple.*;
import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.dto.AccessTokenInfo;
import com.runninghi.runninghibackv2.common.dto.RefreshTokenInfo;
import com.runninghi.runninghibackv2.common.exception.custom.*;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.vo.RunDataVO;
import com.runninghi.runninghibackv2.domain.enumtype.AlarmType;
import com.runninghi.runninghibackv2.domain.enumtype.Role;
import com.runninghi.runninghibackv2.domain.enumtype.TargetPage;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleOauthService {
    
    private static final int NICKNAME_DIGIT_LENGTH = 6;
    private static final int RANDOM_NUMBER_RANGE = 10;
    private static final String GRANT_TYPE = "authorization_code";
    private static final String SIGNUP_FCM_TITLE = "환영합니다! 러닝을 시작하여 다음 레벨에 도전하세요!";

    private final AppleTokenParser appleTokenParser;
    private final AppleClient appleClient;
    private final ApplePublicKeyGenerator applePublicKeyGenerator;
    private final AppleClaimsValidator appleClaimsValidator;
    private final AppleClientSecretProvider appleClientSecretProvider;

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AlarmService alarmService;

    private final SecureRandom random = new SecureRandom();

    @Value("${apple.client-id}")
    private String clientId;

    @Value("${cloud.aws.s3.default-profile}")
    private String defaultProfileImageUrl;

    // client secret 생성
    @Transactional
    public String createClientSecret() {
        log.info("Apple 클라이언트 시크릿 생성 요청");

        try {
            return appleClientSecretProvider.createClientSecret();
        } catch (Exception e) {
            log.error("Apple 클라이언트 시크릿 생성에 실패했습니다. 오류: {}", e.getMessage(), e);
            throw new AppleOauthClientSecretException();
        }
    }

    // apple refreshToken 요청
    @Transactional
    public AppleTokenResponse getAppleToken(String code, String clientSecret) {
        log.info("Apple 토큰 요청. 인가 코드: {}, 클라이언트 시크릿: {}", code, clientSecret);

        try {
            return appleClient.appleAuth(clientId, code, GRANT_TYPE, clientSecret);
        } catch (Exception e) {
            log.error("Apple 토큰 요청에 실패했습니다. 오류: {}", e.getMessage(), e);
            throw new AppleOauthTokenException();
        }
    }

    // apple user 생성
    @Transactional
    public Map<String, String> appleOauth(AppleLoginRequest request, AppleTokenResponse appleTokenResponse) {
        log.info("Apple OAuth 요청을 받았습니다. 요청: {}, 토큰 응답: {}", request, appleTokenResponse);

        // identity_token의 header를 추출
        Map<String, String> appleTokenHeader = appleTokenParser.parseHeader(request.identityToken());

        // identity_token을 검증하기 위해 애플의 publicKey list 요청
        ApplePublicKeys applePublicKeys = appleClient.getApplePublicKeys();

        // publicKey list에서 identity_token과 alg와 kid가 일치하는 publicKey를 찾기
        // 해당 publicKey로 identity_token 추출에 사용할 RSApublicKey 생성
        PublicKey publicKey = applePublicKeyGenerator.generate(appleTokenHeader, applePublicKeys);

        // identity_token을 publicKey로 검증하여 claim 추출 : 서명 검증
        Claims claims = appleTokenParser.extractClaims(request.identityToken(), publicKey);

        // iss, aud, exp, 검증
        if (!appleClaimsValidator.isValid(claims)) {
            log.error("Apple Claims 유효성 검사 실패. 잘못된 Apple 토큰입니다. {}", claims);
            throw new AppleOauthClaimsException();
        }

        Map<String, String> appleResponse = extractAppleResponse(claims);

        Optional<Member> optionalMember = memberRepository.findByAppleId(appleResponse.get("sub"));

        return optionalMember.map(this::loginWithApple)
                .orElseGet(() -> {
                    try {
                        return Collections.unmodifiableMap(loginWithAppleCreateMember(appleResponse, appleTokenResponse.refreshToken()));
                    } catch (FirebaseMessagingException e) {
                        throw new FcmException(e.getMessage());
                    }
                });
    }

    // id-token에서 sub, name 추출
    @Transactional
    public Map<String, String> extractAppleResponse(Claims claims) {
        log.debug("Apple Claims에서 응답 추출. Claims: {}", claims);

        Map<String, String> appleResponse = new HashMap<>();

        appleResponse.put("sub", Objects.toString(claims.get("sub"), "N/A"));
        appleResponse.put("name", Objects.toString(claims.get("name"), "N/A"));
        log.debug("추출된 Apple 응답: {}", appleResponse);

        return appleResponse;
    }

    // 애플 연결해제, 회원 탈퇴 처리
    @Transactional
    public boolean unlinkAndDeleteMember(Long memberNo, String clientSecret) throws InterruptedException {
        log.info("애플 회원 탈퇴 요청. 회원 번호: {}", memberNo);

        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> {
                    log.error("회원 번호 {}에 해당하는 회원을 찾을 수 없습니다.", memberNo);
                    return new EntityNotFoundException();
                });

        try {
            ResponseEntity<String> response = appleClient.revokeToken(
                    clientId,
                    member.getAppleRefreshToken(),
                    clientSecret,
                    "refresh_token"
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                member.deactivateMember();
                memberRepository.save(member);
                log.info("애플 회원 탈퇴 및 비활성화 성공. 회원 번호: {}", memberNo);

                return member.isActive();
            } else {
                log.error("애플 회원 탈퇴 실패. 응답 코드: {}", response.getStatusCode());
                throw new AppleOauthUnlinkException();
            }
        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생. 오류: {}", e.getMessage(), e);
            throw new AppleOauthUnlinkException();
        }

    }

    // 새로운 토큰 생성 & 반환
    private Map<String, String> generateTokens(Member member, boolean isNewMember) {
        log.info("새로운 토큰 생성 요청. 회원 번호: {}", member.getMemberNo());

        AccessTokenInfo accessTokenInfo = new AccessTokenInfo(member.getMemberNo(), member.getRole());
        RefreshTokenInfo refreshTokenInfo = new RefreshTokenInfo(member.getAppleId(), member.getRole());

        String refreshToken = jwtTokenProvider.createRefreshToken(refreshTokenInfo);
        String accessToken = jwtTokenProvider.createAccessToken(accessTokenInfo);

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);

//        if (isNewMember)
            response.put("memberNo", member.getMemberNo().toString());

        log.info("토큰 생성 완료. 응답: {}", response);

        return response;
    }

    // 로그인 메서드
    private Map<String, String> loginWithApple(Member member) {
        log.info("애플 로그인 처리. 회원 번호: {}", member.getMemberNo());

        member.activateMember();  // 멤버의 활성화 상태를 true로 변경, deactivateDate를 null로 설정

        return generateTokens(member, false);
    }

    // 회원 생성 및 로그인 메서드
    private Map<String, String> loginWithAppleCreateMember(Map<String, String> appleResponse, String appleRefreshToken) throws FirebaseMessagingException {
        log.info("새로운 애플 회원 생성 및 로그인 처리. 애플 응답: {}", appleResponse);

        Member member = Member.builder()
                .appleId(appleResponse.get("sub"))
                .name(appleResponse.get("name"))
                .appleRefreshToken(appleRefreshToken)
                .nickname("러너 " + generateRandomDigits())
                .profileImageUrl(defaultProfileImageUrl)
                .isActive(true)
                .isBlacklisted(false)
                .isTermsAgreed(false)
                .role(Role.USER)
                .runDataVO(new RunDataVO(0.0,0.0,2,0))
                .build();

        memberRepository.saveAndFlush(member);
        log.info("새로운 애플 회원 생성 완료. 회원 번호: {}", member.getMemberNo());

        // 회원가입한 사용자에게 알림
        CreateAlarmRequest alarmRequest = CreateAlarmRequest.builder()
                .title(SIGNUP_FCM_TITLE)
                .targetMemberNo(member.getMemberNo())
                .alarmType(AlarmType.LEVEL_UP)
                .targetPage(TargetPage.MYPAGE)
                .targetId(member.getMemberNo())
                .fcmToken(member.getFcmToken())
                .build();
        alarmService.createPushAlarm(alarmRequest);

        return generateTokens(member, true);
    }

    /**
     * 무작위 숫자로 구성된 문자열을 생성하여 반환합니다.
     *
     * @return 생성된 무작위 숫자 8자리 문자열
     */
    private String generateRandomDigits() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < NICKNAME_DIGIT_LENGTH; i++) {
            stringBuilder.append(random.nextInt(RANDOM_NUMBER_RANGE));
        }

        String randomDigits = stringBuilder.toString();
        log.debug("생성된 무작위 숫자 문자열: {}", randomDigits);

        return randomDigits;
    }

}
