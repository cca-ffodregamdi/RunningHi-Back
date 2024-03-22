package com.runninghi.runninghibackv2.reply.application.controller;

import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.dto.MemberJwtInfo;
import com.runninghi.runninghibackv2.common.response.ApiResult;
import com.runninghi.runninghibackv2.reply.application.dto.request.CreateReplyRequest;
import com.runninghi.runninghibackv2.reply.application.dto.request.DeleteReplyRequest;
import com.runninghi.runninghibackv2.reply.application.dto.request.UpdateReplyRequest;
import com.runninghi.runninghibackv2.reply.application.dto.response.CreateReplyResponse;
import com.runninghi.runninghibackv2.reply.application.dto.response.GetReplyListResponse;
import com.runninghi.runninghibackv2.reply.application.dto.response.UpdateReplyResponse;
import com.runninghi.runninghibackv2.reply.application.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reply")
public class ReplyController {

    private final ReplyService replyService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/{postNo}")
    public ResponseEntity<ApiResult> getReplyList(@PathVariable(name = "postNo") Long postNo) {

        List<GetReplyListResponse> replyList =  replyService.getReplyList(postNo);

        return ResponseEntity.ok().body(ApiResult.success("성공적으로 조회되었습니다.", replyList));
    }

    @GetMapping("/byMember")
    public ResponseEntity<ApiResult> getReplyListByMemberNo(@RequestHeader(name = "memberNo") Long memberNo) {

        List<GetReplyListResponse> replyList = replyService.getReplyListByMemberNo(memberNo);

        return ResponseEntity.ok().body(ApiResult.success("성공적으로 조회되었습니다.", replyList));
    }

    // 신고된 댓글들 조회 API 필요

    @PostMapping
    public ResponseEntity<ApiResult> createReply(@RequestBody CreateReplyRequest request) {

        CreateReplyResponse reply = replyService.createReply(request);

        return ResponseEntity.ok().body(ApiResult.success("성공적으로 생성되었습니다.", reply));
    }

    @PutMapping("/update/{replyNo}")
    public ResponseEntity<ApiResult> updateReply(@RequestHeader("Authorization") String bearerToken,
                                                 @PathVariable(name = "replyNo") Long replyNo,
                                                 @RequestBody(required = true) String replyContent) {

        MemberJwtInfo memberJwtInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        UpdateReplyRequest request = UpdateReplyRequest.of(memberJwtInfo.memberNo(), memberJwtInfo.role(), replyContent);
        UpdateReplyResponse reply = replyService.updateReply(replyNo, request);

        return ResponseEntity.ok().body(ApiResult.success("성공적으로 수정되었습니다.", reply));
    }

    @PutMapping("/delete/{replyNo}")
    public ResponseEntity<ApiResult> deleteReply(@PathVariable(name = "replyNo") Long replyNo,
                                                 @RequestHeader("Authorization") String bearerToken) {
        MemberJwtInfo memberJwtInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        DeleteReplyRequest request = DeleteReplyRequest.of(replyNo, memberJwtInfo.role(), memberJwtInfo.memberNo());
        replyService.deleteReply(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.success("성공적으로 삭제되었습니다.", null));
    }




}
