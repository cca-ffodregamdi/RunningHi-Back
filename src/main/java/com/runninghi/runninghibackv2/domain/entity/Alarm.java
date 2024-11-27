package com.runninghi.runninghibackv2.domain.entity;

import com.runninghi.runninghibackv2.domain.enumtype.AlarmType;
import com.runninghi.runninghibackv2.domain.enumtype.TargetPage;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_alarm")
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_no")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "MEMBER_NO")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Comment("알림 대상자")
    private Member member;

    @Column(name = "title", length = 100)
    @Comment(value = "알림 제목")
    private String title;

//    @Column(name = "content", nullable = false, length = 500)
//    @Comment(value = "알림 내용")
//    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = true)
    @Comment(value = "알림 타입")
    private AlarmType alarmType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_page", nullable = false)
    @Comment(value = "이동해야 할 페이지")
    private TargetPage targetPage;

    @Column(name = "target_id")
    @Comment(value = "관련 식별 값")
    private Long targetId;

    @ElementCollection
    @CollectionTable(name = "tbl_alarm_additional_data", joinColumns = @JoinColumn(name = "alarm_no"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private Map<String, String> additionalData;

    @Column(name = "is_read", nullable = false)
    @ColumnDefault(value = "false")
    @Comment(value = "확인 여부")
    private boolean isRead;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    @Comment(value = "알림 생성 시간")
    private LocalDateTime createDate;

    @Column(name = "read_date", updatable = false)
    @Comment(value = "알림 확인 시간")
    private LocalDateTime readDate;

    @Builder
    public Alarm(Long id, Member member, String title, AlarmType alarmType, TargetPage targetPage, Long targetId, Map<String, String> additionalData, LocalDateTime readDate) {
        this.id = id;
        this.member = member;
        this.title = title;
        this.alarmType = alarmType;
        this.targetPage = targetPage;
        this.targetId = targetId;
        this.additionalData = additionalData;
        this.isRead = false;
        this.createDate = LocalDateTime.now();
        this.readDate = LocalDateTime.now();
    }

    public void readAlarm() {
        this.isRead = true;
        this.readDate = LocalDateTime.now();
    }

}
