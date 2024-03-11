package com.runninghi.runninghibackv2.bookmark.application.service;

import com.runninghi.runninghibackv2.bookmark.application.dto.request.CreateBookmarkRequest;
import com.runninghi.runninghibackv2.bookmark.application.dto.response.BookmarkedPostListResponse;
import com.runninghi.runninghibackv2.bookmark.application.dto.response.CreateBookmarkResponse;
import com.runninghi.runninghibackv2.bookmark.domain.aggregate.entity.Bookmark;
import com.runninghi.runninghibackv2.bookmark.domain.aggregate.vo.BookmarkId;
import com.runninghi.runninghibackv2.bookmark.domain.repository.BookmarkRepository;
import com.runninghi.runninghibackv2.bookmark.domain.service.ApiBookmarkService;
import com.runninghi.runninghibackv2.member.domain.aggregate.entity.Member;
import com.runninghi.runninghibackv2.post.domain.aggregate.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private ApiBookmarkService apiBookmarkService;


    @Transactional(readOnly = true)
    public  List<BookmarkedPostListResponse> getBookmarkedPostList(Long memberNo) {
        List<Bookmark> bookmarkListResult = bookmarkRepository.findAllByBookmarkId_MemberNo(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 항목이 없습니다."));

        List<BookmarkedPostListResponse> bookmarkList =
                bookmarkListResult.stream().map(i -> BookmarkedPostListResponse.convertToDTO(i.getPost())).toList();
        return bookmarkList;
    }

    @Transactional
    public CreateBookmarkResponse createBookmark(CreateBookmarkRequest request) {

        BookmarkId bookmarkId = BookmarkId.builder()
                                        .memberNo(request.memberNo())
                                        .postNo(request.postNo())
                                        .build();

        Member member = apiBookmarkService.getMemberById(request.memberNo());
        Post post = apiBookmarkService.getPostById(request.postNo());


        Bookmark bookmark = Bookmark.builder()
                                .bookmarkId(bookmarkId)
                                .member(member)
                                .post(post)
                                .build();

        return CreateBookmarkResponse.convertToDTO(bookmarkRepository.save(bookmark));
    }

    @Transactional
    public void deleteBookmark(Long memberNo, Long postNo) {

        bookmarkRepository.deleteById(
                BookmarkId.builder()
                        .memberNo(memberNo)
                        .postNo(postNo)
                        .build()
        );
    }
}
