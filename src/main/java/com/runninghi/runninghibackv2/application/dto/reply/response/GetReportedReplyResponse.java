package com.runninghi.runninghibackv2.application.dto.reply.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GetReportedReplyResponse (
        @Schema(description = "댓글 번호", example = "2")
        Long replyNo,
        @Schema(description = "댓글 작성자 닉네임", example = "러너1")
        String memberName,
        @Schema(description = "게시글 번호", example = "1")
        Long postNo,
        @Schema(description = "댓글 내용", example = "댓글 내용")
        String replyContent,
        @Schema(description = "신고된 횟수", example = "1")
        int reportedCount,
        @Schema(description = "댓글 삭제 여부", example = "false")
        boolean isDeleted,
        @Schema(description = "부모 댓글", example = "")
        Long parentReplyNo,
        @Schema(description = "댓글 생성 일", example = "2024-03-27T13:23:12")
        LocalDateTime createDate,
        @Schema(description = "댓글 수정 일", example = "2024-03-27T13:23:12")
        LocalDateTime updateDate

){
}