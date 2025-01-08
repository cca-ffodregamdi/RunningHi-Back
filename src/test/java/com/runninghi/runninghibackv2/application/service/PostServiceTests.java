package com.runninghi.runninghibackv2.application.service;

import com.runninghi.runninghibackv2.MotherObject.MemberMother;
import com.runninghi.runninghibackv2.MotherObject.PostMother;
import com.runninghi.runninghibackv2.application.dto.alarm.request.CreateAlarmRequest;
import com.runninghi.runninghibackv2.application.dto.post.request.CreatePostRequest;
import com.runninghi.runninghibackv2.application.dto.post.request.UpdatePostRequest;
import com.runninghi.runninghibackv2.application.dto.post.response.*;
import com.runninghi.runninghibackv2.domain.entity.*;
import com.runninghi.runninghibackv2.domain.entity.vo.BookmarkId;
import com.runninghi.runninghibackv2.domain.entity.vo.GpsDataVO;
import com.runninghi.runninghibackv2.domain.entity.vo.LikeId;
import com.runninghi.runninghibackv2.domain.repository.BookmarkRepository;
import com.runninghi.runninghibackv2.domain.repository.LikeRepository;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import com.runninghi.runninghibackv2.domain.repository.PostRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PostServiceTests {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private BookmarkRepository bookmarkRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private MemberRepository memberRepository;

    private Member member1;
    private Member member2;

    private String sampleData = """
            {"runInfo":{"runStartDate":"2023-08-05T06:36:15","location":"제주","distance":1.5,"time":3000,"kcal":100,"meanPace":1500,"difficulty":"EASY"},"sectionData":{"pace":[1000,2000],"kcal":[40,60]},"gpsData":[{"lon":126.655,"lat":33.4518,"time":"2023-08-05T06:36:15"},{"lon":126.656,"lat":33.4517,"time":"2023-08-05T06:36:17"},{"lon":126.656,"lat":33.4516,"time":"2023-08-05T06:36:21"}]}
    """;

    private MultipartFile mockFile = new MockMultipartFile("file", "run_data.txt", "text/plain", sampleData.getBytes());
    private List<Post> postList = new ArrayList<>();

    @BeforeEach
    void clear() {
        memberRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setup() {
        member1 = MemberMother.createUserMember("member1");
        member2 = MemberMother.createUserMember("member2");
        List<Member> memberList =  Arrays.asList(member1, member2);
        memberRepository.saveAllAndFlush(memberList);

        for(int i = 0 ; i < 15 ; i ++) {
            Post post = PostMother.createUserPost(member1);
            postRepository.saveAndFlush(post);
            postList.add(post);
        }

        Post post = PostMother.createUserPostFalse(member1);
        postRepository.saveAndFlush(post);
        postList.add(post);

        Like like = Like.builder()
                .likeId(new LikeId(member2.getMemberNo(), postList.get(0).getPostNo()))
                .post(postList.get(0))
                .member(member2)
                .build();
        likeRepository.saveAndFlush(like);

        Bookmark bookmark = Bookmark.builder()
                .bookmarkId(new BookmarkId(member2.getMemberNo(), postList.get(0).getPostNo()))
                .post(postList.get(0))
                .member(member2)
                .build();
        bookmarkRepository.saveAndFlush(bookmark);
    }

    @Test
    @DisplayName("최신순 전체 게시글 조회 : success")
    void testGetPostScrollByLatest() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetAllPostsResponse> postList1 = postService.getPostScroll(member1.getMemberNo(), pageable1, "latest").getContent();
        assertEquals(10, postList1.size());

        Pageable pageable2 = PageRequest.of(1 , 10);
        List<GetAllPostsResponse> postList2 = postService.getPostScroll(member1.getMemberNo(), pageable2, "latest").getContent();
        assertEquals(5, postList2.size());

        assertTrue(postList1.get(0).createDate().isAfter(postList1.get(1).createDate()));
    }

    @Test
    @DisplayName("북마크순 전체 게시글 조회 : success")
    void testGetPostScrollByRecommended() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetAllPostsResponse> postList1 = postService.getPostScroll(member1.getMemberNo(), pageable1, "recommended").getContent();
        assertEquals(10, postList1.size());

        Pageable pageable2 = PageRequest.of(1 , 10);
        List<GetAllPostsResponse> postList2 = postService.getPostScroll(member1.getMemberNo(), pageable2, "recommended").getContent();
        assertEquals(5, postList2.size());

        assertEquals(postList.get(0).getPostNo(), postList1.get(0).postNo());
    }

    @Test
    @DisplayName("좋아요순 전체 게시글 조회 : success")
    void testGetPostScrollByLike() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetAllPostsResponse> postList1 = postService.getPostScroll(member1.getMemberNo(), pageable1, "like").getContent();
        assertEquals(10, postList1.size());

        Pageable pageable2 = PageRequest.of(1 , 10);
        List<GetAllPostsResponse> postList2 = postService.getPostScroll(member1.getMemberNo(), pageable2, "like").getContent();
        assertEquals(5, postList2.size());

        assertTrue(postList1.get(0).likeCnt() > postList1.get(1).likeCnt());
        assertTrue(postList1.get(2).createDate().isAfter(postList1.get(3).createDate()));
    }

    @Test
    @DisplayName("게시글 상세 조회 : success")
    void testGetPostDetailByPostNo() {
        Post post = postList.get(0);
        GetPostResponse postResponse = postService.getPostDetailByPostNo(member1.getMemberNo(), postList.get(0).getPostNo());

        assertEquals(post.getPostContent(), postResponse.postContent());
        assertEquals(post.getGpsDataVO().getLocationName(), postResponse.locationName());
        assertEquals(1, postResponse.bookmarkCnt());
        assertEquals(1, postResponse.likeCnt());
    }

    @Test
    @DisplayName("나의 게시글 전체 조회 : success")
    void testGetMyPostsScroll() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetMyPostsResponse> myPostsResponses1 = postService.getMyPostsScroll(pageable1, member1.getMemberNo()).getContent();
        assertEquals(10, myPostsResponses1.size());

        Pageable pageable2 = PageRequest.of(1 , 10);
        List<GetMyPostsResponse> myPostsResponses2 = postService.getMyPostsScroll(pageable2, member1.getMemberNo()).getContent();
        assertEquals(5, myPostsResponses2.size());

        List<GetMyPostsResponse> myPostsResponses3 = postService.getMyPostsScroll(pageable1, member2.getMemberNo()).getContent();
        assertEquals(0, myPostsResponses3.size());
    }

    @Test
    @DisplayName("내가 좋아요 누른 전체 게시글 조회 : success")
    void testGetMyLikedPostsScroll() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetAllPostsResponse> myPostsResponses1 = postService.getMyLikedPosts(pageable1, member2.getMemberNo()).getContent();
        assertEquals(1, myPostsResponses1.size());
        assertEquals(postList.get(0).getPostNo(), myPostsResponses1.get(0).postNo());

        List<GetAllPostsResponse> myPostsResponses2 = postService.getMyLikedPosts(pageable1, member1.getMemberNo()).getContent();
        assertEquals(0, myPostsResponses2.size());
    }

    @Test
    @DisplayName("내가 북마크 저장한 전체 게시글 조회 : success")
    void testGetMyBookmarkedPostsScroll() {
        Pageable pageable1 = PageRequest.of(0 , 10);
        List<GetAllPostsResponse> myPostsResponses1 = postService.getMyBookmarkedPosts(pageable1, member2.getMemberNo()).getContent();
        assertEquals(1, myPostsResponses1.size());
        assertEquals(postList.get(0).getPostNo(), myPostsResponses1.get(0).postNo());

        List<GetAllPostsResponse> myPostsResponses2 = postService.getMyBookmarkedPosts(pageable1, member1.getMemberNo()).getContent();
        assertEquals(0, myPostsResponses2.size());
    }

    @Test  // mock 을 이용한 수정 필요합니다.
    @DisplayName("GPS 데이터 저장 : success (No exception thrown)")
    void testCreateRecord() {
        assertDoesNotThrow(() -> {
            CreateRecordResponse response = postService.createRecord(member1.getMemberNo(), mockFile);
            assertNotNull(response);

            Optional<Post> createdRecord = postRepository.findById(response.postNo());
            assertTrue(createdRecord.isPresent());
            assertFalse(createdRecord.get().getStatus());
        });
    }

    @Test
    @DisplayName("저장한 GPS 데이터 게시글로 생성 : success")
    void testCreatePost() {
        CreatePostRequest request = new CreatePostRequest(postList.get(15).getPostNo(), "Test Content","EASY", 0, "");
        Long createdPostNo = postService.createPost(member1.getMemberNo(), request).postNo();

        Optional<Post> createdPost = postRepository.findById(createdPostNo);
        assertTrue(createdPost.isPresent());
        assertTrue(createdPost.get().getStatus());
    }

    @Test
    @DisplayName("저장한 GPS 데이터 게시글로 공유 : 저장하지 않은 데이터에 대한 게시글 생성 예외처리")
    void testRecordDoesntExistException() {
        CreatePostRequest request = new CreatePostRequest(999L, "Test Content","EASY", 0, "");

        assertThrowsExactly(EntityNotFoundException.class, () -> {
            postService.createPost(member1.getMemberNo(), request);
        });
    }

    @Test
    @DisplayName("저장한 GPS 데이터 게시글로 공유 : 나의 데이터가 아닌 게시글 생성 예외처리")
    void testIsNotMyRecordException() {
        CreatePostRequest request = new CreatePostRequest(postList.get(15).getPostNo(), "Test Content","EASY", 0, "");

        assertThrowsExactly(AccessDeniedException.class, () -> {
            postService.createPost(member2.getMemberNo(), request);
        });
    }

    @Test
    @DisplayName("작성한 게시글 수정 : success")
    void testUpdatePost() {
        UpdatePostRequest request = new UpdatePostRequest("Update Content", 1, "");
        postService.updatePost(member1.getMemberNo(), postList.get(0).getPostNo(), request);

        assertNotNull(postList.get(0).getUpdateDate());
        assertEquals(postList.get(0).getPostContent(), request.postContent());
        assertEquals(postList.get(0).getMainDataType(), request.mainData());
    }

    @Test
    @DisplayName("작성한 게시글 수정 : 나의 데이터가 아닌 게시글 수정 예외처리")
    void testUpdateIsNotMyPostException() {
        UpdatePostRequest request = new UpdatePostRequest("Update Content", 1, "");

        assertThrowsExactly(AccessDeniedException.class, () -> {
            postService.updatePost(member2.getMemberNo(), postList.get(0).getPostNo(), request);
        });
    }

    @Test
    @DisplayName("작성한 게시글 삭제 : success")
    void testDeletePost() {
        //Record 함께 테스트
    }

    @Test
    @DisplayName("작성한 게시글 삭제 : 나의 데이터가 아닌 게시글 삭제 예외처리")
    void testDeleteIsNotMyPostException() {
        assertThrowsExactly(AccessDeniedException.class, () -> {
            postService.deletePost(member2.getMemberNo(), postList.get(0).getPostNo());
        });
    }
}
