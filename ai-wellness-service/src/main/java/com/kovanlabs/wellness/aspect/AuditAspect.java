package com.kovanlabs.wellness.aspect;

import com.kovanlabs.wellness.entity.AuditLogEntity;
import com.kovanlabs.wellness.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning(pointcut = "execution(* com.kovanlabs.wellness.service.*.*(..))")
    public void logUserAction(JoinPoint joinPoint) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return; // Do not log unauthenticated actions here
            }

            Long userId = null;
            if (auth.getPrincipal() instanceof Long id) {
                userId = id;
            } else {
                try {
                    userId = Long.parseLong(auth.getPrincipal().toString());
                } catch (NumberFormatException e) {
                    return;
                }
            }

            String action = joinPoint.getSignature().getName();
            String resource = joinPoint.getSignature().getDeclaringTypeName();

            String ipAddress = "UNKNOWN";
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest request = servletAttributes.getRequest();
                ipAddress = request.getRemoteAddr();
                
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    ipAddress = forwarded.split(",")[0].trim();
                }
            }

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .userId(userId)
                    .action(action)
                    .resource(resource)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(logEntity);
            log.debug("Audit log saved for action: {} by user: {}", action, userId);
        } catch (Exception e) {
            log.error("Failed to save audit log for action: {}", joinPoint.getSignature().getName(), e);
        }
    }
}
