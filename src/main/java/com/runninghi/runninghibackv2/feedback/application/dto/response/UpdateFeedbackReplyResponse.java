package com.runninghi.runninghibackv2.feedback.application.dto.response;

import com.runninghi.runninghibackv2.feedback.domain.aggregate.entity.FeedbackCategory;

import java.time.LocalDateTime;

public record UpdateFeedbackReplyResponse(
        String title,
        String content,
        FeedbackCategory category,
        LocalDateTime createDate,
        LocalDateTime updateDate,
        boolean hasReply,
        String reply,
        String nickname
) {
    public static UpdateFeedbackReplyResponse create(String title, String content, FeedbackCategory category,
                                             LocalDateTime createDate, LocalDateTime updateDate, boolean hasReply,
                                             String reply, String nickname) {
        return new UpdateFeedbackReplyResponse(title, content, category, createDate, updateDate, hasReply, reply, nickname);
    }
}
