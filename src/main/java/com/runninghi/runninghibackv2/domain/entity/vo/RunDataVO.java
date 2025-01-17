package com.runninghi.runninghibackv2.domain.entity.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.Comment;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class RunDataVO {

    @Column
    @Comment("누적 거리")
    private double totalDistance = 0.0;

    @Column
    @Comment("누적 칼로리")
    private double totalKcal = 0.0;

    @Column
    @Comment("다음 레벨에 필요한 거리")
    private double distanceToNextLevel = 2.0;

    @Column
    @Comment("누적거리에 따른 레벨")
    private int level = 1;

    @Builder
    public RunDataVO(double totalDistance, double totalKcal, int distanceToNextLevel, int level) {
        this.totalDistance = totalDistance;
        this.totalKcal = totalKcal;
        this.distanceToNextLevel = distanceToNextLevel;
        this.level = level;
    }

    // 누적 거리 업데이트
    public void updateTotalDistanceKcalAndLevel(double distance, double kcal) {
        this.totalDistance += distance;
        this.totalKcal += kcal;

        while (distance >= distanceToNextLevel) {
            distance -= distanceToNextLevel;
            checkAndLevelUp();
        }

        this.distanceToNextLevel -= distance;
    }

    // 레벨 업데이트를 확인하고 처리
    private void checkAndLevelUp() {
        this.level++;
        this.distanceToNextLevel = 2 * this.level;
    }

    public void cleanupDeactivateMemberRunData() {
        this.totalDistance = 0;
        this.totalKcal = 0;
        this.distanceToNextLevel = 2;
        this.level = 1;
    }

}

