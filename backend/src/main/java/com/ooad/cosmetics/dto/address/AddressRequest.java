package com.ooad.cosmetics.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank
        @Size(max = 150)
        String receiverName,

        @NotBlank
        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(max = 255)
        String addressLine,

        @Size(max = 120)
        String ward,

        @Size(max = 120)
        String district,

        @NotBlank
        @Size(max = 120)
        String city,

        boolean defaultAddress
) {
}
