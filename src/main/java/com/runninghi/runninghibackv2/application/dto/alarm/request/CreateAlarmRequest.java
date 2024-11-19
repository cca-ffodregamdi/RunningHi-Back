package com.runninghi.runninghibackv2.application.dto.alarm.request;

import com.runninghi.runninghibackv2.domain.enumtype.AlarmType;
import com.runninghi.runninghibackv2.domain.enumtype.TargetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlarmRequest {

        @Schema(description = "알림 제목", example = "공지 알림")
        @NotBlank(message = "알림 제목을 입력해주세요.")
        private String title;

        @Schema(description = "알림 생성 대상자 번호", example = "1")
        private Long targetMemberNo;

        @Schema(description = "알림 타입", example = "reply")
        private AlarmType alarmType;

        @Schema(description = "이동해야 할 페이지", example = "Feed")
        private TargetPage targetPage;

        @Schema(description = "관련 식별 값", example = "234")
        private Long targetId;

        @Schema(description = "부가 정보", example = "{\"key1\": \"value1\", \"key2\": \"value2\"}")
        private Map<String, String> additionalData;

        @Schema(description = "FCM 토큰")
        @NotBlank(message = "토큰을 포함시켜주세요.")
        private String fcmToken;

        public CreateAlarmRequest(String title, Long targetMemberNo, AlarmType alarmType, TargetPage targetPage, Long targetId, String fcmToken) {
                this.title = title;
                this.targetMemberNo = targetMemberNo;
                this.alarmType = alarmType;
                this.targetPage = targetPage;
                this.targetId = targetId;
                this.fcmToken = fcmToken;
        }
}