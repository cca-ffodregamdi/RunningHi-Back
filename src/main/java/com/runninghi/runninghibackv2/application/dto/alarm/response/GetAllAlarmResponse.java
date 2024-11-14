package com.runninghi.runninghibackv2.application.dto.alarm.response;

import com.runninghi.runninghibackv2.domain.entity.Alarm;
import com.runninghi.runninghibackv2.domain.enumtype.AlarmType;
import com.runninghi.runninghibackv2.domain.enumtype.TargetPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GetAllAlarmResponse(
        @Schema(description = "알림 식별 값", example = "1")
        Long alarmId,
        @Schema(description = "알림 제목", example = "버전 업데이트 v1.2.1")
        String title,
        @Schema(description = "관련 도메인", example = "Reply")
        AlarmType alarmType,
        @Schema(description = "이동해야 할 페이지", example = "Main")
        TargetPage targetPage,
        @Schema(description = "관련 도메인 식별 값", example = "30")
        Long targetId,
        @Schema(description = "알림 확인 여부", example = "false")
        boolean isRead,
        @Schema(description = "알림 생성 시간", example = "2024-06-01T00:00:00")
        LocalDateTime createDate
) {
        public static GetAllAlarmResponse ofAlarmEntity(Alarm alarm) {

                return new GetAllAlarmResponse(
                        alarm.getId(),
                        alarm.getTitle(),
                        alarm.getAlarmType(),
                        alarm.getTargetPage(),
                        alarm.getTargetId(),
                        alarm.isRead(),
                        alarm.getCreateDate()
                );

        }

}
