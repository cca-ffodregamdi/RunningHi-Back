package com.runninghi.runninghibackv2.domain.service;

import com.runninghi.runninghibackv2.common.exception.custom.ImageException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class ImageCheckerTests {

    @Autowired
    private ImageChecker imageChecker;

    @Test
    @DisplayName("이미지 파일인 지 확인 후 확장자 반환 확인")
    void checkImageFileTest() {

        // given
        String fileName = "test.jpg";

        // when
        String extension = imageChecker.checkImageFile(fileName);
        String expected = "jpg";

        // then
        Assertions.assertThat(extension)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("이미지 외 다른 파일을 업로드할 시에 예외 발생 확인")
    void checkImageFileTest_exception() {

        // given
        String fileName = "test.test";

        // when & then
        Assertions.assertThatThrownBy(() -> imageChecker.checkImageFile(fileName))
                .isInstanceOf(ImageException.UnSupportedImageTypeException.class);
    }

    @Test
    @DisplayName("이미지 파일이 전달되지 않았을 때 예외 발생 확인")
    void checkImageFileTest_exception2() {

        // given
        String fileName = null;
        String fileName2 = "   ";
        String fileName3 = "test";

        // when & then
        Assertions.assertThatCode(
                () -> {
                    imageChecker.checkImageFile(fileName);
                    imageChecker.checkImageFile(fileName2);
                    imageChecker.checkImageFile(fileName3);
                }

        ).isInstanceOf( ImageException.InvalidFileName.class);
    }

    @Test
    @DisplayName("이미지 개수 6개 이하일 때 메소드 통과 확인")
    void checkMaxLengthTest() {

        // given
        List<MultipartFile> multipartFiles = new ArrayList<>();
        multipartFiles.add(new MockMultipartFile("file1", "filename1.txt", "text/plain", "some xml".getBytes()));
        multipartFiles.add(new MockMultipartFile("file2", "filename2.txt", "text/plain", "some text".getBytes()));

        // when & then
        Assertions.assertThatCode(
                () -> imageChecker.checkMaxLength(multipartFiles)
        ).doesNotThrowAnyException();

    }

    @Test
    @DisplayName("이미지 개수가 6개 이상일 때 예외 처리 확인")
    void checkMaxLengthTest_exception() {

        // given
        List<MultipartFile> multipartFiles = new ArrayList<>();
        multipartFiles.add(new MockMultipartFile("file1", "filename1.txt", "text/plain", "some xml".getBytes()));
        multipartFiles.add(new MockMultipartFile("file2", "filename2.txt", "text/plain", "some text".getBytes()));
        multipartFiles.add(new MockMultipartFile("file3", "filename3.txt", "text/plain", "some text".getBytes()));
        multipartFiles.add(new MockMultipartFile("file4", "filename4.txt", "text/plain", "some text".getBytes()));
        multipartFiles.add(new MockMultipartFile("file5", "filename5.txt", "text/plain", "some text".getBytes()));
        multipartFiles.add(new MockMultipartFile("file6", "filename6.txt", "text/plain", "some text".getBytes()));
        multipartFiles.add(new MockMultipartFile("file7", "filename7.txt", "text/plain", "some text".getBytes()));

        // when & then
        Assertions.assertThatThrownBy(
                        () -> imageChecker.checkMaxLength(multipartFiles))
                .isInstanceOf(ImageException.InvalidImageLength.class);
    }

}