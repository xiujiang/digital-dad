package com.digitaldad.project.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.dto.AddKeyPersonRequest;
import com.digitaldad.project.dto.KeyPersonResponse;
import com.digitaldad.project.dto.UpdateKeyPersonRequest;
import com.digitaldad.project.entity.KeyPerson;
import com.digitaldad.project.repository.InterviewSessionRepository;
import com.digitaldad.project.repository.KeyPersonRepository;
import com.digitaldad.project.repository.ProjectParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * 列出会话中的关键人物
     */
    public List<KeyPersonResponse> list(Long sessionId, Long userId) {
        checkSessionAccess(sessionId, userId);
        return personRepository.findBySessionId(sessionId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 添加关键人物
     */
    @Transactional
    public KeyPersonResponse add(Long sessionId, Long userId, AddKeyPersonRequest request) {
        checkSessionAccess(sessionId, userId);
        var session = sessionRepository.findById(sessionId).orElseThrow();
        KeyPerson p = new KeyPerson();
        p.setSessionId(sessionId);
        p.setParticipantId(session.getParticipantId());
        p.setName(request.getName());
        p.setRoleLabel(request.getRoleLabel());
        p = personRepository.save(p);
        return toResponse(p);
    }

    /**
     * 更新关键人物
     */
    @Transactional
    public KeyPersonResponse update(Long personId, Long userId, UpdateKeyPersonRequest request) {
        var p = personRepository.findById(personId).orElseThrow(() -> new BusinessException(404, "人物不存在"));
        checkSessionAccess(p.getSessionId(), userId);
        if (request.getName() != null) p.setName(request.getName());
        if (request.getRoleLabel() != null) p.setRoleLabel(request.getRoleLabel());
        p = personRepository.save(p);
        return toResponse(p);
    }

    /**
     * 删除关键人物
     */
    @Transactional
    public void delete(Long personId, Long userId) {
        var p = personRepository.findById(personId).orElseThrow(() -> new BusinessException(404, "人物不存在"));
        checkSessionAccess(p.getSessionId(), userId);
        personRepository.delete(p);
    }

    private KeyPersonResponse toResponse(KeyPerson p) {
        return KeyPersonResponse.builder().id(p.getId()).sessionId(p.getSessionId()).name(p.getName()).roleLabel(p.getRoleLabel()).createdAt(p.getCreatedAt()).build();
    }
}
