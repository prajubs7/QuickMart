package com.quickmart.dto;

import com.quickmart.entity.Address;

import java.time.Instant;

public record AddressResponse(Long id, String label, String line1, String city, String pincode,
                              boolean isDefault, Instant createdAt) {

    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLine1(), a.getCity(), a.getPincode(),
                a.isDefaultAddress(), a.getCreatedAt());
    }
}
