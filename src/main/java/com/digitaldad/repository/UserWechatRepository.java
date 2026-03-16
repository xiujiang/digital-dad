package com.digitaldad.repository;

import com.digitaldad.entity.UserWechat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 微信用户 Repository
 */
public interface UserWechatRepository extends JpaRepository<UserWechat, Long> {

    Optional<UserWechat> findByAppTypeAndOpenid(String appType, String openid);

    Optional<UserWechat> findByUserId(Long userId);

    boolean existsByAppTypeAndOpenid(String appType, String openid);
}
