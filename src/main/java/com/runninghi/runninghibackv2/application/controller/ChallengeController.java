package com.runninghi.runninghibackv2.application.controller;

import com.runninghi.runninghibackv2.application.dto.challenge.request.CreateChallengeRequest;
import com.runninghi.runninghibackv2.application.dto.challenge.request.UpdateChallengeRequest;
import com.runninghi.runninghibackv2.application.dto.challenge.response.*;
import com.runninghi.runninghibackv2.application.dto.memberchallenge.request.CreateMemberChallengeRequest;
import com.runninghi.runninghibackv2.application.dto.memberchallenge.response.CreateMemberChallengeResponse;
import com.runninghi.runninghibackv2.application.dto.memberchallenge.response.GetAllMemberChallengeResponse;
import com.runninghi.runninghibackv2.application.dto.memberchallenge.response.GetMemberChallengeResponse;
import com.runninghi.runninghibackv2.application.service.ChallengeService;
import com.runninghi.runninghibackv2.application.service.MemberChallengeService;
import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.annotations.HasAccess;
import com.runninghi.runninghibackv2.common.response.ApiResult;
import com.runninghi.runninghibackv2.domain.enumtype.ChallengeStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "챌린지 API", description = "챌린지 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final MemberChallengeService memberChallengeService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 챌린지를 생성합니다.
     *
     * @return 저장된 Challenge 데이터를 반환합니다.
     * @apiNote 관리자만 권한이 있습니다.
     */
    @Operation(summary = "챌린지 저장", description = "챌린지를 저장합니다.\n 관리자만 챌린지를 생성할 수 있습니다.")
    @HasAccess
    @PostMapping()
    public ResponseEntity<ApiResult<CreateChallengeResponse>> createChallenge(
            @ModelAttribute CreateChallengeRequest request,
            @RequestHeader(name = "Authorization") String bearerToken) {

        Long memberNo = jwtTokenProvider.getMemberNoFromToken(bearerToken);

        CreateChallengeResponse response = challengeService.createChallenge(request, memberNo);

        return ResponseEntity.ok(ApiResult.success("챌린지 저장 성공", response));
    }

    /**
     * 챌린지 상태 값으로 챌린지를 조회합니다.
     * @param status SCHEDULED=예정된 챌린지, IN_PROGRESS=진행중인 챌린지, COMPLETED=종료된 챌린지가 조회됩니다.
     * @return Challenge 리스트를 반환합니다.
     */
    @Operation(summary = "챌린지 상태 별 챌린지 조회", description = "SCHEDULED=예정된 챌린지, IN_PROGRESS=진행중인 챌린지, \n" +
            "COMPLETED=종료된 챌린지가 조회됩니다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResult<GetAllChallengeResponse>> getAllChallengesByStatus(
            @RequestParam(name = "status") ChallengeStatus status,
            @RequestHeader(name = "Authorization") String bearerToken) {

        Long memberNo = jwtTokenProvider.getMemberNoFromToken(bearerToken);

        GetAllChallengeResponse response = challengeService.getAllChallengesByStatus(status, memberNo);

        return ResponseEntity.ok(ApiResult.success("챌린지 전체 조회 성공", response));
    }

    /**
     * 챌린지 상세 정보를 조회합니다.
     * 랭킹 조회 시 전체 회원의 러닝 순위를 조회합니다. 기록이 높을수록 높은 순위를 부여합니다.
     * 동일한 값이 있을 경우 동일한 순위를 부여하고 다음 순위를 건너뜁니다.
     * @return  전체 회원 랭킹을 포함한 Challenge 기본 정보가 반환됩니다.
     * @apiNote 이 메서드를 사용하기 위해서는 요청 헤더에 유효한 Bearer 토큰이 포함되어야 합니다.
     *          토큰이 유효하지 않거나, 토큰에 해당하는 사용자가 존재하지 않을 경우 접근이 거부됩니다.
     */
    @Operation(summary = "챌린지 상세 조회", description = "챌린지 상세 정보를 조회합니다.")
    @GetMapping("/{challengeNo}")
    public ResponseEntity<ApiResult<GetChallengeResponse>> getChallengeById(
            @PathVariable Long challengeNo) {

        GetChallengeResponse response = challengeService.getChallengeById(challengeNo);

        return ResponseEntity.ok(ApiResult.success("챌린지 상세 조회 성공", response));
    }

    @Operation(summary = "챌린지 수정", description = "챌린지 제목, 내용, 카테고리, 목표수치, 시작일자, 종료일자를 수정할 수 있습니다." +
            "\n 관리자만 챌린지를 수정할 수 있습니다")
    @HasAccess
    @PutMapping("/{challengeNo}")
    public ResponseEntity<ApiResult<UpdateChallengeResponse>> updateChallenge(
            @PathVariable Long challengeNo,
            @RequestBody UpdateChallengeRequest request) {

        UpdateChallengeResponse response = challengeService.updateChallenge(challengeNo, request);

        return ResponseEntity.ok(ApiResult.success("챌린지 수정 성공", response));
    }

    @Operation(summary = "챌린지 삭제", description = "챌린지를 삭제합니다. \n 관리자만 챌린지를 삭제할 수 있습니다")
    @HasAccess
    @DeleteMapping("/{challengeNo}")
    public ResponseEntity<ApiResult<DeleteChallengeResponse>> deleteChallenge(@PathVariable Long challengeNo) {

        DeleteChallengeResponse response = challengeService.deleteChallenge(challengeNo);

        return ResponseEntity.ok(ApiResult.success("챌린지 삭제 성공", response));
    }

    @Operation(summary = "나의 챌린지 저장", description = "나의 챌린지를 생성합니다. \n 챌린지 참여하기를 누르면 생성됩니다.")
    @PostMapping("/my-challenge")
    public ResponseEntity<ApiResult<CreateMemberChallengeResponse>> createMemberChallenge(
            @RequestBody CreateMemberChallengeRequest request,
            @RequestHeader(name = "Authorization") String bearerToken) {

        Long memberNo = jwtTokenProvider.getMemberNoFromToken(bearerToken);

        CreateMemberChallengeResponse response = memberChallengeService.createMemberChallenge(request, memberNo);

        return ResponseEntity.ok(ApiResult.success("나의 챌린지 저장 성공", response));
    }

    /**
     * 회원이 참여한 챌린지 데이터입니다.
     * 챌린지 상태 값으로 회원의 챌린지를 조회합니다.
     * @param status IN_PROGRESS-진행중인 챌린지, COMPLETED-종료된 챌린지가 조회됩니다. SCHEDULED는 허용되지 않습니다.
     * @return MemberChallenge 리스트를 반환합니다.
     * @apiNote 이 메서드를 사용하기 위해서는 요청 헤더에 유효한 Bearer 토큰이 포함되어야 합니다.
     *          토큰이 유효하지 않거나, 토큰에 해당하는 사용자가 존재하지 않을 경우 접근이 거부됩니다.
     */
    @Operation(summary = "활성화 여부에 따른 나의 챌린지 전체 조회",
            description = "IN_PROGRESS-진행중인 챌린지, COMPLETED-종료된 챌린지가 조회됩니다. \n" +
                    "SCHEDULED는 허용되지 않습니다.")
    @GetMapping("/my-challenge/status")
    public ResponseEntity<ApiResult<GetAllMemberChallengeResponse>> getAllMemberChallengesByStatus(
            @RequestParam(name = "status") ChallengeStatus status,
            @RequestHeader(name = "Authorization") String bearerToken) {

        if (status == ChallengeStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SCHEDULED는 잘못된 요청입니다.");
        }

        Long memberNo = jwtTokenProvider.getMemberNoFromToken(bearerToken);

        GetAllMemberChallengeResponse response = memberChallengeService.getMemberChallengesByStatus(memberNo, status);

        return ResponseEntity.ok(ApiResult.success("나의 챌린지 전체 조회 성공", response));
    }

    /**
     * 회원이 참여한 챌린지 데이터입니다.
     * challengeNo와 memberNo로 MemberChallenge 데이터를 조회합니다.
     * 회원 랭킹을 포함한 나의 챌린지 상세 정보를 조회합니다.
     * 전체 회원 랭킹과 로그인한 회원의 랭킹을 조회합니다. 기록이 높을수록 높은 순위를 부여합니다.
     * 동일한 값이 있을 경우 동일한 순위를 부여하고 다음 순위를 건너뜁니다.
     * @return 회원의 기록, 랭킹을 포함한 Challenge 데이터를 반환합니다.
     * @apiNote 이 메서드를 사용하기 위해서는 요청 헤더에 유효한 Bearer 토큰이 포함되어야 합니다.
     *          토큰이 유효하지 않거나, 토큰에 해당하는 사용자가 존재하지 않을 경우 접근이 거부됩니다.
     */
    @Operation(summary = "나의 챌린지 상세 조회", description = "로그인한 사용자가 참여중인 챌린지 목록 화면에서 " +
            "선택한 챌린지의 정보를 조회합니다.")
    @GetMapping("/my-challenge/{challengeNo}")
    public ResponseEntity<ApiResult<GetMemberChallengeResponse>> getMemberChallengeById(
            @PathVariable Long challengeNo,
            @RequestHeader(name = "Authorization") String bearerToken) {

        Long memberNo = jwtTokenProvider.getMemberNoFromToken(bearerToken);

        GetMemberChallengeResponse response = memberChallengeService.getMemberChallengeByChallengeId(memberNo, challengeNo);

        return ResponseEntity.ok(ApiResult.success("나의 챌린지 상세 조회 성공", response));
    }
}
