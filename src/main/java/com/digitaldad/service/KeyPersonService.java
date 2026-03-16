package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.AddKeyPersonRequest;
import com.digitaldad.dto.KeyPersonResponse;
import com.digitaldad.dto.UpdateKeyPersonRequest;
import com.digitaldad.entity.KeyPerson;
import com.digitaldad.repository.InterviewSessionRepository;
import com.digitaldad.repository.KeyPersonRepository;
import com.digitaldad.repository.ProjectParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关键人物服务
 * <p>管理会话中的关键人物（如家人、朋友等）的增删改查。</p>
 */
@Service
@RequiredArgsConstructor
public class KeyPersonService {

    private final KeyPersonRepository personRepository;
    private final InterviewSessionRepository sessionRepository;
    private final ProjectParticipantRepository participantRepository;

    private void checkSessionAccess(Long sessionId, Long userId) {
        var session = sessionRepository.findById(sessionId).orElseThrow(() -> new BusinessException(404, "会话不存在"));
        var p = participantRepository.findById(session.getParticipantId()).orElseThrow();
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
    }

    /**
     * 列出当前用户的角色库（用户维度，与 session 无关）
     */
    public List<KeyPersonResponse> listByUser(Long userId) {
        return personRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 列出会话中的关键人物（兼容旧接口：仍返回该用户整份角色库）
     */
    public List<KeyPersonResponse> list(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        return listByUser(userId);
    }

    /**
     * 添加关键人物（写入用户角色库，可选记录创建时会话）
     */
    @Transactional
    public KeyPersonResponse add(Long sessionId, Long userId, AddKeyPersonRequest request) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow();
        var participant = participantRepository.findById(session.getParticipantId()).orElseThrow();
        KeyPerson p = new KeyPerson();
        p.setUserId(participant.getUserId());
        p.setSessionId(sessionId);
        p.setParticipantId(session.getParticipantId());
        p.setName(request.getName());
        p.setRoleLabel(request.getRoleLabel());
        p = personRepository.save(p);
        return toResponse(p);
    }

    /**
     * 按用户直接添加关键人物（不依赖 session，用于「我的-关键人物」等入口）
     */
    @Transactional
    public KeyPersonResponse addByUser(Long userId, AddKeyPersonRequest request) {
        KeyPerson p = new KeyPerson();
        p.setUserId(userId);
        p.setName(request.getName());
        p.setRoleLabel(request.getRoleLabel());
        p = personRepository.save(p);
        return toResponse(p);
    }

    /**
     * 更新关键人物（校验归属用户）
     */
    @Transactional
    public KeyPersonResponse update(Long personId, Long userId, UpdateKeyPersonRequest request) {
        var p = personRepository.findById(personId).orElseThrow(() -> new BusinessException(404, "人物不存在"));
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
        if (request.getName() != null) p.setName(request.getName());
        if (request.getRoleLabel() != null) p.setRoleLabel(request.getRoleLabel());
        p = personRepository.save(p);
        return toResponse(p);
    }

    /**
     * 删除关键人物（校验归属用户）
     */
    @Transactional
    public void delete(Long personId, Long userId) {
        var p = personRepository.findById(personId).orElseThrow(() -> new BusinessException(404, "人物不存在"));
        if (!p.getUserId().equals(userId)) throw new BusinessException(403, "无权限");
        personRepository.delete(p);
    }

    /**
     * 由 AI 小结解析时创建关键人物（归属到会话），供小结绑定；用户可在前端编辑。
     */
    @Transactional
    public KeyPerson createForSession(Long userId, Long sessionId, Long participantId, String name, String roleLabel) {
        KeyPerson p = new KeyPerson();
        p.setUserId(userId);
        p.setSessionId(sessionId);
        p.setParticipantId(participantId);
        p.setName(trimToLength(name, 50));
        p.setRoleLabel(trimToLength(roleLabel, 50));
        return personRepository.save(p);
    }

    private static String trimToLength(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /**
     * 校验关键人物是否属于当前用户
     */
    public boolean belongsToUser(Long personId, Long userId) {
        return personRepository.findById(personId).map(p -> p.getUserId().equals(userId)).orElse(false);
    }

    /**
     * 按 ID 列表返回关键人物（保持 ids 顺序），供小结详情等使用
     */
    public List<KeyPersonResponse> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<KeyPerson> list = personRepository.findAllById(ids);
        Map<Long, KeyPerson> map = list.stream().collect(Collectors.toMap(KeyPerson::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<KeyPersonResponse> result = new ArrayList<>();
        for (Long id : ids) {
            KeyPerson p = map.get(id);
            if (p != null) result.add(toResponse(p));
        }
        return result;
    }

    private KeyPersonResponse toResponse(KeyPerson p) {
        return KeyPersonResponse.builder().id(p.getId()).userId(p.getUserId()).sessionId(p.getSessionId()).name(p.getName()).roleLabel(p.getRoleLabel()).createdAt(p.getCreatedAt()).build();
    }
}
