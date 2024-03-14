package com.runninghi.runninghibackv2.reply.application.dto.response;

import com.runninghi.runninghibackv2.reply.domain.aggregate.entity.Reply;

import java.time.LocalDateTime;

public record UpdateReplyResponse (
        Long replyNo,
        String memberName,
        Long postNo,
        String replyContent,
        boolean isDeleted,
        Long parentReplyNo,
        LocalDateTime createDate,
        LocalDateTime updateDate
)
{
    public static UpdateReplyResponse fromEntity (Reply reply) {
        return new UpdateReplyResponse(
                reply.getReplyNo(),
                reply.getWriter().getNickname(),
                reply.getPost().getPostNo(),
                reply.getReplyContent(),
                reply.isDeleted(),
                reply.getParent().getReplyNo(),
                reply.getCreateDate(),
                reply.getUpdateDate()
        );
    }
}
