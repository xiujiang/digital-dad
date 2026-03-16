package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.WeChatLoginRequest;
import com.digitaldad.config.dto.PasswordPolicyConfigDto;
import com.digitaldad.dto.LoginResponse;
import com.digitaldad.dto.CurrentUserResponse;
import com.digitaldad.dto.WeChatCode2SessionResponse;
import com.digitaldad.entity.User;
import com.digitaldad.entity.UserLoginLog;
import com.digitaldad.entity.UserRole;
import com.digitaldad.entity.UserWechat;
import com.digitaldad.enums.SmsScene;
import com.digitaldad.enums.UserStatus;
import com.digitaldad.repository.UserLoginLogRepository;
import com.digitaldad.repository.UserRepository;
import com.digitaldad.repository.UserRoleRepository;
import com.digitaldad.repository.UserWechatRepository;
import com.digitaldad.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务
 * <p>统一用户表 + 角色表；主持人登录即注册；超管登录需具备 SUPER_ADMIN 角色。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserWechatRepository userWechatRepository;
    private final UserLoginLogRepository userLoginLogRepository;
    private final WeChatMiniProgramService weChatMiniProgramService;
    private final SmsService smsService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;

    public void sendCode(String phone) {
        smsService.sendCode(phone, SmsScene.LOGIN);
    }

    /**
     * 主持人登录（按手机号；未注册则自动创建用户并赋予 HOST 角色）
     */
    @Transactional
    public LoginResponse login(String phone, String code) {
        if (!smsService.verifyCode(phone, SmsScene.LOGIN, code)) {
            throw new BusinessException(401, "验证码错误或已过期，请重新获取");
        }

        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseGet(() -> registerHost(phone));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        List<String> roles = loadRoles(user.getId());
        String token = jwtUtils.generateToken(user.getId(), roles, user.getPhone());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType(primaryRole(roles))
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    private User registerHost(String phone) {
        if (userRepository.existsByPhoneAndDeletedAtIsNull(phone)) {
            throw new BusinessException(400, "该手机号已绑定其他类型账号");
        }
        User user = new User();
        user.setStatus(UserStatus.ENABLED);
        user.setPhone(phone);
        user.setName(null);
        user = userRepository.save(user);
        UserRole role = new UserRole();
        role.setUserId(user.getId());
        role.setRole("HOST");
        userRoleRepository.save(role);
        log.info("主持人自动注册: phone={}, userId={}", phone, user.getId());
        return user;
    }

    public void logout() {
    }

    /**
     * 微信小程序登录/注册（后端用 code 调微信 jscode2session 换 openid，再查/建用户、写登录流水并签发 JWT）
     *
     * @param request   必填 code；可选 nickName、avatarUrl 等用于完善资料
     * @param clientIp  请求 IP（写登录流水，可为 null）
     * @param userAgent User-Agent（写登录流水，可为 null）
     */
    @Transactional
    public LoginResponse wechatLogin(WeChatLoginRequest request, String clientIp, String userAgent) {
        String code = request.getCode() != null ? request.getCode().trim() : "";
        if (code.isEmpty()) {
            throw new BusinessException(400, "code 不能为空");
        }

        WeChatCode2SessionResponse session = weChatMiniProgramService.code2session(code);
        String openid = session.getOpenid();
        String sessionKey = session.getSessionKey();
        String unionidFromWechat = session.getUnionid();
        String unionid = request.getUnionId() != null && !request.getUnionId().isBlank()
                ? request.getUnionId().trim()
                : unionidFromWechat;

        String appType = "miniprogram";
        var existingWechat = userWechatRepository.findByAppTypeAndOpenid(appType, openid);
        User user;
        if (existingWechat.isPresent()) {
            user = userRepository.findById(existingWechat.get().getUserId())
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
            if (request.getNickName() != null && !request.getNickName().isBlank()) {
                user.setName(request.getNickName().trim());
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(request.getAvatarUrl().trim());
            }
            userRepository.save(user);
            UserWechat wechat = existingWechat.get();
            if (request.getNickName() != null) wechat.setNickname(request.getNickName());
            if (request.getAvatarUrl() != null) wechat.setAvatarUrl(request.getAvatarUrl());
            if (unionid != null) wechat.setUnionid(unionid);
            if (request.getGender() != null) wechat.setGender(request.getGender());
            if (request.getCountry() != null) wechat.setCountry(request.getCountry());
            if (request.getProvince() != null) wechat.setProvince(request.getProvince());
            if (request.getCity() != null) wechat.setCity(request.getCity());
            if (sessionKey != null && !sessionKey.isBlank()) {
                wechat.setSessionKey(sessionKey);
            }
            userWechatRepository.save(wechat);
        } else {
            user = new User();
            user.setStatus(UserStatus.ENABLED);
            user.setPhone(null);
            user.setName(request.getNickName() != null && !request.getNickName().isBlank() ? request.getNickName().trim() : null);
            user.setAvatarUrl(request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank() ? request.getAvatarUrl().trim() : null);
            user = userRepository.save(user);
            UserRole role = new UserRole();
            role.setUserId(user.getId());
            role.setRole("WECHAT_USER");
            userRoleRepository.save(role);
            UserWechat wechat = new UserWechat();
            wechat.setUserId(user.getId());
            wechat.setOpenid(openid);
            wechat.setAppType(appType);
            wechat.setUnionid(unionid);
            wechat.setSessionKey(sessionKey);
            wechat.setNickname(request.getNickName());
            wechat.setAvatarUrl(request.getAvatarUrl());
            wechat.setGender(request.getGender());
            wechat.setCountry(request.getCountry());
            wechat.setProvince(request.getProvince());
            wechat.setCity(request.getCity());
            userWechatRepository.save(wechat);
            log.info("微信用户自动注册: openid={}, userId={}", openid, user.getId());
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setLoginAt(LocalDateTime.now());
        loginLog.setChannel("WECHAT_MINIPROGRAM");
        loginLog.setIp(clientIp);
        loginLog.setUserAgent(userAgent);
        userLoginLogRepository.save(loginLog);

        List<String> roles = loadRoles(user.getId());
        String token = jwtUtils.generateToken(user.getId(), roles, user.getPhone());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType(primaryRole(roles))
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * 超管登录（按手机号；该用户须具备 SUPER_ADMIN 角色）
     */
    @Transactional
    public LoginResponse adminLogin(String phone, String code) {
        if (!smsService.verifyCode(phone, SmsScene.LOGIN, code)) {
            throw new BusinessException(401, "验证码错误或已过期，请重新获取");
        }

        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new BusinessException(401, "该手机号未注册，请先使用主持人入口登录或联系管理员开通"));

        List<String> roles = loadRoles(user.getId());
        if (!roles.contains("SUPER_ADMIN")) {
            throw new BusinessException(401, "该手机号未开通超管账号，请联系系统管理员");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId(), roles, user.getPhone());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType("SUPER_ADMIN")
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 主持人密码登录（手机号+密码；用户须已设置密码）
     */
    @Transactional
    public LoginResponse loginWithPassword(String phone, String password) {
        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new BusinessException(401, "该手机号未注册"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(401, "该账号未设置密码，请使用验证码登录");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "密码错误");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }
        checkPasswordExpiry(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        List<String> roles = loadRoles(user.getId());
        String token = jwtUtils.generateToken(user.getId(), roles, user.getPhone());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType(primaryRole(roles))
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 超管密码登录（手机号+密码；该用户须具备 SUPER_ADMIN 角色且已设置密码）
     */
    @Transactional
    public LoginResponse adminLoginWithPassword(String phone, String password) {
        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new BusinessException(401, "该手机号未注册"));
        List<String> roles = loadRoles(user.getId());
        if (!roles.contains("SUPER_ADMIN")) {
            throw new BusinessException(401, "该手机号未开通超管账号");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(401, "该账号未设置密码，请使用验证码登录");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "密码错误");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(401, "账号已被禁用");
        }
        checkPasswordExpiry(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getId(), roles, user.getPhone());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .userType("SUPER_ADMIN")
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

    /**
     * 设置或修改密码（需已登录）
     * 首次设置时 oldPassword 可不填；修改时需填原密码
     */
    @Transactional
    public void setPassword(Long userId, String newPassword, String oldPassword) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        if (hasPassword) {
            if (oldPassword == null || oldPassword.isBlank()) {
                throw new BusinessException(400, "修改密码须提供原密码");
            }
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new BusinessException(401, "原密码错误");
            }
        }
        validatePasswordAgainstPolicy(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * 按密码策略校验密码（强密码：8位以上，含大小写字母、数字、特殊字符）
     */
    private void validatePasswordAgainstPolicy(String password) {
        PasswordPolicyConfigDto policy = configService.getPasswordPolicy();
        if (Boolean.TRUE.equals(policy.getEnforceStrongPassword())) {
            if (password.length() < 8) {
                throw new BusinessException(400, "密码至少8位");
            }
            boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
            boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
            boolean hasDigit = password.chars().anyMatch(Character::isDigit);
            boolean hasSpecial = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
            int matchCount = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
            if (matchCount < 3) {
                throw new BusinessException(400, "强密码须包含大小写字母、数字、特殊字符中的至少三种");
            }
        }
    }

    /**
     * 检查密码是否过期（定期修改策略）
     */
    private void checkPasswordExpiry(User user) {
        PasswordPolicyConfigDto policy = configService.getPasswordPolicy();
        if (!Boolean.TRUE.equals(policy.getRequirePasswordChangePeriodically())) {
            return;
        }
        LocalDateTime lastChanged = user.getLastPasswordChangedAt();
        if (lastChanged == null) {
            throw new BusinessException(403, "密码已过期，请先登录后修改密码");
        }
        int intervalDays = policy.getPasswordChangeIntervalDays() != null ? policy.getPasswordChangeIntervalDays() : 90;
        if (lastChanged.plusDays(intervalDays).isBefore(LocalDateTime.now())) {
            throw new BusinessException(403, "密码已过期，请先使用验证码登录后修改密码");
        }
    }

    /**
     * 获取当前用户信息（含角色列表）
     */
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        List<String> roles = loadRoles(user.getId());

        return CurrentUserResponse.builder()
                .userId(user.getId())
                .userType(primaryRole(roles))
                .roles(roles)
                .name(user.getName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private List<String> loadRoles(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    private static String primaryRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "";
        if (roles.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (roles.contains("HOST")) return "HOST";
        if (roles.contains("WECHAT_USER")) return "WECHAT_USER";
        return roles.get(0);
    }
}
