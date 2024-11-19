package com.runninghi.runninghibackv2.application.controller;

import com.runninghi.runninghibackv2.application.dto.bookmark.request.CreateBookmarkRequest;
import com.runninghi.runninghibackv2.application.dto.bookmark.response.CreateBookmarkResponse;
import com.runninghi.runninghibackv2.application.service.BookmarkService;
import com.runninghi.runninghibackv2.auth.jwt.JwtTokenProvider;
import com.runninghi.runninghibackv2.common.dto.AccessTokenInfo;
import com.runninghi.runninghibackv2.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/bookmark")
@Tag(name = "북마크 API", description = "게시글 북마크 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtTokenProvider jwtTokenProvider;

    private final static String POST_RESPONSE_MESSAGE = "북마크 생성 성공";
    private final static String DELETE_RESPONSE_MESSAGE = "북마크 취소 성공";

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "북마크 생성",
            description = "특정 게시물을 북마크합니다. <br /> 사용자 요청으로 '회원 번호'와 '게시글 번호'를 입력 받아 북마크 정보를 저장하고 저장 정보를 반환합니다.",
            responses = @ApiResponse(responseCode = "201", description = POST_RESPONSE_MESSAGE)
    )
    public ResponseEntity<ApiResult<CreateBookmarkResponse>> createBookmark (@Parameter(description = "사용자 인증을 위한 Bearer 토큰") @RequestHeader("Authorization") String bearerToken,
                                                                             @RequestBody(required = true) @Schema(description = "post 번호", example = "{\"postNo\" : 1}") Map<String, Long> body) {

        AccessTokenInfo accessTokenInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        CreateBookmarkRequest request = CreateBookmarkRequest.of(accessTokenInfo.memberNo(), body.get("postNo"));
        CreateBookmarkResponse response = bookmarkService.createBookmark(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(POST_RESPONSE_MESSAGE, response));
    }

    @DeleteMapping(value = "/{postNo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "북마크 취소",
            description = "특정 게시물의 북마크를 취소합니다.",
            responses = @ApiResponse(responseCode = "204", description = DELETE_RESPONSE_MESSAGE)
    )
    public ResponseEntity<ApiResult> deleteBookmark (@Parameter(description = "사용자 인증을 위한 Bearer 토큰") @RequestHeader("Authorization") String bearerToken,
                                                     @Parameter(description = "북마크 취소할 특정 게시물 번호") @PathVariable(name = "postNo") Long postNo) {

        AccessTokenInfo accessTokenInfo = jwtTokenProvider.getMemberInfoByBearerToken(bearerToken);
        bookmarkService.deleteBookmark(accessTokenInfo.memberNo(), postNo);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResult.success(DELETE_RESPONSE_MESSAGE, null)); // statusCode 204
    }

}

