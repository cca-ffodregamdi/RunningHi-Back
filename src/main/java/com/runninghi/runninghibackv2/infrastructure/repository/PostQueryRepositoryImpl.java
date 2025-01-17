package com.runninghi.runninghibackv2.infrastructure.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.runninghi.runninghibackv2.application.dto.post.response.GetAllPostsResponse;
import com.runninghi.runninghibackv2.application.dto.post.response.GetMyPostsResponse;
import com.runninghi.runninghibackv2.application.dto.post.response.GetPostResponse;
import com.runninghi.runninghibackv2.application.dto.post.response.GetRecordPostResponse;
import com.runninghi.runninghibackv2.common.response.PageResultData;
import com.runninghi.runninghibackv2.domain.entity.Image;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.QPost;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import com.runninghi.runninghibackv2.domain.repository.PostQueryRepository;
import com.runninghi.runninghibackv2.domain.repository.PostRepository;
import com.runninghi.runninghibackv2.domain.repository.ReplyQueryRepository;
import com.runninghi.runninghibackv2.domain.service.PostChecker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static com.runninghi.runninghibackv2.domain.entity.QBookmark.bookmark;
import static com.runninghi.runninghibackv2.domain.entity.QImage.image;
import static com.runninghi.runninghibackv2.domain.entity.QLike.like;
import static com.runninghi.runninghibackv2.domain.entity.QPost.post;
import static com.runninghi.runninghibackv2.domain.entity.QReply.reply;

@Repository
@RequiredArgsConstructor
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final PostRepository postRepository;
    private final ReplyQueryRepository replyQueryRepository;
    private final PostChecker postChecker;
    private final MemberRepository memberRepository;

