package com.ooad.cosmetics.dto.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResponseTest {

    @Test
    void maskNameKeepsFirstWordAndInitialOfTheLastWord() {
        assertThat(ReviewResponse.maskName("Nguyen Van An")).isEqualTo("Nguyen A.");
    }

    @Test
    void maskNameKeepsSingleWordAsIs() {
        assertThat(ReviewResponse.maskName("Thao")).isEqualTo("Thao");
    }

    @Test
    void maskNameHandlesExtraSpacesAndBlankValues() {
        assertThat(ReviewResponse.maskName("  Tran   Thi  Bao ")).isEqualTo("Tran B.");
        assertThat(ReviewResponse.maskName("   ")).isEqualTo("Customer");
        assertThat(ReviewResponse.maskName(null)).isEqualTo("Customer");
    }
}
