package com.runninghi.runninghibackv2.infrastructure.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.runninghi.runninghibackv2.application.dto.reply.request.GetReplyListByMemberRequest;
import com.runninghi.runninghibackv2.application.dto.reply.request.GetReplyListRequest;
import com.runninghi.runninghibackv2.application.dto.reply.request.GetReportedReplyRequest;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetReplyListResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetReportedReplyResponse;
import com.runninghi.runninghibackv2.common.response.PageResultData;
import com.runninghi.runninghibackv2.domain.enumtype.ProcessingStatus;
import com.runninghi.runninghibackv2.domain.repository.ReplyQueryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.runninghi.runninghibackv2.domain.entity.QMember.member;
import static com.runninghi.runninghibackv2.domain.entity.QPost.post;
import static com.runninghi.runninghibackv2.domain.entity.QReply.reply;
import static com.runninghi.runninghibackv2.domain.entity.QReplyReport.replyReport;

@Repository
@RequiredArgsConstructor
public class ReplyQueryRepositoryImpl implements ReplyQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private static final int REPORTED_COUNT = 1;

    @Override
    public PageResultData<GetReportedReplyResponse> findAllReportedByPageableAndSearch(GetReportedReplyRequest request) {

        Long count = getReportedCount(request);
        if (count < 1) return null;
        List<GetReportedReplyResponse> content = getReportedReplyList(request);

        return new PageResultData<>(content, request.pageable(), count);
    }

    @Override
    public PageResultData<GetReplyListResponse> findAllByPostNo(GetReplyListRequest request) {

        Long count = getCountByPostNo(request);
        if (count < 1) throw new EntityNotFoundException();
        List<GetReplyListResponse> content = getReplyListByPostNo(request);

        return new PageResultData<>(content, request.getPageable(), count);
    }

    @Override
    public PageResultData<GetReplyListResponse> findAllByMemberNo(GetReplyListByMemberRequest request) {

        Long count = getCountByMemberNo(request);
        if (count < 1) throw new EntityNotFoundException();
        List<GetReplyListResponse> content = getReplyListByMemberNo(request);

        return new PageResultData<>(content, request.getPageable(), count);
    }

    private Long getCountByMemberNo(GetReplyListByMemberRequest request) {
        return jpaQueryFactory
                .select(reply.replyNo.count())
                .from(reply)
                .where(
                        reply.writer.memberNo.eq(request.getMemberNo()),
                        reply.isDeleted.eq(false))
                .fetchOne();
    }

    private Long getCountByPostNo(GetReplyListRequest request) {

        return jpaQueryFactory
                .select(reply.replyNo.count())
                .from(reply)
                .where(
                        reply.post.postNo.eq(request.getPostNo()),
                        reply.isDeleted.eq(false))
                .fetchOne();

    }

    private Long getReportedCount(GetReportedReplyRequest request) {
        return jpaQueryFactory
                .select(reply.replyNo.count())
                .from(reply)
                .join(reply.writer, member)
                .join(reply.reportList, replyReport)
                .where(
                        likeNickname(request.search()),
                        eqReportStatus(request.reportStatus()),
                        reply.reportedCount.goe(REPORTED_COUNT))
                .fetchOne();

    }

    private List<GetReportedReplyResponse> getReportedReplyList (GetReportedReplyRequest request) {
        // request.reportStatus로 정렬
        return jpaQueryFactory
                .select(Projections.constructor(GetReportedReplyResponse.class,
                        reply.replyNo,
                        member.nickname,
                        reply.post.postNo,
                        reply.replyContent,
                        reply.reportedCount,
                        reply.isDeleted,
                        reply.createDate,
                        reply.updateDate
                ))
                .from(reply)
                .join(reply.writer, member)
                .join(reply.reportList, replyReport)
                .where(likeNickname(request.search()),
                        eqReportStatus(request.reportStatus()),
                        reply.reportedCount.goe(REPORTED_COUNT))
                .orderBy(
                        getOrderSpecifierList(request.pageable().getSort())
                                .toArray(OrderSpecifier[]::new))
                .offset(request.offset())
                .limit( request.pageable().getPageSize())
                .fetch();

    }

    private List<GetReplyListResponse> getReplyListByPostNo(GetReplyListRequest request) {
        return jpaQueryFactory
                .select(Projections.constructor(GetReplyListResponse.class,
                        reply.replyNo,
                        member.memberNo,
                        member.nickname,
                        reply.post.postNo,
                        reply.replyContent,
                        reply.reportedCount,
                        reply.isDeleted,
                        reply.createDate,
                        reply.updateDate))
                .from(reply)
                .join(reply.post, post)
                .join(reply.writer, member)
                .where(
                        reply.post.postNo.eq(request.getPostNo()),
                        reply.replyNo.loe(request.getReplyNo()),
                        reply.isDeleted.eq(false))
                .orderBy(reply.replyNo.desc())
                .limit(request.getSize())
                .fetch();
    }

    private List<GetReplyListResponse> getReplyListByMemberNo(GetReplyListByMemberRequest request) {
        return jpaQueryFactory
                .select(Projections.constructor(GetReplyListResponse.class,
                        reply.replyNo,
                        member.memberNo,
                        member.nickname,
                        reply.post.postNo,
                        reply.replyContent,
                        reply.reportedCount,
                        reply.isDeleted,
                        reply.createDate,
                        reply.updateDate))
                .from(reply)
                .join(reply.writer, member)
                .where(
                        reply.replyNo.loe(request.getReplyNo()),
                        reply.isDeleted.eq(false))
                .orderBy(reply.replyNo.desc())
                .limit(request.getSize())
                .fetch();
    }


    private BooleanExpression likeNickname (String search) {
        if (!StringUtils.hasText(search)) return null;   // space bar까지 막아줌
        return member.nickname.like("%" + search + "%");
    }

    private BooleanExpression eqReportStatus (ProcessingStatus reportStatus) {

        if (reportStatus == null || !StringUtils.hasText(reportStatus.name())) return  null;

        return replyReport.status.eq(reportStatus);
    }

    private List<OrderSpecifier<?>> getOrderSpecifierList (Sort sort) {

        List<OrderSpecifier<?>> orderList = new ArrayList<>();
        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty();
            Path<Object> target = Expressions.path(Object.class, reply,  property);
            orderList.add(new OrderSpecifier(direction, target));
        });

        return orderList;
    }

}