package com.digitaldad.repository;

import com.digitaldad.entity.SmsCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 验证码 Repository（可选，审计用）
 */
public interface SmsCodeRepository extends JpaRepository<SmsCode, Long> {

    Optional<SmsCode> findTopByPhoneAndSceneAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, String scene, LocalDateTime now);
}