//    private static final String DISTANCE_CONDITION = "ST_Distance_Sphere({0}, {1}) <= {2}";

    @Value("${image}")
    String nullUrl;

    @Override
    public PageResultData<GetMyPostsResponse> findMyPostsByPageable(Pageable pageable, Long memberNo) {
        long total = jpaQueryFactory.selectFrom(post)
                .where(post.member.memberNo.eq(memberNo)
                        .and(post.status.eq(true)))
                .fetchCount();

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .where(post.member.memberNo.eq(memberNo).and(post.status.eq(true)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(post.createDate.desc(), post.postNo.desc())
                .fetch();

        List<GetMyPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            return GetMyPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }


    @Override
    public GetPostResponse getPostDetailByPostNo(Long memberNo, Long postNo) {

        Post post = postRepository.findById(postNo)
                .orElseThrow(EntityNotFoundException::new);

        Boolean isWriter = postChecker.isOwner(memberNo, post.getMember().getMemberNo());

        String imageUrl = jpaQueryFactory
                .select(image.imageUrl)
                .from(image)
                .where(image.targetNo.eq(postNo))
                .fetchFirst();

        Long likeCnt = jpaQueryFactory.select(like.count())
                .from(like)
                .where(like.post.postNo.eq(post.getPostNo()))
                .fetchOne();

        Long bookmarkCnt = jpaQueryFactory
                .select(bookmark.count())
                .from(bookmark)
                .where(bookmark.post.postNo.eq(postNo))
                .fetchOne();

        Long replyCnt = replyQueryRepository.getCountByPostNo(postNo);

        Boolean isLiked = jpaQueryFactory.selectFrom(like)
                .where(
                        like.post.postNo.eq(post.getPostNo()),
                        like.member.memberNo.eq(memberNo))
                .fetchOne() != null;

        Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                .where(bookmark.member.memberNo.eq(memberNo)
                        .and(bookmark.post.postNo.eq(post.getPostNo())))
                .fetchFirst() != null;


        return GetPostResponse.from(post, imageUrl,likeCnt, bookmarkCnt, replyCnt, isWriter, isLiked, isBookmarked);
    }

    @Override
    public PageResultData<GetAllPostsResponse> findAllPostsByLatest(Long memberNo, Pageable pageable) {
        List<Post> posts;
        long total;

        Point referencePoint = memberRepository.findByMemberNo(memberNo).getGeometry();

        total = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(post.gpsDataVO.isNotNull())
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 10000
//                        ))
                )
                .fetchCount();

        posts = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(post.gpsDataVO.startPoint.isNotNull())
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 10000
//                        ))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(post.createDate.desc(), post.postNo.desc())
                .fetch();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.selectFrom(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;
            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }

    @Override
    public PageResultData<GetAllPostsResponse> findAllPostsByRecommended(Long memberNo, Pageable pageable) {
        List<Post> posts;
        long total;

        Point referencePoint = memberRepository.findByMemberNo(memberNo).getGeometry();

        total = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 10000
//                        ))
                )
                .fetchCount();

        posts = jpaQueryFactory.select(post)
                .from(post)
                .leftJoin(bookmark).on(bookmark.post.postNo.eq(post.postNo))
                .where(post.status.eq(true)
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 10000
//                        ))
                )
                .groupBy(post.postNo)
                .orderBy(
                    Expressions.numberTemplate(Long.class, "count({0})", bookmark.post.postNo).desc(),
                    post.postNo.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.selectFrom(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;
            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }

    @Override
    public PageResultData<GetAllPostsResponse> findAllPostsByLikeCnt(Long memberNo, Pageable pageable) {
        List<Post> posts;
        long total;

        Point referencePoint = memberRepository.findByMemberNo(memberNo).getGeometry();

        total = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 100
//                        ))
                )
                .fetchCount();

        posts = jpaQueryFactory.select(post)
                .from(post)
                .leftJoin(like).on(like.post.postNo.eq(post.postNo))
                .where(post.status.eq(true)
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 100
//                        ))
                )
                .groupBy(post.postNo)
                .orderBy(
                        Expressions.numberTemplate(Long.class, "count({0})", like.post.postNo).desc(),
                        post.postNo.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.selectFrom(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;
            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }

    @Override
    public PageResultData<GetAllPostsResponse> findAllPostsByDistance(Long memberNo, Pageable pageable) {
        List<Post> posts;
        long total;

        Point referencePoint = memberRepository.findByMemberNo(memberNo).getGeometry();

        total = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(post.gpsDataVO.isNotNull())
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 100
//                        ))
                )
                .fetchCount();

        posts = jpaQueryFactory.selectFrom(post)
                .where(post.status.eq(true)
//                        .and(post.gpsDataVO.startPoint.isNotNull())
//                        .and(Expressions.booleanTemplate(
//                                DISTANCE_CONDITION,
//                                post.gpsDataVO.startPoint, referencePoint, distance * 100
//                        ))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(Expressions.numberTemplate(Double.class, "ST_Distance({0}, {1})", post.gpsDataVO.startPoint, referencePoint).asc())
                .fetch();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.selectFrom(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;
            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }

    @Override
    public PageResultData<GetAllPostsResponse> findMyLikedPostsByPageable(Pageable pageable, Long memberNo) {
        long total;

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .join(like).on(post.postNo.eq(like.post.postNo))
                .where(like.member.memberNo.eq(memberNo))
                .orderBy(post.createDate.desc(), post.postNo.desc())
                .fetch();
        total = jpaQueryFactory.selectFrom(post)
                .join(like).on(post.postNo.eq(like.post.postNo))
                .where(like.member.memberNo.eq(memberNo))
                .fetchCount();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }


    @Override
    public PageResultData<GetAllPostsResponse> findMyBookmarkedPostsByPageable(Pageable pageable, Long memberNo) {
        long total;

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .join(bookmark).on(post.postNo.eq(bookmark.post.postNo))
                .where(bookmark.member.memberNo.eq(memberNo))
                .orderBy(post.createDate.desc(), post.postNo.desc())
                .fetch();

        total = jpaQueryFactory.selectFrom(post)
                .join(bookmark).on(post.postNo.eq(bookmark.post.postNo))
                .where(bookmark.member.memberNo.eq(memberNo))
                .fetchCount();

        List<GetAllPostsResponse> responses = posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;

            Long replyCnt = replyQueryRepository.getCountByPostNo(post.getPostNo());

            Long likeCnt = jpaQueryFactory.select(like.count())
                    .from(like)
                    .where(like.post.postNo.eq(post.getPostNo()))
                    .fetchOne();

            Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                    .where(bookmark.member.memberNo.eq(memberNo)
                            .and(bookmark.post.postNo.eq(post.getPostNo())))
                    .fetchFirst() != null;

            Boolean isLiked = jpaQueryFactory.selectFrom(like)
                    .where(
                            like.post.postNo.eq(post.getPostNo()),
                            like.member.memberNo.eq(memberNo))
                    .fetchOne() != null;

            Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);


            return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
        }).collect(Collectors.toList());

        return new PageResultData<>(responses, pageable, total);
    }


    @Override
    public List<GetRecordPostResponse> findWeeklyRecord(Long memberNo, LocalDate date) {

        LocalDateTime start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .where(post.createDate.between(start,end)
                    .and(post.member.memberNo.eq(memberNo)))
                .fetch();

        return posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : nullUrl;

            return GetRecordPostResponse.from(post, imageUrl);
        }).collect(Collectors.toList());
    }

    @Override
    public List<GetRecordPostResponse> findMonthlyRecord(Long memberNo, LocalDate date) {
        LocalDateTime start = date.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = date.withDayOfMonth(date.lengthOfMonth()).atTime(23, 59, 59);

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .where(post.createDate.between(start,end)
                        .and(post.member.memberNo.eq(memberNo)))
                .fetch();

        return posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : nullUrl;

            return GetRecordPostResponse.from(post, imageUrl);
        }).collect(Collectors.toList());
    }

    @Override
    public List<GetRecordPostResponse> findYearlyRecord(Long memberNo, LocalDate date) {
        int year = date.getYear();
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<Post> posts = jpaQueryFactory.select(post)
                .from(post)
                .where(post.createDate.between(start,end)
                        .and(post.member.memberNo.eq(memberNo)))
                .fetch();

        return posts.stream().map(post -> {
            Image mainImage = jpaQueryFactory.select(image)
                    .from(image)
                    .where(image.targetNo.eq(post.getPostNo()))
                    .limit(1)
                    .fetchOne();

            String imageUrl = mainImage != null ? mainImage.getImageUrl() : nullUrl;

            return GetRecordPostResponse.from(post, imageUrl);
        }).collect(Collectors.toList());
    }

    @Override
    public GetAllPostsResponse getPostByPostNo(Long memberNo, Long postNo) {
        Post post = jpaQueryFactory.selectFrom(QPost.post)
                .where(QPost.post.postNo.eq(postNo))
                .fetchOne();

        if (post == null) {
            return null; // 또는 적절한 예외 처리
        }

        Image mainImage = jpaQueryFactory.select(image)
                .from(image)
                .where(image.targetNo.eq(postNo))
                .limit(1)
                .fetchOne();

        String imageUrl = mainImage != null ? mainImage.getImageUrl() : null;

        Long replyCnt = replyQueryRepository.getCountByPostNo(postNo);

        Long likeCnt = jpaQueryFactory.select(like.count())
                .from(like)
                .where(like.post.postNo.eq(postNo))
                .fetchOne();

        Boolean isBookmarked = jpaQueryFactory.selectFrom(bookmark)
                .where(bookmark.member.memberNo.eq(memberNo)
                        .and(bookmark.post.postNo.eq(postNo)))
                .fetchFirst() != null;

        Boolean isLiked = jpaQueryFactory.selectFrom(like)
                .where(
                        like.post.postNo.eq(postNo),
                        like.member.memberNo.eq(memberNo))
                .fetchOne() != null;

        Boolean isWriter = post.getMember().getMemberNo().equals(memberNo);

        return GetAllPostsResponse.from(post, imageUrl, replyCnt, likeCnt, isBookmarked, isLiked, isWriter);
    }

}
