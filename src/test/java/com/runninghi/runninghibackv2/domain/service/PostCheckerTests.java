package com.runninghi.runninghibackv2.domain.service;

import com.runninghi.runninghibackv2.MotherObject.MemberMother;
import com.runninghi.runninghibackv2.MotherObject.PostMother;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.repository.BookmarkRepository;
import com.runninghi.runninghibackv2.domain.repository.LikeRepository;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import com.runninghi.runninghibackv2.domain.repository.PostRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PostCheckerTests {

    @Autowired
    private PostChecker postChecker;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MemberRepository memberRepository;

    private Member member1;
    private Member member2;
    private Post post;

    @BeforeEach
    void clear() {
        memberRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setup() {
        member1 = MemberMother.createUserMember("member1");
        member2 = MemberMother.createUserMember("member2");
        memberRepository.saveAndFlush(member1);
        memberRepository.saveAndFlush(member2);

        post = PostMother.createUserPost(member1);
        postRepository.saveAndFlush(post);
    }

    @Test
    @DisplayName("Checker: 게시글 작성 시 길이 0 예외처리")
    void checkPostValidationTest1() {
        String text = "";

        assertThatThrownBy(() -> {
            postChecker.checkPostValidation(text);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내용은 1글자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("Checker: 게시글 작성 시 공백 예외처리")
    void checkPostValidationTest2() {
        String text = "   ";

        assertThatThrownBy(() -> {
            postChecker.checkPostValidation(text);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내용은 1글자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("Checker: 본인 게시글 아닐 시 예외처리")
    void checkIsWriter() {
        assertThatThrownBy(() -> {
            postChecker.isWriter(member2.getMemberNo(), post.getMember().getMemberNo());
        })
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("권한이 없습니다.");
    }

    @Test
    @DisplayName("Checker: 본인 게시글 맞을 시 true")
    void checkIsOwnerTrueTest() {
        assertTrue(postChecker.isOwner(member1.getMemberNo(), post.getMember().getMemberNo()));
    }

    @Test
    @DisplayName("Checker: 본인 게시글 아닐 시 false")
    void checkIsOwnerFalseTest() {
        assertFalse(postChecker.isOwner(member2.getMemberNo(), post.getMember().getMemberNo()));
    }

}
