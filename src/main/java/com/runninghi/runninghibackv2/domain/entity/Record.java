package com.runninghi.runninghibackv2.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "TBL_RECORD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "MEMBER_NO")
    @Comment("회원")
    private Member member;

    @Column
    @Comment("달린 km")
    private Float distance;

    @Column
    @Comment("달린 시간")
    private int time;

    @Column
    @Comment("평균 페이스")
    private int meanPaceSec;

    @Column
    @Comment("소모 칼로리")
    private int kcal;

    @Column
    @Comment("날짜")
    private LocalDate date;





}