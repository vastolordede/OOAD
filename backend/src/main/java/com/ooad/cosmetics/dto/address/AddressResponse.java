package com.ooad.cosmetics.dto.address;

import com.ooad.cosmetics.entity.Address;

import java.time.Instant;

public record AddressResponse(
        Long id,
        String receiverName,
        String phone,
        String addressLine,
        String ward,
        String district,
        String city,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
