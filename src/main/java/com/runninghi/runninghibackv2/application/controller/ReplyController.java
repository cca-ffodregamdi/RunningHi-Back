package com.runninghi.runninghibackv2.application.controller;

import com.runninghi.runninghibackv2.application.dto.reply.request.*;
import com.runninghi.runninghibackv2.application.dto.reply.response.CreateReplyResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetReplyListResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetReportedReplyResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.UpdateReplyResponse;
import com.runninghi.runninghibackv2.application.service.ReplyService;
import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.annotations.HasAccess;
import com.runninghi.runninghibackv2.common.dto.AccessTokenInfo;
import com.runninghi.runninghibackv2.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reply")
@Tag(name = "댓글 API", description = "댓글 관련 API")
public class ReplyController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ReplyService replyService;

    private static final String GET_RESPONSE_MESSAGE = "댓글 리스트 조회 성공";
    private static final String CREATE_RESPONSE_MESSAGE = "댓글 작성 성공";
    private static final String UPDATE_RESPONSE_MESSAGE = "댓글 수정 성공";
    private static final String DELETE_RESPONSE_MESSAGE = "댓글 삭제 성공";
    private static final String NO_CONTENT_RESPONSE_MESSAGE = "검색 결과가 없습니다.";

    @GetMapping(value = "/{postNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "댓글 리스트 조회", description = "특정 게시물에 대한 댓글들 리스트를 조회합니다.", responses = @ApiResponse(description = GET_RESPONSE_MESSAGE))
    public ResponseEntity<ApiResult<List<GetReplyListResponse>>> getReplyList(@Parameter(description = "특정 게시물 번호") @PathVariable(name = "postNo") Long postNo) {

        List<GetReplyListResponse> replyList =  replyService.getReplyList(postNo);

        return ResponseEntity.ok().body(ApiResult.success(GET_RESPONSE_MESSAGE, replyList));
    }

    @GetMapping(value = "/byMember", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "특정 회원 댓글 리스트 조회", description = "특정 회원의 댓글 리스트를 조회합니다.", responses = @ApiResponse(description = GET_RESPONSE_MESSAGE))
    public ResponseEntity<ApiResult<List<GetReplyListResponse>>> getReplyListByMemberNo(@Parameter(description = "특정 회원 번호") @RequestHeader(name = "memberNo") Long memberNo) {

        List<GetReplyListResponse> replyList = replyService.getReplyListByMemberNo(memberNo);

        return ResponseEntity.ok().body(ApiResult.success(GET_RESPONSE_MESSAGE, replyList));
    }

    @HasAccess
    @GetMapping(value = "/reported", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "신고된 댓글 리스트 조회", description = "신고된 댓글 리스트를 조회합니다.", responses = @ApiResponse(description = GET_RESPONSE_MESSAGE))
    public ResponseEntity<ApiResult<Page<GetReportedReplyResponse>>> getReportedReplyList(@Valid @ModelAttribute GetReportedReplySearchRequest searchRequest) {

        Sort sort = Sort.by( Sort.Direction.fromString(searchRequest.getSortDirection()), "createDate" );
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
        Page<GetReportedReplyResponse> reportedReplyPage = replyService.getReportedReplyList(
                GetReportedReplyRequest.of(pageable, searchRequest.getSearch(), searchRequest.getReportStatus())
        );
        if (reportedReplyPage == null) return ResponseEntity.ok().body(ApiResult.success(NO_CONTENT_RESPONSE_MESSAGE, null));

        return ResponseEntity.ok().body(ApiResult.success(GET_RESPONSE_MESSAGE, reportedReplyPage));
    }


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "댓글 작성",
            description = "댓글을 작성하고, 해당 게시글 작성자와 부모 댓글이 있다면 부모 댓글 작성자에게 알림을 발송합니다.",
            responses = @ApiResponse(responseCode = "200", description = CREATE_RESPONSE_MESSAGE)
    )
    public ResponseEntity<ApiResult<CreateReplyResponse>> createReply(@Parameter(description = "사용자 인증을 위한 Bearer Token")
                                                                        @RequestHeader("Authorization") String bearerToken,
                                                                      @RequestBody CreateReplyRequest request) {
        AccessTokenInfo accessTokenInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        CreateReplyResponse response = replyService.createReply(request, accessTokenInfo.memberNo());

        return ResponseEntity.ok().body(ApiResult.success(CREATE_RESPONSE_MESSAGE, response));
    }

    @PutMapping(value = "/update/{replyNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "댓글 수정",
            description = "특정 댓글을 수정합니다.",
            responses = @ApiResponse(responseCode = "200", description = UPDATE_RESPONSE_MESSAGE)
    )
    public ResponseEntity<ApiResult<UpdateReplyResponse>> updateReply(@Parameter(description = "사용자 인증을 위한 BearerToken")
                                                 @RequestHeader("Authorization") String bearerToken,
                                                 @Parameter(description = "수정할 댓글 번호")
                                                 @PathVariable(name = "replyNo") Long replyNo,
                                                 @Parameter(description = "수정할 댓글 내용")
                                                 @RequestBody(required = true) String replyContent) {

        AccessTokenInfo accessTokenInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        UpdateReplyRequest request = UpdateReplyRequest.of(accessTokenInfo.memberNo(), accessTokenInfo.role(), replyContent);
        UpdateReplyResponse reply = replyService.updateReply(replyNo, request);

        return ResponseEntity.ok().body(ApiResult.success(UPDATE_RESPONSE_MESSAGE, reply));
    }

    @PutMapping(value = "/delete/{replyNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "댓글 삭제",
            description = "특정 댓글을 삭제합니다.",
            responses = @ApiResponse(responseCode = "204", description = DELETE_RESPONSE_MESSAGE)
    )
    public ResponseEntity<ApiResult<Void>> deleteReply(@Parameter(description = "삭제할 댓글 번호") @PathVariable(name = "replyNo") Long replyNo,
                                                 @Parameter(description = "사용자 인증을 위한 BearerToken") @RequestHeader("Authorization") String bearerToken) {
        AccessTokenInfo accessTokenInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        DeleteReplyRequest request = DeleteReplyRequest.of(replyNo, accessTokenInfo.role(), accessTokenInfo.memberNo());
        replyService.deleteReply(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.success(DELETE_RESPONSE_MESSAGE, null));
    }

}
