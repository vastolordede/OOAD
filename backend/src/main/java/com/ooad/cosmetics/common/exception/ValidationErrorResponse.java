package com.ooad.cosmetics.common.exception;

import java.util.Map;

public record ValidationErrorResponse(String error, Map<String, String> fields) {}
