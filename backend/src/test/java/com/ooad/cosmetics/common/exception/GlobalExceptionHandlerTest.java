package com.ooad.cosmetics.common.exception;

import com.ooad.cosmetics.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundReturns404() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(new ResourceNotFoundException("Product", 10));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
    }

    @Test
    void badRequestReturns400() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequest(new BadRequestException("Bad request"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
