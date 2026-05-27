package com.hbcstore.hbcstore_api.common;

import java.time.Instant;

public record ErrorResponse(
        String message,
        String path,
        Instant timestamp
) {
}