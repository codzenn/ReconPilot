package com.reconcileguard.dto;

import com.reconcileguard.domain.AuditEvent;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        LocalDateTime timestamp,
        String actor,
        String action,
        String reference,
        String details
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventTime(),
                event.getActor(),
                event.getAction(),
                event.getReferenceId(),
                event.getDetails()
        );
    }
}
