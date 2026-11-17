package com.quickmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request DTO: only the fields a client may send. No id, no createdAt. The server owns those.
public record CategoryRequest(
        @NotBlank @Size(max = 100) String name   // max matches VARCHAR(100), so a 400 instead of a DB error
) {
}
