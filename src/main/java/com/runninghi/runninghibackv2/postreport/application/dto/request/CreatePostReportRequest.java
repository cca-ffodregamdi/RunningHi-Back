package com.runninghi.runninghibackv2.postreport.application.dto.request;

import com.runninghi.runninghibackv2.common.enumtype.ReportCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreatePostReportRequest(
        @Schema(description = "신고 유형", example = "SPAM")
        ReportCategory category,
        @Schema(description = "기타 신고 사유", example = "공백 제외 10자 이상의 사유")
        String content,
        @Schema(description = "신고된 게시글 번호", example = "1")
        Long reportedPostNo
)
{}
