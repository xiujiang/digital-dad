package com.digitaldad.repository;

import com.digitaldad.entity.SpeechTranscriptionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 语音转写使用记录 Repository
 */
public interface SpeechTranscriptionUsageRepository extends JpaRepository<SpeechTranscriptionUsage, Long> {

    /** 今日语音识别总秒数 */
    @Query("SELECT COALESCE(SUM(s.durationSeconds), 0) FROM SpeechTranscriptionUsage s WHERE s.createdAt >= :start AND s.createdAt < :end")
    long sumDurationSecondsByCreatedAtBetween(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
