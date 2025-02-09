package com.runninghi.runninghibackv2.domain.service;

import com.runninghi.runninghibackv2.domain.enumtype.Role;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ReplyCheckerTests {

    @Autowired
    private ReplyChecker replyChecker;

    @Nested
    @DisplayName("작성자와 요청자의 권한 비교")
    class memberCheckTest {

        @Test
        @DisplayName("작성자 본인일 시 true로 판별하는 지 확인")
        void isWriter() {

            // given
            Long memberNo = 1L;
            Role roleUser = Role.USER;
            Long writerNo = 1L;

            // when
            boolean isWriter = replyChecker.memberCheck(memberNo, roleUser, writerNo);

            // then
            Assertions.assertThat(isWriter).isEqualTo(true);
        }

        @Test
        @DisplayName("요청자가 관리자일 시 true로 판별하는 지 확인")
        void isAdmin() {

            // given
            Long memberNo = 2L;
            Role roleAdmin = Role.ADMIN;
            Long writerNo = 1L;

            // when
            boolean isAdmin = replyChecker.memberCheck(memberNo, roleAdmin, writerNo);

            // then
            Assertions.assertThat(isAdmin).isEqualTo(true);
        }

        @Test
        @DisplayName("요청자가 다른 3자일 시 false로 판별하는 지 확인")
        void isWrongMember() {

            // given
            Long memberNo = 2L;
            Long writerNo = 1L;

            // when
            boolean wrongWriter = replyChecker.memberCheck(memberNo, Role.USER, writerNo);

            // then
            Assertions.assertThat(wrongWriter).isEqualTo(false);
        }

    }

    @Nested
    @DisplayName("검색어 Valid 체크 메소드 테스트")
    class checkSearchValidTest {

        @Test
        @DisplayName("특수 문자 미 포함 시 검사 통과하는 지 확인")
        void isNormalOperation() {

            // given
            String searchString = "러너01";

            // when & then
            Assertions.assertThatCode(
                    () -> replyChecker.checkSearchValid(searchString)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("특수 문자 포함 시 예외 발생하는 지 확인")
        void isThrownBySpecialSymbols() {

            // given
            String searchString = "러#너@3";

            // when & then
            Assertions.assertThatThrownBy(
                    () -> replyChecker.checkSearchValid(searchString)
            );
        }
    }

}