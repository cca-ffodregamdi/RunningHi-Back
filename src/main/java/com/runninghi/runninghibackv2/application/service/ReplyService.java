package com.runninghi.runninghibackv2.application.service;

import com.runninghi.runninghibackv2.application.dto.alarm.request.CreateAlarmRequest;
import com.runninghi.runninghibackv2.application.dto.reply.GetReplyList;
import com.runninghi.runninghibackv2.application.dto.reply.request.*;
import com.runninghi.runninghibackv2.application.dto.reply.response.CreateReplyResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetContentResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.GetReportedReplyResponse;
import com.runninghi.runninghibackv2.application.dto.reply.response.UpdateReplyResponse;
import com.runninghi.runninghibackv2.common.response.ErrorCode;
import com.runninghi.runninghibackv2.common.response.PageResultData;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.Reply;
import com.runninghi.runninghibackv2.domain.enumtype.AlarmType;
import com.runninghi.runninghibackv2.domain.enumtype.Role;
import com.runninghi.runninghibackv2.domain.enumtype.TargetPage;
import com.runninghi.runninghibackv2.domain.repository.*;
import com.runninghi.runninghibackv2.domain.service.ReplyChecker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final ReplyQueryRepository replyQueryRepository;
    private final ReplyChecker replyChecker;

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;

    private final AlarmService alarmService;

    private static final String POST_REPLY_FCM_TITLE = "회원님의 게시글에 댓글이 등록되었습니다.";

    /**
     * 게시글 조회 시 해당 게시글에 대한 댓글들 조회 메소드
     *
     * @param request 게시글 식별을 위한 키 값
     * @return 댓글들 리스트 ( 댓글 정보)
     */
    @Transactional(readOnly = true)
    public GetContentResponse<List<GetReplyList>> getReplyList(GetReplyListRequest request) {

        List<GetReplyList> replyList = replyQueryRepository.findAllByPostNo(request);
        replyList.stream()
                .filter(reply -> reply.getMemberNo().equals(request.getMemberNo()))
                .forEach(reply -> reply.setIsOwner(true));

        return new GetContentResponse<>(replyList);
    }


    /**
     * '내가 쓴 댓글들' 혹은 '특정 회원이 쓴 댓글들' 조회 메소드
     *
     * @param memberNo 작성자 식별을 위한 키 값
     * @param sort
     * @return 댓글들
     */
    @Transactional(readOnly = true)
    public GetContentResponse<List<GetReplyList>> getReplyListByMemberNo(Long memberNo, Sort sort) {

        List<GetReplyList> replyList = replyQueryRepository.findAllByMemberNo(memberNo, sort);
        replyList.stream()
                .filter(i -> i.getMemberNo().equals(memberNo))
                .forEach(i -> i.setIsOwner(true));
        return new GetContentResponse<>(replyList);
    }

    /**
     * 댓글 작성 메소드
     *
     * @param request 댓글 작성에 필요한 정보
     * @return 댓글 번호, 작성자 닉네임, 게시글 번호, 댓글 내용, 삭제 여부, 생성 일, 수정 일
     */
    @Transactional
    public CreateReplyResponse createReply(CreateReplyRequest request, Long memberNo) {

        Member member = memberRepository.findByMemberNo(memberNo);
        Post post = postRepository.findById(request.postNo())
                .orElseThrow(EntityNotFoundException::new);

        Reply reply = Reply.builder()
                .member(member)
                .post(post)
                .replyContent(request.replyContent())
                .build();

        Reply savedReply = replyRepository.save(reply);

        // 게시물 작성자에게 푸쉬 알림
        Map<String, String> additionalData = new HashMap<>(Map.of(
                "replyNo", String.valueOf(savedReply.getReplyNo())
        ));
        CreateAlarmRequest alarmRequest = CreateAlarmRequest.builder()
                .title(POST_REPLY_FCM_TITLE)
                .targetMemberNo(savedReply.getPost().getMember().getMemberNo())
                .alarmType(AlarmType.REPLY)
                .targetPage(TargetPage.POST)
                .targetId(savedReply.getPost().getPostNo())
                .additionalData(additionalData)
                .fcmToken(savedReply.getMember().getFcmToken())
                .build();
        alarmService.createPushAlarm(alarmRequest);

        return new CreateReplyResponse(likeRepository.countByPost_PostNo(post.getPostNo()));
    }

    /**
     * 댓글 수정 메소드
     *
     * @param request 회원 식별을 위한 키 값, 회원 역할, 수정할 댓글 키 값, 수정할 댓글 내용
     * @return 댓글 번호, 작성자 닉네임, 게시글 번호, 댓글 내용, 삭제 여부, 생성 일, 수정 일
     */
    @Transactional
    public UpdateReplyResponse updateReply(UpdateReplyRequest request) {

        Reply reply = findReplyByReplyNo(request.getReplyNo());
        checkWriterOrAdmin(request.getMemberNo(), request.getRole(), reply);
        reply.update(request.getReplyContent());

        return UpdateReplyResponse.fromEntity(reply);
    }

    /**
     * 댓글 삭제 메소드 - 댓글 엔티티의 isDeleted의 상태를 'true' 값으로 변경
     *
     * @param request {replyNo, role, memberNo}
     */
    @Transactional
    public void deleteReply(DeleteReplyRequest request) {  // 프론트에서 role 같이 받을 수 있는 지 확인 필요

        Reply reply = findReplyByReplyNo(request.replyNo());
        checkWriterOrAdmin(request.memberNo(), request.role(), reply);
        reply.delete();
    }

    /**
     * 댓글 삭제 메소드 - 댓글 엔티티의 isDeleted의 상태를 'true' 값으로 변경
     *
     * @param replyNo
     */
    @Transactional
    public void deleteReplyById(Long replyNo) {

        Reply reply = findReplyByReplyNo(replyNo);
        reply.delete();
    }

    /**
     * 신고 횟수를 올리는 메소드입니다.
     *
     * @param replyNo
     */
    @Transactional
    public void plusReportedCount(Long replyNo) {

        Reply reply = findReplyByReplyNo(replyNo);
        reply.addReportedCount();
    }

    /**
     * 신고 횟수를 초기화하는 메소드입니다.
     *
     * @param replyNo
     */
    public void resetReportedCount(Long replyNo) {

        Reply reply = findReplyByReplyNo(replyNo);
        reply.resetReportedCount();
    }

    @Transactional(readOnly = true)
    public PageResultData<GetReportedReplyResponse> getReportedReplyList(GetReportedReplyRequest request) {
        replyChecker.checkSearchValid(request.search());
        return replyQueryRepository.findAllReportedByPageableAndSearch(request);
    }

    private Reply findReplyByReplyNo(Long replyNo) {

        return replyRepository.findById(replyNo)
                .orElseThrow(EntityNotFoundException::new);
    }

    private void checkWriterOrAdmin(Long memberNo, Role role, Reply reply) {

        boolean checkResult =
                replyChecker.memberCheck(memberNo, role, reply.getMember().getMemberNo());

        if (!checkResult) throw new AccessDeniedException(ErrorCode.ACCESS_DENIED.getMessage());

    }

}


