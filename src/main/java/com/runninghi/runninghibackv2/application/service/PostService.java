package com.runninghi.runninghibackv2.application.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runninghi.runninghibackv2.application.dto.alarm.request.CreateAlarmRequest;
import com.runninghi.runninghibackv2.application.dto.image.response.ImageTarget;
import com.runninghi.runninghibackv2.application.dto.post.request.CreatePostRequest;
import com.runninghi.runninghibackv2.application.dto.post.request.UpdatePostRequest;
import com.runninghi.runninghibackv2.application.dto.post.response.*;
import com.runninghi.runninghibackv2.common.exception.custom.S3UploadException;
import com.runninghi.runninghibackv2.common.response.PageResultData;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.MemberChallenge;
import com.runninghi.runninghibackv2.domain.entity.Post;
import com.runninghi.runninghibackv2.domain.entity.Record;
import com.runninghi.runninghibackv2.domain.entity.vo.GpsDataVO;
import com.runninghi.runninghibackv2.domain.enumtype.*;
import com.runninghi.runninghibackv2.domain.repository.*;
import com.runninghi.runninghibackv2.domain.service.PostChecker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostChecker postChecker;
    private final PostRepository postRepository;
    private final ImageService imageService;
    private final MemberRepository memberRepository;
    private final PostQueryRepository postQueryRepository;
    private final ChallengeQueryRepository challengeQueryRepository;
    private final RecordService recordService;
    private final AlarmService alarmService;

    private static final String LEVEL_FCM_TITLE = "새로운 레벨에 도달했습니다! 계속 나아가세요!";

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final AmazonS3Client amazonS3Client;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private String buildKey(String dirName) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String now = sdf.format(new Date());

        String newFileName = UUID.randomUUID() + "_" + now;

        return dirName + "/" + newFileName + ".txt";
    }

    public String uploadGpsToS3(MultipartFile file, String dirName) {
        String key = buildKey(dirName);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("text/plain");
        objectMetadata.setContentLength(file.getSize());

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file.getBytes())) {
            amazonS3Client.putObject(bucketName, key, byteArrayInputStream, objectMetadata);
            return amazonS3Client.getUrl(bucketName, key).toString();
        } catch (IOException e) {
            log.error("GPS txt 파일 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException(e.getMessage());
        }
    }

    private String getMainData(int dataNo, GpsDataVO gpsDataVO) {
        String mainData = null;
        DecimalFormat df = new DecimalFormat("#.##");

        try {
            switch (dataNo) {
                case 0:
                    mainData = df.format(gpsDataVO.getDistance()) + "km";
                    break;
                case 1:
                    mainData = gpsDataVO.getTime() / 60 + "분 " + gpsDataVO.getTime() % 60 + "초";
                    break;
                case 2:
                    mainData = gpsDataVO.getKcal() + "Kcal";
                    break;
                case 3:
                    mainData = gpsDataVO.getMeanPace() / 60 + "' " + gpsDataVO.getMeanPace() % 60 + "\"";
                    break;
                case 4:
                    break;
                default:
                    log.warn("Invalid dataNo: {}", dataNo);
                    break;
            }
        } catch (Exception e) {
            log.error("MainDataNo: {}에 대한 데이터 처리 중 오류 발생", dataNo, e);
            throw new IllegalArgumentException(e.getMessage());
        }
        return mainData;
    }

    public Post findPostByNo(Long postNo) {
        return postRepository.findById(postNo)
                .orElseThrow(EntityNotFoundException::new);
    }

    private void savePostImage(String imageUrl, Long postNo) {
        imageService.saveTargetNo(imageUrl, ImageTarget.POST, postNo);
    }

    private String createPostTitle(GpsDataVO request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN);
        return request.getRunStartTime().format(formatter) + " " + request.getLocationName() + " 러닝";
    }

    private String readFileContent(MultipartFile file) throws IOException {

        log.info("런데이터 GPS 파일 읽기 시작: {}", LocalDateTime.now());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            log.info("런데이터 GPS 파일 읽기 완료: {}", LocalDateTime.now());
            return content;
        } catch (IOException e) {
            log.error("런데이터 GPS 파일 읽기 중 오류 발생: {}", LocalDateTime.now(), e);
            throw e;
        }
    }

    private List<Integer> getIntegerList(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asInt)
                .collect(Collectors.toList());
    }

    public GpsDataVO getRunDataFromTxt(MultipartFile file) throws IOException {
        String content = readFileContent(file);
        JsonNode rootNode = objectMapper.readTree(content);

        JsonNode runInfoNode = rootNode.get("runInfo");
        JsonNode sectionDataNode = rootNode.get("sectionData");
        JsonNode gpsDataNode = rootNode.get("gpsData");

        Point startPoint = null;
        if (gpsDataNode.isArray() && gpsDataNode.size() > 0) {
            JsonNode firstGpsPoint = gpsDataNode.get(0);
            double lon = firstGpsPoint.get("lon").asDouble();
            double lat = firstGpsPoint.get("lat").asDouble();
            startPoint = geometryFactory.createPoint(new Coordinate(lon, lat));
        }

        return GpsDataVO.builder()
                .locationName(runInfoNode.get("location").asText())
                .startPoint(startPoint)
                .runStartTime(LocalDateTime.parse(runInfoNode.get("runStartDate").asText()))
                .distance(Math.round(runInfoNode.get("distance").asDouble() * 100) / 100.0f)
                .time(runInfoNode.get("time").asInt())
                .kcal(runInfoNode.get("kcal").asInt())
                .meanPace(runInfoNode.get("meanPace").asInt())
                .sectionPace(getIntegerList(sectionDataNode.get("pace")))
                .sectionKcal(getIntegerList(sectionDataNode.get("kcal")))
                .difficulty(Difficulty.valueOf(runInfoNode.get("difficulty").asText()))
                .build();
    }

    @Transactional(readOnly = true)
    public PageResultData<GetAllPostsResponse> getPostScroll(Long memberNo, Pageable pageable, String sort) {
        log.info("게시물 전체 조회 요청. 회원번호: {}, 정렬기준: {}", memberNo, sort);

        PageResultData<GetAllPostsResponse> result;
        try {
            switch (sort.toLowerCase()) {
                case "latest":
                    result = postQueryRepository.findAllPostsByLatest(memberNo, pageable);
                    break;
                case "recommended":
                    result = postQueryRepository.findAllPostsByRecommended(memberNo, pageable);
                    break;
                case "like":
                    result = postQueryRepository.findAllPostsByLikeCnt(memberNo, pageable);
                    break;
//                case "distance":
//                    result = postQueryRepository.findAllPostsByDistance(memberNo, pageable);
//                    break;
                default:
                    log.warn("알 수 없는 정렬 기준: {}. 기본값(최신순)으로 조회합니다.", sort);
                    result = postQueryRepository.findAllPostsByLatest(memberNo, pageable);
            }
            return result;
        } catch (Exception e) {
            log.error("게시물 스크롤 조회 중 오류 발생", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public GetPostResponse getPostDetailByPostNo(Long memberNo, Long postNo) {
        log.info("게시물 상세 조회. 회원번호: {}, 게시물번호: {}", memberNo, postNo);
        return postQueryRepository.getPostDetailByPostNo(memberNo, postNo);
    }

    @Transactional(readOnly = true)
    public PageResultData<GetMyPostsResponse> getMyPostsScroll(Pageable pageable, Long memberNo) {
        log.info("내 게시물 리스트 조회. 회원번호: {}", memberNo);
        return postQueryRepository.findMyPostsByPageable(pageable, memberNo);
    }

    @Transactional(readOnly = true)
    public PageResultData<GetAllPostsResponse> getMyLikedPosts(Pageable pageable, Long memberNo) {
        log.info("나의 좋아요된 게시글 리스트 조회. 회원번호: {}", memberNo);
        return postQueryRepository.findMyLikedPostsByPageable(pageable, memberNo);
    }

    @Transactional(readOnly = true)
    public PageResultData<GetAllPostsResponse> getMyBookmarkedPosts(Pageable pageable, Long memberNo) {
        log.info("나의 북마크된 게시글 리스트 조회. 회원번호: {}", memberNo);
        return postQueryRepository.findMyBookmarkedPostsByPageable(pageable, memberNo);
    }

    @Transactional(readOnly = true)
    public GetAllPostsResponse getPostByPostNo(Long memberNo, Long postNo) {
        log.info("게시물 하나 조회. 회원번호: {}, 게시물번호: {}", memberNo, postNo);
        return postQueryRepository.getPostByPostNo(memberNo, postNo);
    }


    @Transactional
    public CreateRecordResponse createRecord(Long memberNo, MultipartFile file) throws IOException {
        log.info("GPS 기록 생성 시작. 회원번호: {}", memberNo);
        try {
            GpsDataVO gpsDataVO = getRunDataFromTxt(file);

            Member member = memberRepository.findByMemberNo(memberNo);

            String gpsUrl = uploadGpsToS3(file, member.getMemberNo().toString());

            int nowLevel = member.getRunDataVO().getLevel();
            member.getRunDataVO().updateTotalDistanceKcalAndLevel(gpsDataVO.getDistance(), gpsDataVO.getKcal());

            int updateLevel = member.getRunDataVO().getLevel();

            if (nowLevel < updateLevel) {
                // 레벨업 알림
                CreateAlarmRequest alarmRequest = CreateAlarmRequest.builder()
                        .title(LEVEL_FCM_TITLE)
                        .targetMemberNo(member.getMemberNo())
                        .alarmType(AlarmType.LEVEL_UP)
                        .targetPage(TargetPage.MYPAGE)
                        .targetId(member.getMemberNo())
                        .fcmToken(member.getFcmToken())
                        .build();
                alarmService.createPushAlarm(alarmRequest);
            }

            Post createdPost = postRepository.save(Post.builder()
                    .member(member)
                    .role(member.getRole())
                    .gpsDataVO(gpsDataVO)
                    .gpxUrl(gpsUrl)
                    .status(false)
                    .postTitle(createPostTitle(gpsDataVO))
                    .build());
            recordService.createRecord(member, gpsDataVO, createdPost.getPostNo());
            updateRecordOfMemberChallenges(memberNo, gpsDataVO);

            return new CreateRecordResponse(createdPost.getPostNo());
        } catch (Exception e) {
            log.error("GPS 기록 생성 중 오류 발생. 회원번호: {}", memberNo, e);
            throw e;
        }
    }


    @Transactional
    public CreatePostResponse createPost(Long memberNo, CreatePostRequest request) {
        log.info("게시물 생성 시작. 회원번호: {}, 임시 게시물번호: {}", memberNo, request.postNo());

        postChecker.checkPostValidation(request.postContent());

        Post post = postRepository.findById(request.postNo())
                .orElseThrow(EntityNotFoundException::new);

        postChecker.isWriter(memberNo, post.getMember().getMemberNo());

        if (!request.imageUrl().isBlank()) {
            savePostImage(request.imageUrl(), post.getPostNo());
        }

        String mainData = getMainData(request.mainData(), post.getGpsDataVO());

        try {
            post.shareToPost(request, mainData, request.mainData());
        } catch (Exception e) {
            log.error("게시글 생성 중 오류 발생. 회원번호: {}, 게시글 번호: {}", memberNo, request.postNo(), e);
            throw e;
        }

        return new CreatePostResponse(post.getPostNo());
    }

    @Transactional
    public UpdatePostResponse updatePost(Long memberNo, Long postNo, UpdatePostRequest request) {
        log.info("게시물 수정 시작. 회원번호: {}, 게시물 번호: {}", memberNo, postNo);

        Post post = findPostByNo(postNo);
        postChecker.isWriter(memberNo, post.getMember().getMemberNo());

        String mainData = getMainData(request.mainData(), post.getGpsDataVO());

        try {
            post.update(request, mainData, request.mainData());
            imageService.updateImage(ImageTarget.POST, postNo, request.imageUrl());
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생. 회원번호: {}, 게시글 번호: {}", memberNo, postNo, e);
            throw e;
        }

        return UpdatePostResponse.from(post, request.imageUrl());
    }

    @Transactional
    public DeletePostResponse deletePost(Long memberNo, Long postNo) {
        log.info("게시물 삭제 시작. 회원번호: {}, 게시물 번호: {}", memberNo, postNo);

        Post post = findPostByNo(postNo);
        postChecker.isWriter(memberNo, post.getMember().getMemberNo());

        try {
            postRepository.deleteById(postNo);
            log.info("게시물 삭제 완료. 게시물 번호: {}", postNo);

            recordService.deleteRecord(postNo);
            log.info("연관 레코드 삭제 완료. 게시물 번호: {}", postNo);
        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생. 회원번호: {}, 게시물 번호: {}", memberNo, postNo, e);
            throw e;
        }

        return DeletePostResponse.from(postNo);
    }


    @Transactional
    public void addReportedCount(Long postNo) {
        Post post = findPostByNo(postNo);
        post.addReportedCount();
    }

    @Transactional
    public void resetReportedCount(Long postNo) {

        Post post = findPostByNo(postNo);

        post.resetReportedCount();
    }

    private void updateRecordOfMemberChallenges(Long memberNo, GpsDataVO gpsDataVO) {
        Member member = memberRepository.findByMemberNo(memberNo);

        List<MemberChallenge> memberChallenges =
                challengeQueryRepository.findMemberChallengesByStatus(memberNo, ChallengeStatus.IN_PROGRESS);

        if (memberChallenges.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);

        for (MemberChallenge myChallenge : memberChallenges) {
            if (myChallenge.getChallenge().getChallengeCategory() == ChallengeCategory.ATTENDANCE) {
                List<Post> postList = postRepository.findByMemberAndCreateDateBetween(member, startOfDay, endOfDay);
                if (postList.size() == 1) {
                    myChallenge.updateRecord();
                }
                continue;
            }
            myChallenge.updateRecord(gpsDataVO);
        }
    }
}
