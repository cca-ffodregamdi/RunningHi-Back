package com.runninghi.runninghibackv2.application.service;

import com.runninghi.runninghibackv2.application.dto.challenge.request.CreateChallengeRequest;
import com.runninghi.runninghibackv2.application.dto.challenge.request.UpdateChallengeRequest;
import com.runninghi.runninghibackv2.application.dto.challenge.response.*;
import com.runninghi.runninghibackv2.application.dto.memberchallenge.response.ChallengeRankResponse;
import com.runninghi.runninghibackv2.common.exception.custom.ImageException;
import com.runninghi.runninghibackv2.domain.entity.Challenge;
import com.runninghi.runninghibackv2.domain.enumtype.ChallengeStatus;
import com.runninghi.runninghibackv2.domain.repository.ChallengeQueryRepository;
import com.runninghi.runninghibackv2.domain.repository.ChallengeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryRepository challengeQueryRepository;
    private final ImageService imageService;

    @Transactional
    public CreateChallengeResponse createChallenge(CreateChallengeRequest request, Long memberNo) {

        String challengeImageUrl = null;
        try {
            challengeImageUrl = imageService.uploadImage(request.image(), memberNo, "challenge/");
        } catch (IOException e) {
            log.error("챌린지 이미지 업로드 중 오류 발생: {}", e.getMessage());
            throw new ImageException("챌린지 이미지 업로드 중 오류가 발생했습니다.");
        }

        Challenge challenge = Challenge.builder()
                .title(request.title())
                .content(request.content())
                .challengeCategory(request.challengeCategory())
                .imageUrl(challengeImageUrl)
                .goal(request.goal())
                .goalDetail(request.goalDetail())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(ChallengeStatus.SCHEDULED)
                .build();

        challengeRepository.save(challenge);

        return CreateChallengeResponse.from(challenge);
    }

    @Transactional(readOnly = true)
    public GetAllChallengeResponse getAllChallengesByStatus(ChallengeStatus status, Long memberNo) {

        log.info("전체 챌린지 조회. 상태: {}", status);
        List<ChallengeListResponse> challengeList =
                challengeQueryRepository.findChallengesByStatusAndMember(status, memberNo).stream()
                .map(ChallengeListResponse::from)
                .toList();
        int challengeCount = challengeList.size();

        return new GetAllChallengeResponse(challengeList, challengeCount);
    }

    @Transactional(readOnly = true)
    public GetChallengeResponse getChallengeById(Long challengeNo) {

        Challenge challenge = challengeRepository.findById(challengeNo)
                .orElseThrow(EntityNotFoundException::new);

        List<ChallengeRankResponse> challengeRanking =
                challengeQueryRepository.findTop100Ranking(challenge.getChallengeNo());

        return GetChallengeResponse.from(challenge, challengeRanking);
    }

    @Transactional
    public UpdateChallengeResponse updateChallenge(Long challengeNo, UpdateChallengeRequest request) {

        Challenge challenge = challengeRepository.findById(challengeNo)
                .orElseThrow(EntityNotFoundException::new);

        challenge.update(request);

        return UpdateChallengeResponse.from(challenge);
    }

    @Transactional
    public DeleteChallengeResponse deleteChallenge(Long challengeNo) {

        challengeRepository.deleteById(challengeNo);

        return DeleteChallengeResponse.from(challengeNo);
    }
}
