package com.runninghi.runninghibackv2.application.service;

import com.runninghi.runninghibackv2.application.dto.record.response.GetMonthlyRecordResponse;
import com.runninghi.runninghibackv2.application.dto.record.response.GetWeeklyRecordResponse;
import com.runninghi.runninghibackv2.application.dto.record.response.GetYearlyRecordResponse;
import com.runninghi.runninghibackv2.domain.entity.Member;
import com.runninghi.runninghibackv2.domain.entity.Record;
import com.runninghi.runninghibackv2.domain.entity.vo.GpsDataVO;
import com.runninghi.runninghibackv2.domain.repository.RecordQueryRepository;
import com.runninghi.runninghibackv2.domain.repository.RecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;
    private final RecordQueryRepository recordQueryRepository;

    public void createRecord(Member member, GpsDataVO recordData, Long targetNo) {

        Record runRecord = Record.builder()
                .member(member)
                .kcal(recordData.getKcal())
                .meanPace(recordData.getMeanPace())
                .distance(recordData.getDistance())
                .time(recordData.getTime())
                .date(LocalDate.from(recordData.getRunStartTime()))
                .targetNo(targetNo)
                .build();

        recordRepository.save(runRecord);
    }


    public GetWeeklyRecordResponse getWeeklyRecord(Long memberNo, LocalDate date) {
        return recordQueryRepository.getWeeklyRecord(memberNo, date);
    }

    public GetMonthlyRecordResponse getMonthlyRecord(Long memberNo, LocalDate date) {
        return recordQueryRepository.getMonthlyRecord(memberNo, date);
    }

    public GetYearlyRecordResponse getYearlyRecord(Long memberNo, LocalDate date) {
        return recordQueryRepository.getYearlyRecord(memberNo, date);
    }

    public void deleteRecord(Long targetNo) {
        try {
            recordRepository.deleteByTargetNo(targetNo);
        } catch (Exception e) {
            log.error("기록 삭제 중 오류 발생. 관련 게시글 번호: {}",  targetNo, e);
            throw e;
        }
    }

}
