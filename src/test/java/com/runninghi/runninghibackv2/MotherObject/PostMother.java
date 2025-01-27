package com.runninghi.runninghibackv2.MotherObject;


import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.Record;
import com.runninghi.runninghibackv2.domain.entity.vo.GpsDataVO;
import com.runninghi.runninghibackv2.domain.enumtype.Difficulty;
import com.runninghi.runninghibackv2.domain.enumtype.Role;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;
import java.util.Arrays;

public final class PostMother {

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    static Point point = geometryFactory.createPoint(new Coordinate(126.978,37.566));
    private static final GpsDataVO gpsDataVO = new GpsDataVO("도쿄", point, LocalDateTime.now(), 100f, 42000, 200, 300, Arrays.asList(100, 200, 300), Arrays.asList(50, 100, 150), Difficulty.EASY);


    public static Post createUserPost(Member member) {
        return Post.builder()
                .member(member)
                .postContent("post content for test")
                .role(Role.USER)
                .status(true)
                .gpsDataVO(gpsDataVO)
                .build();
    }

    public static Post createUserPostFalse(Member member) {
        return Post.builder()
                .member(member)
                .role(Role.USER)
                .status(false)
                .gpsDataVO(gpsDataVO)
                .build();
    }

    public static Post createAdminPost(Member member) {
        return Post.builder()
                .member(member)
                .postContent("post content for test")
                .role(Role.ADMIN)
                .status(true)
                .locationName("SEOUL")
                .build();
    }

}
