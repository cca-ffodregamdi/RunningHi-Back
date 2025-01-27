package com.runninghi.runninghibackv2.common.schedule;

import com.runninghi.runninghibackv2.domain.entity.*;
import com.runninghi.runninghibackv2.domain.entity.vo.BookmarkId;
import com.runninghi.runninghibackv2.domain.entity.vo.GpsDataVO;
import com.runninghi.runninghibackv2.domain.entity.vo.RunDataVO;
import com.runninghi.runninghibackv2.domain.enumtype.*;
import com.runninghi.runninghibackv2.domain.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@Transactional
class MemberCleanupBatchTests {

    @Autowired
    private MemberCleanupBatch memberCleanupBatch;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PostReportRepository postReportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReplyReportRepository replyReportRepository;

    @Autowired
    private ReplyRepository replyRepository;

    private LocalDateTime dateTime;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    Point point = geometryFactory.createPoint(new Coordinate(126.978,37.566));

    @BeforeEach
    @AfterEach
    public void tearDown() {
        memberRepository.deleteAllInBatch();
        alarmRepository.deleteAllInBatch();
        bookmarkRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        postReportRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        replyReportRepository.deleteAllInBatch();
        replyRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        List<Member> members = new ArrayList<>();
        dateTime = LocalDateTime.now().minusDays(31);

        Member member1 = Member.builder()
                .deactivateDate(dateTime)
                .alarmConsent(true)
                .name("kakaoName")
                .isActive(false)
                .runDataVO(new RunDataVO(1000,0.0,2,1))
                .build();
        members.add(member1);

        Member member2 = Member.builder()
                .deactivateDate(LocalDateTime.now().minusDays(15))
                .alarmConsent(true)
                .name("nyam")
                .isActive(true)
                .runDataVO(new RunDataVO(1000,0.0,2,1))
                .build();
        members.add(member2);

        memberRepository.saveAllAndFlush(members);


        List<Alarm> alarms = new ArrayList<>();

        Alarm alarm1 = Alarm.builder()
                .alarmType(AlarmType.CHALLENGE)
                .member(member1)
                .title("테스트 알림 1")
                .targetPage(TargetPage.CHALLENGE)
                .targetId(1L)
                .build();
        alarms.add(alarm1);

        Alarm alarm2 = Alarm.builder()
                .alarmType(AlarmType.FEEDBACK)
                .member(member1)
                .title("테스트 알림 2")
                .targetPage(TargetPage.CHALLENGE)
                .targetId(2L)
                .readDate(LocalDateTime.now().minusDays(1))
                .build();
        alarms.add(alarm2);

        Alarm alarm3 = Alarm.builder()
                .alarmType(AlarmType.REPLY)
                .member(member2)
                .title("테스트 알림 3")
                .targetPage(TargetPage.POST)
                .targetId(3L)
                .readDate(LocalDateTime.now().minusDays(1))
                .build();
        alarms.add(alarm3);

        alarmRepository.saveAllAndFlush(alarms);

        List<Post> posts = new ArrayList<>();

        Post post1 = Post.builder()
                .member(member1)
                .postContent("첫 번째 게시글 내용입니다.")
                .role(Role.USER)
                .locationName("서울")
                .gpsDataVO(new GpsDataVO("도쿄", point, LocalDateTime.now(), 100f, 42000, 200, 300, Arrays.asList(100, 200, 300), Arrays.asList(50, 100, 150), Difficulty.EASY))
                .build();
        posts.add(post1);

        // 남아있는 게시물
        Post post2 = Post.builder()
                .member(member2)
                .postContent("두 번째 게시글 내용입니다. : 남아있는 게시글 입니다.")
                .role(Role.ADMIN)
                .locationName("부산")
                .gpsDataVO(new GpsDataVO("도쿄", point, LocalDateTime.now(), 100f, 42000, 200, 300, Arrays.asList(100, 200, 300), Arrays.asList(50, 100, 150), Difficulty.EASY))
                .build();
        posts.add(post2);

        Post post3 = Post.builder()
                .member(member1)
                .postContent("세 번째 게시글 내용입니다.")
                .role(Role.USER)
                .locationName("대구")
                .gpsDataVO(new GpsDataVO("도쿄", point, LocalDateTime.now(), 100f, 42000, 200, 300, Arrays.asList(100, 200, 300), Arrays.asList(50, 100, 150), Difficulty.EASY))
                .build();
        posts.add(post3);

        postRepository.saveAllAndFlush(posts);


        List<Bookmark> bookmarks = new ArrayList<>();

        Bookmark bookmark1 = Bookmark.builder()
                .bookmarkId(new BookmarkId(1L, 1L))
                .member(member1)
                .post(post1)
                .build();
        bookmarks.add(bookmark1);

        Bookmark bookmark2 = Bookmark.builder()
                .bookmarkId(new BookmarkId(1L, 2L))
                .member(member1)
                .post(post2)
                .build();
        bookmarks.add(bookmark2);

        Bookmark bookmark3 = Bookmark.builder()
                .bookmarkId(new BookmarkId(2L, 3L))
                .member(member2)
                .post(post3)
                .build();
        bookmarks.add(bookmark3);

        // 남아있는 북마크
        Bookmark bookmark4 = Bookmark.builder()
                .bookmarkId(new BookmarkId(2L, 2L))
                .member(member2)
                .post(post2)
                .build();
        bookmarks.add(bookmark4);

        bookmarkRepository.saveAllAndFlush(bookmarks);


        List<Reply> replies = new ArrayList<>();

        Reply reply1 = Reply.builder()
                .member(member1)
                .post(post1)
                .replyContent("첫 번째 댓글 내용입니다. : member1")
                .reportedCount(0)
                .isDeleted(false)
                .build();
        replies.add(reply1);

        // 남아있는 댓글
        Reply reply2 = Reply.builder()
                .member(member2)
                .post(post2)
                .replyContent("두 번째 댓글 내용입니다. : 남아있는 댓글입니다.")
                .reportedCount(0)
                .isDeleted(false)
                .build();
        replies.add(reply2);

        Reply reply3 = Reply.builder()
                .member(member2)
                .post(post1)
                .replyContent("첫 번째 댓글 내용입니다. : member2")
                .reportedCount(0)
                .isDeleted(false)
                .build();
        replies.add(reply3);

        replyRepository.saveAllAndFlush(replies);


        List<ReplyReport> replyReports = new ArrayList<>();

        // 남아있는 댓글 신고
        ReplyReport report1 = ReplyReport.builder()
                .category(ReportCategory.SPAM)
                .content("스팸 댓글입니다.")
                .status(ProcessingStatus.INPROGRESS)
                .reporter(member1)
                .reportedReply(reply2)
                .replyContent(reply2.getReplyContent())
                .build();
        replyReports.add(report1);

        ReplyReport report2 = ReplyReport.builder()
                .category(ReportCategory.ILLEGAL)
                .content("부적절한 내용이 포함된 댓글입니다.")
                .status(ProcessingStatus.INPROGRESS)
                .reporter(member2)
                .reportedReply(reply1)
                .replyContent(reply1.getReplyContent())
                .build();
        replyReports.add(report2);

        replyReportRepository.saveAllAndFlush(replyReports);


        List<PostReport> postReports = new ArrayList<>();

        PostReport postReport1 = PostReport.builder()
                .category(ReportCategory.SPAM)
                .content("스팸 게시글입니다.")
                .status(ProcessingStatus.INPROGRESS)
                .reporter(member2)
                .reportedPost(post1)
                .postContent(post1.getPostContent())
                .build();
        postReports.add(postReport1);

        // 남아있는 게시물 신고
        PostReport postReport2 = PostReport.builder()
                .category(ReportCategory.ILLEGAL)
                .content("부적절한 내용이 포함된 게시글입니다.")
                .status(ProcessingStatus.INPROGRESS)
                .reporter(member1)
                .reportedPost(post2)
                .postContent(post2.getPostContent())
                .build();
        postReports.add(postReport2);

        // 남아있는 게시물 신고
        PostReport postReport3 = PostReport.builder()
                .category(ReportCategory.ILLEGAL)
                .content("부적절한 내용이 포함된 게시글입니다.")
                .status(ProcessingStatus.INPROGRESS)
                .reporter(member2)
                .reportedPost(post2)
                .postContent(post2.getPostContent())
                .build();
        postReports.add(postReport3);

        postReportRepository.saveAllAndFlush(postReports);

        List<Feedback> feedbacks = new ArrayList<>();

        Feedback feedback1 = Feedback.builder()
                .title("첫 번째 피드백")
                .content("첫 번째 피드백 내용입니다.")
                .hasReply(false)
                .reply(null)
                .category(FeedbackCategory.PROPOSAL)
                .feedbackWriter(member1)
                .build();
        feedbacks.add(feedback1);

        Feedback feedback2 = Feedback.builder()
                .title("두 번째 피드백")
                .content("두 번째 피드백 내용입니다.")
                .hasReply(true)
                .reply("두 번째 피드백에 대한 답변입니다.")
                .category(FeedbackCategory.INQUIRY)
                .feedbackWriter(member2)
                .build();
        feedbacks.add(feedback2);

        feedbackRepository.saveAllAndFlush(feedbacks);
    }

//    @Test
//    @DisplayName("회원 탈퇴 : Scheduling 테스트")
//    void cleanupDeactivateMemberTest() {
//        int beforePost = postRepository.findAll().size();
//        int beforeAlarm = alarmRepository.findAll().size();
//        int beforeBookmark = bookmarkRepository.findAll().size();
//        int beforeReply = replyRepository.findAll().size();
//        int beforeReplyReport = replyReportRepository.findAll().size();
//        int beforePostReport = postReportRepository.findAll().size();
//        int beforePostKeyword = postKeywordRepository.findAll().size();
//        int beforeFeedback = feedbackRepository.findAll().size();
//
//        List<Member> deactivatedMembers = memberRepository.findAllByDeactivateDate(dateTime);
//
//        if (!deactivatedMembers.isEmpty()) {
//            // cleanupDeactivateMember() 메서드 실행
//            CompletableFuture<Void> cleanupFuture = CompletableFuture.runAsync(
//                    () -> memberCleanupBatch.cleanupDeactivateMember()
//            );
//
//            // cleanupDeactivateMember() 메서드의 비동기 작업 완료 대기
//            cleanupFuture.join();
//
//            // cleanupFuture가 완료된 후에 실행되는 코드
//            List<Post> afterPosts = postRepository.findAll();
//            List<Alarm> afterAlarms = alarmRepository.findAll();
//            List<Bookmark> afterBookmarks = bookmarkRepository.findAll();
//            List<Reply> afterReplies = replyRepository.findAll();
//            List<ReplyReport> afterReplyReport = replyReportRepository.findAll();
//            List<PostReport> afterPostReport = postReportRepository.findAll();
//            List<PostKeyword> afterPostKeyword = postKeywordRepository.findAll();
//            List<Feedback> afterFeedback = feedbackRepository.findAll();
//
//            // 결과 확인 및 검증하는 코드
//            assertEquals(3, beforePost);
//            assertEquals(3, beforeAlarm);
//            assertEquals(4, beforeBookmark);
//            assertEquals(3, beforeReply);
//            assertEquals(2, beforeReplyReport);
//            assertEquals(3, beforePostReport);
//            assertEquals(2, beforePostKeyword);
//            assertEquals(2, beforeFeedback);
//            assertEquals(1, deactivatedMembers.size());
//            assertEquals(1, afterPosts.size());
//            assertEquals(1, afterAlarms.size());
//            assertEquals(1, afterBookmarks.size());
//            assertEquals(1, afterReplies.size());
//            assertEquals(2, afterPostReport.size());
//            assertEquals(1, afterPostKeyword.size());
//            assertEquals(1, afterFeedback.size());
//            assertEquals(0, afterReplyReport.size());
//        }
//    }

}