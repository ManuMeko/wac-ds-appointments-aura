package com.wacds.appointments.error;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    Map<String, String> validationErrors
) {
}