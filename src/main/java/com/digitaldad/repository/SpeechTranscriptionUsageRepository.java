package com.digitaldad.ai.repository;

import com.digitaldad.ai.entity.SpeechTranscriptionUsage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 语音转写使用记录 Repository
 */
public interface SpeechTranscriptionUsageRepository extends JpaRepository<SpeechTranscriptionUsage, Long> {
}
