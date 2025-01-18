package com.runninghi.runninghibackv2.MotherObject;

import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.Record;

import java.time.LocalDate;

public class RecordMother {
    public static Record createUserRecord(Post post, LocalDate date) {
        return Record.builder()
                .date(date)
                .targetNo(post.getPostNo())
                .member(post.getMember())
                .distance(post.getGpsDataVO().getDistance())
                .kcal(post.getGpsDataVO().getKcal())
                .time(post.getGpsDataVO().getTime())
                .meanPace(post.getGpsDataVO().getMeanPace())
                .build();
    }

}
