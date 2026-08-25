package com.queueless.service;

import com.queueless.entity.AuditLog;
import com.queueless.entity.User;
import com.queueless.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(User user, String action, String entityType, Long entityId, String metadata) {
        AuditLog auditLog = new AuditLog(user, action, entityType, entityId, metadata);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Long userId, String action, Pageable pageable) {
        if (userId != null) {
            return auditLogRepository.findByUserId(userId, pageable);
        } else if (action != null && !action.isBlank()) {
            return auditLogRepository.findByAction(action, pageable);
        } else {
            return auditLogRepository.findAll(pageable);
        }
    }
}
