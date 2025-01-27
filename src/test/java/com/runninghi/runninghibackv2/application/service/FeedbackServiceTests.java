package com.runninghi.runninghibackv2.application.service;

import com.runninghi.runninghibackv2.application.dto.feedback.request.CreateFeedbackRequest;
import com.runninghi.runninghibackv2.application.dto.feedback.request.UpdateFeedbackReplyRequest;
import com.runninghi.runninghibackv2.application.dto.feedback.request.UpdateFeedbackRequest;
import com.runninghi.runninghibackv2.application.dto.feedback.response.*;
import com.runninghi.runninghibackv2.domain.enumtype.Role;
import com.runninghi.runninghibackv2.domain.entity.Feedback;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.enumtype.FeedbackCategory;
import com.runninghi.runninghibackv2.domain.repository.FeedbackRepository;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import com.runninghi.runninghibackv2.domain.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FeedbackServiceTests {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PostRepository postRepository;

    private Member testMember1;
    private Member testMember2;
    private Member testAdmin;
    private Feedback testFeedback;

    @BeforeEach
    @AfterEach
    void clear() {
        postRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // 테스트 멤버 생성 메서드
    @BeforeEach
    void setup() {
        // 테스트 멤버 생성 및 저장
        testMember1 = Member.builder()
                .nickname("testUser1")
                .role(Role.USER)
                .build();

        testMember2 = Member.builder()
                .nickname("testUser2")
                .role(Role.USER)
                .build();

        testAdmin = Member.builder()
                .nickname("testUser2")
                .role(Role.ADMIN)
                .build();

        testFeedback = Feedback.builder()
                .feedbackWriter(testMember1)
                .title("Old Title")
                .content("Old Content")
                .category(FeedbackCategory.INQUIRY)
                .hasReply(false)
                .build();

        memberRepository.saveAndFlush(testMember1);
        memberRepository.saveAndFlush(testMember2);
        memberRepository.saveAndFlush(testAdmin);
        feedbackRepository.saveAndFlush(testFeedback);
    }

    @Test
    @DisplayName("피드백 생성")
    void testCreateFeedback() throws BadRequestException {

        // 피드백 생성
        CreateFeedbackRequest request = new CreateFeedbackRequest("제목", "내용", FeedbackCategory.PROPOSAL);

        // 서비스 메서드 호출
        CreateFeedbackResponse response = feedbackService.createFeedback(request, testMember1.getMemberNo());

        // 생성된 피드백 확인
        assertNotNull(response);
        assertNotNull(response.feedbackNo());

        // 피드백을 조회하여 확인
        Optional<Feedback> savedFeedbackOptional = feedbackRepository.findById(response.feedbackNo());
        assertTrue(savedFeedbackOptional.isPresent());

        Feedback savedFeedback = savedFeedbackOptional.get();
        assertEquals(request.title(), savedFeedback.getTitle());
        assertEquals(request.content(), savedFeedback.getContent());
    }

    @Test
    @DisplayName("피드백 수정")
    void testUpdateFeedback() throws BadRequestException {
        // 새로운 제목과 내용으로 피드백 업데이트를 요청
        String newTitle = "New Title";
        String newContent = "New Content";
        FeedbackCategory newCategory = FeedbackCategory.INQUIRY;

        UpdateFeedbackRequest request = new UpdateFeedbackRequest(newTitle, newContent, newCategory);

        // 서비스 메서드 호출
        UpdateFeedbackResponse response = feedbackService.updateFeedback(request, testFeedback.getFeedbackNo(), testMember1.getMemberNo());

        // 응답 확인
        assertNotNull(response);
        assertNotNull(response.feedbackNo());

        // 업데이트된 피드백을 조회해서 확인
        Optional<Feedback> updatedFeedbackOptional = feedbackRepository.findById(response.feedbackNo());
        assertTrue(updatedFeedbackOptional.isPresent());

        Feedback updatedFeedback = updatedFeedbackOptional.get();
        assertEquals(newTitle, updatedFeedback.getTitle());
        assertEquals(newContent, updatedFeedback.getContent());
        assertEquals(FeedbackCategory.INQUIRY, updatedFeedback.getCategory());
        assertFalse(updatedFeedback.isHasReply());
    }

    @Test
    @DisplayName("본인이 작성한 특정 피드백 조회")
    void testGetFeedback() {
        // 서비스 메서드 호출
        GetFeedbackResponse response = feedbackService.getFeedback(testFeedback.getFeedbackNo(), testMember1.getMemberNo());

        // 응답 확인
        assertNotNull(response);
        assertEquals(testFeedback.getTitle(), response.title());
        assertEquals(testFeedback.getContent(), response.content());
        assertEquals(testFeedback.getCategory(), response.category());
        assertEquals(testFeedback.getCreateDate(), response.createDate());
        assertEquals(testFeedback.getUpdateDate(), response.updateDate());
        assertEquals(testFeedback.isHasReply(), response.hasReply());
        assertEquals(testFeedback.getReply(), response.reply());
        assertEquals(testMember1.getNickname(), response.nickname());

        // 작성자가 아닌 사람이 조회할 때 에러가 발생하는지 확인
        assertThrows(AccessDeniedException.class, () -> feedbackService.getFeedback(testFeedback.getFeedbackNo(), testMember2.getMemberNo()));

        // 존재하지 않는 피드백 번호를 사용하여 피드백을 조회할 때 예외가 발생하는지 확인
        assertThrows(EntityNotFoundException.class, () -> feedbackService.getFeedback(99999L, testMember1.getMemberNo()));
    }

    @Test
    @DisplayName("본인의 피드백 리스트 조회")
    void testGetFeedbackScroll() {
        // 서비스 메서드 호출
        Pageable pageable = PageRequest.of(0, 10);
        FeedbackPageResponse<GetFeedbackResponse> response = feedbackService.getFeedbackScroll(pageable, testMember1.getMemberNo());

        // 응답 확인
        assertNotNull(response);
        assertEquals(1, response.totalPages());
        assertEquals(testFeedback.getTitle(), response.content().get(0).title());
        assertEquals(testFeedback.getContent(), response.content().get(0).content());
        assertEquals(testFeedback.getCategory(), response.content().get(0).category());
        assertEquals(testFeedback.isHasReply(), response.content().get(0).hasReply());
        assertEquals(testFeedback.getReply(), response.content().get(0).reply());
        assertEquals(testMember1.getNickname(), response.content().get(0).nickname());
    }


    @Test
    @DisplayName("관리자가 특정 피드백 조회")
    void testGetFeedbackByAdmin() {
        // 관리자 번호, 피드백 번호 설정
        Long adminMemberNo = testAdmin.getMemberNo();
        Long testFeedbackNo = testFeedback.getFeedbackNo();

        assertDoesNotThrow(() -> {
            GetFeedbackResponse response = feedbackService.getFeedbackByAdmin(testFeedbackNo, adminMemberNo);
            assertNotNull(response);
            assertEquals(testFeedback.getTitle(), response.title());
            assertEquals(testFeedback.getContent(), response.content());
            assertEquals(testFeedback.getCategory(), response.category());
            assertEquals(testFeedback.getCreateDate(), response.createDate());
            assertEquals(testFeedback.getUpdateDate(), response.updateDate());
            assertEquals(testFeedback.isHasReply(), response.hasReply());
            assertEquals(testFeedback.getReply(), response.reply());
            assertEquals(testFeedback.getFeedbackWriter().getNickname(), response.nickname());
        });

        // 일반 사용자로 인증된 경우
        Long regularMemberNo = testMember2.getMemberNo();

        assertThrows(AccessDeniedException.class, () -> feedbackService.getFeedbackByAdmin(testFeedbackNo, regularMemberNo));
    }


    @Test
    @DisplayName("관리자가 피드백 전체 조회")
    void testGetFeedbackScrollByAdmin() {
        // 관리자로 인증된 경우
        Long adminMemberNo = testAdmin.getMemberNo();

        assertDoesNotThrow(() -> {
            FeedbackPageResponse<GetFeedbackResponse> feedbackPage = feedbackService.getFeedbackScrollByAdmin(PageRequest.of(0, 10), adminMemberNo);
            assertNotNull(feedbackPage);
            assertEquals(1, feedbackPage.totalPages()); // 페이지에 포함된 피드백의 총 개수가 1개인지 확인
        });

        // 일반 사용자로 인증된 경우
        Long regularMemberNo = testMember2.getMemberNo();
        assertThrows(AccessDeniedException.class, () -> feedbackService.getFeedbackScrollByAdmin(PageRequest.of(0, 10), regularMemberNo));
    }

    @Test
    @DisplayName("피드백 답변 작성")
    void testUpdateFeedbackReply() {
        // 멤버 번호, 피드백 번호 설정
        Long feedbackNo = testFeedback.getFeedbackNo();
        Long memberNo = testFeedback.getFeedbackWriter().getMemberNo();
        UpdateFeedbackReplyRequest request = new UpdateFeedbackReplyRequest("reply content");

        assertThrows(AccessDeniedException.class, () -> {
            UpdateFeedbackReplyResponse response = feedbackService.updateFeedbackReply(request, feedbackNo, memberNo);
            assertNotNull(response);
            assertEquals(testFeedback.getTitle(), response.title());
            assertEquals(testFeedback.getContent(), response.content());
            assertEquals(testFeedback.getCategory(), response.category());
            assertEquals(testFeedback.getCreateDate(), response.createDate());
            assertEquals(testFeedback.getUpdateDate(), response.updateDate());
            assertEquals(testFeedback.isHasReply(), response.hasReply());
            assertTrue(response.hasReply());
            assertEquals(testFeedback.getReply(), response.reply());
            assertEquals(testFeedback.getFeedbackWriter().getNickname(), response.nickname());
        });

        // 일반 사용자로 인증된 경우
        Long regularMemberNo = testMember2.getMemberNo();

        assertThrows(AccessDeniedException.class, () -> feedbackService.updateFeedbackReply(request, feedbackNo, regularMemberNo));
    }

    @Test
    @DisplayName("피드백 삭제")
    void testDeleteFeedback() {
        // 피드백 번호, 멤버 번호 설정
        Long feedbackNo = testFeedback.getFeedbackNo();
        Long memberNo = testMember1.getMemberNo();
        long feedbackCount = feedbackRepository.count();

        // 작성자가 아닌 사람이 삭제할 때 에러가 발생하는지 확인
        Long regularMemberNo = testMember2.getMemberNo();

        assertThrows(AccessDeniedException.class, () -> feedbackService.deleteFeedback(feedbackNo, regularMemberNo));

        // 삭제를 시도하고 반환된 응답 확인
        assertDoesNotThrow(() -> {
            DeleteFeedbackResponse response = feedbackService.deleteFeedback(feedbackNo, memberNo);
            assertNotNull(response);
            assertEquals(feedbackNo, response.feedbackNo());
            assertEquals(feedbackCount - 1, feedbackRepository.count());
        });
    }

}