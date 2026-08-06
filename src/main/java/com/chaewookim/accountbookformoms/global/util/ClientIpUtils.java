package com.chaewookim.accountbookformoms.global.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String extract(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
