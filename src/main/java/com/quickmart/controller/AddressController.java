package com.quickmart.controller;

import com.quickmart.config.JwtClaims;
import com.quickmart.dto.AddressRequest;
import com.quickmart.dto.AddressResponse;
import com.quickmart.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CUSTOMER only (SecurityConfig). Always "my" addresses: the owner comes from the token.
// Admins read a customer's addresses through AdminController.
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return addressService.list(JwtClaims.customerId(jwt));
    }

    @GetMapping("/{id}")
    public AddressResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return addressService.get(JwtClaims.customerId(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest req) {
        return addressService.create(JwtClaims.customerId(jwt), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        addressService.delete(JwtClaims.customerId(jwt), id);
    }
}
