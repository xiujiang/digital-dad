package com.digitaldad.user.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.user.dto.LoginResponse;
import com.digitaldad.user.dto.CurrentUserResponse;
import com.digitaldad.user.entity.User;
import com.digitaldad.user.enums.SmsScene;
import com.digitaldad.user.enums.UserStatus;
import com.digitaldad.user.enums.UserType;
import com.digitaldad.user.repository.UserRepository;
import com.digitaldad.user.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 * <p>提供验证码发送、用户/超管登录、退出、当前用户信息查询。主持人登录即注册。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final JwtUtils jwtUtils;

    /**
     * 发送登录验证码
     *
     * @param phone 手机号
     */
    public void sendCode(String phone) {
        smsService.sendCode(phone, SmsScene.LOGIN);
    }

    /**
     * 主持人登录（登录即注册：未注册则自动创建主持人账号）
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录结果（Token、用户信息）
     */
    @Transactional
    public LoginResponse login(String phone, String code) {
        // 1. 校验验证码
        if (!smsService.verifyCode(phone, SmsScene.LOGIN, code)) {
            throw new BusinessException(401, "验证码错误或已过期，请重新获取");
        }

        // 2. 查找或注册用户
        User user = userRepository.findByPhoneAndUserTypeAndDeletedAtIsNull(phone, UserType.HOST)
                .orElseGet(() -> registerHost(phone));

        // 3. 校验状态
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        // 4. 更新最近登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 5. 验证码已在 verifyCode 中失效

        // 6. 签发 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUserType(), user.getPhone());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType(user.getUserType().name())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 注册主持人（手机号未注册时，登录即注册）
     * 若手机号已被其他类型账号使用，则抛出异常
     */
    private User registerHost(String phone) {
        if (userRepository.existsByPhoneAndDeletedAtIsNull(phone)) {
            throw new BusinessException(400, "该手机号已绑定其他类型账号");
        }
        User user = new User();
        user.setUserType(UserType.HOST);
        user.setStatus(UserStatus.ENABLED);
        user.setPhone(phone);
        user.setName(null);  // 可在「我的资料」中完善
        user = userRepository.save(user);
        log.info("主持人自动注册: phone={}, userId={}", phone, user.getId());
        return user;
    }

    /**
     * 退出登录（JWT 无状态，客户端清除 Token 即可）
     */
    public void logout() {
        // 可选：Token 黑名单，v0.1 暂不实现
    }

    /**
     * 超管登录（仅允许 SUPER_ADMIN 类型）
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录结果
     */
    @Transactional
    public LoginResponse adminLogin(String phone, String code) {
        if (!smsService.verifyCode(phone, SmsScene.LOGIN, code)) {
            throw new BusinessException(401, "验证码错误或已过期，请重新获取");
        }

        User user = userRepository.findByPhoneAndUserTypeAndDeletedAtIsNull(phone, UserType.SUPER_ADMIN)
                .orElseThrow(() -> new BusinessException(401, "该手机号未开通超管账号，请联系系统管理员"));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUserType(), user.getPhone());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType(user.getUserType().name())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 获取当前用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        return CurrentUserResponse.builder()
                .userId(user.getId())
                .userType(user.getUserType().name())
                .name(user.getName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
