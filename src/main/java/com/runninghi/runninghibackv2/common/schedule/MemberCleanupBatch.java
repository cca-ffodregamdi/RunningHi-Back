package com.runninghi.runninghibackv2.common.schedule;

import com.runninghi.runninghibackv2.common.exception.custom.SchedulingException;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.Reply;
import com.runninghi.runninghibackv2.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberCleanupBatch {

    private final MemberRepository memberRepository;
    private final AlarmRepository alarmRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FeedbackRepository feedbackRepository;
    private final PostReportRepository postReportRepository;
    private final PostRepository postRepository;
    private final ReplyReportRepository replyReportRepository;
    private final ReplyRepository replyRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupDeactivateMember() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Member> deactivateMembers = memberRepository.findAllByDeactivateDate(thirtyDaysAgo);

        if (!deactivateMembers.isEmpty()) {
            for (Member deactivateMember : deactivateMembers) {
                deleteRelatedAlarms(deactivateMember);
                deleteRelatedFeedbacks(deactivateMember);
                deleteRelatedBookmarks(deactivateMember);
                deleteRelatedPosts(deactivateMember);
                deleteRelatedReplies(deactivateMember);
                deactivateMember.cleanupDeactivateMemberData();
            }
        }
    }

    private void deleteRelatedAlarms(Member deactivateMember) {
        try {
            alarmRepository.deleteAllByMember_MemberNo(deactivateMember.getMemberNo());
        } catch (Exception e) {
            throw new SchedulingException("회원 탈퇴 : 알림 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteRelatedReplies(Member deactivateMember) {
        try {
            replyRepository.deleteAllByMember(deactivateMember);
        } catch (Exception e) {
            throw new SchedulingException("회원 탈퇴 : 댓글 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteRelatedBookmarks(Member deactivateMember) {
        try {
            bookmarkRepository.deleteAllByMember(deactivateMember);
        } catch (Exception e) {
            throw new SchedulingException("회원 탈퇴 : 북마크 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteRelatedFeedbacks(Member deactivateMember) {
        try {
            feedbackRepository.deleteAllByFeedbackWriter(deactivateMember);
        } catch (Exception e) {
            throw new SchedulingException("회원 탈퇴 : 피드백/건의사항 삭제 중 오류가 발생했습니다.", e);
        }
    }

    private void deleteRelatedPosts(Member deactivateMember) {
        try {
            List<Post> posts = postRepository.findAllByMember(deactivateMember);
            if (!posts.isEmpty()) {
                for (Post post : posts) {
                    // 관련된 신고 기록 삭제
                    List<Reply> replies = replyRepository.findAllByPost(post);
                    for (Reply reply : replies) { replyReportRepository.deleteAllByReportedReply(reply); }

                    // 키워드, 북마크, 댓글 등을 일괄 삭제
                    bookmarkRepository.deleteAllByPost(post);
                    replyRepository.deleteAllByPost(post);
                    replyReportRepository.deleteAllByReportedReply_Post(post);
                    postReportRepository.deleteAllByReportedPost(post);
                }

                // Reporter와 관련된 모든 신고 삭제
                replyReportRepository.deleteAllByReporter(deactivateMember);

                // 게시물 일괄 삭제
                postRepository.deleteAllInBatch(posts);
            }
        } catch (Exception e) {
            throw new SchedulingException("회원 탈퇴 : 게시물 삭제 중 오류가 발생했습니다.", e);
        }
    }


}