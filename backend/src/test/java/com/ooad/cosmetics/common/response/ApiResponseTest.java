package com.ooad.cosmetics.common.response;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successFactoryCreatesSuccessfulResponse() {
        ApiResponse<String> response = ApiResponse.success("Created", "payload");
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Created");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void failureFactoryCreatesFailureResponse() {
        ApiResponse<Void> response = ApiResponse.failure("Invalid request");
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Invalid request");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
}
