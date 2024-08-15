package com.runninghi.runninghibackv2.application.dto.alarm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateAlarmRequest(
        @Schema(description = "알림 제목", example = "공지 알림")
        @NotBlank(message = "알림 제목을 입력해주세요.")
        String title,
        @Schema(description = "알림 생성 대상자 번호", example = "1")
        Long memberNo
) {

        public static CreateAlarmRequest of (String title, Long memberNo) {
                return new CreateAlarmRequest(title, memberNo);
        }
}
