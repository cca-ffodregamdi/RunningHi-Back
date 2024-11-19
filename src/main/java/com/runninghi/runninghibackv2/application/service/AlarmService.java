package com.runninghi.runninghibackv2.application.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.runninghi.runninghibackv2.application.dto.alarm.request.CreateAlarmRequest;
import com.runninghi.runninghibackv2.application.dto.alarm.response.GetAllAlarmResponse;
import com.runninghi.runninghibackv2.common.constant.AlarmConstants;
import com.runninghi.runninghibackv2.common.exception.custom.FcmException;
import com.runninghi.runninghibackv2.domain.entity.Alarm;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.repository.AlarmRepository;
import com.runninghi.runninghibackv2.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AlarmService {

    private final FirebaseMessaging firebaseMessaging;
    private final AlarmRepository alarmRepository;
    private final MemberRepository memberRepository;
    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public SseEmitter sseSubscribe(Long memberNo) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.put(memberNo, emitter);
        log.info("{}의 SSE 구독을 시작합니다. SSE Connected: {}", memberNo, sseEmitters.size());

        emitter.onCompletion(() -> sseEmitters.remove(memberNo));
        emitter.onTimeout(() -> sseEmitters.remove(memberNo));
        return emitter;
    }

    @Transactional(readOnly = true)
    public List<GetAllAlarmResponse> getAllPushAlarms(Long memberNo) {

        // 알람이 없으면 어차피 리스트는 텅! 이므로 알림동의 여부 파악 x
        List<Alarm> alarmList = alarmRepository.findAllByMember_MemberNo(memberNo);
        List<GetAllAlarmResponse> alarmResponseList = alarmList.stream()
                .filter(alarm -> !alarm.isRead())
                .map(GetAllAlarmResponse::ofAlarmEntity)
                .toList();

        for (Alarm alarm : alarmList) {
            alarm.readAlarm();
        }

        return alarmResponseList;
    }

    public void createPushAlarm(CreateAlarmRequest request) {

        Member member = memberRepository.findByMemberNo(request.getTargetMemberNo());
        Alarm alarm = Alarm.builder()
                .member(member)
                .title(request.getTitle())
                .alarmType(request.getAlarmType())
                .targetPage(request.getTargetPage())
                .targetId(request.getTargetId())
                .additionalData(request.getAdditionalData())
                .build();

        alarmRepository.save(alarm);

//        if (member.isAlarmConsent()) { // 알림 동의했을 시 발송, 아닐 시 stop
//            try {
//                sendPushAlarm(request);
//            } catch (FirebaseMessagingException e) {
//                log.info(e.getMessage());
//                throw new FcmException(e.getMessage());
//            }
//        }

        sendSseAlarm(request); // 알림 동의 여부 상관 없이 발송
    }

    private void sendPushAlarm(CreateAlarmRequest request) throws FirebaseMessagingException {

        Notification notification = Notification.builder()
                .setTitle(AlarmConstants.title)
                .setBody(request.getTitle())
                .build();

        Message message = Message.builder()
                .setToken(request.getFcmToken())
                .setNotification(notification)
                .putAllData(request.getAdditionalData())
                .build();

        firebaseMessaging.send(message);
        log.info("푸쉬 알림 전송에 성공하였습니다. memberNo: {}, alarmType: {}", request.getTargetMemberNo(), request.getAlarmType());
    }

    private void sendSseAlarm(CreateAlarmRequest request) {
        SseEmitter emitter = sseEmitters.get(request.getTargetMemberNo());
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alarm")
                        .data(request));
            } catch (IOException e) {
                sseEmitters.remove(request.getTargetMemberNo());
            }
        }
    }

    @Transactional(readOnly = true)
    public void readAlarm(Long alarmNo) {

        Alarm alarm = alarmRepository.findById(alarmNo)
                .orElseThrow(EntityNotFoundException::new);

        alarm.readAlarm();

    }

    @Transactional
    public void deleteAlarm(Long alarmNo) {
        alarmRepository.deleteById(alarmNo);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanUpReadAlarm() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        alarmRepository.deleteReadAlarmsOlderThan(thirtyDaysAgo);
    }

    public void pushAlarmTest(CreateAlarmRequest request) {

        try {
            sendPushAlarm(request);
        } catch (FirebaseMessagingException e) {
            throw new FcmException(e.getMessage());
        }

    }
}
