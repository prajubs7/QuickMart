package com.quickmart.controller;

import com.quickmart.dto.LoginRequest;
import com.quickmart.dto.RefreshTokenRequest;
import com.quickmart.dto.RegisterRequest;
import com.quickmart.dto.TokenResponse;
import com.quickmart.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// All four endpoints are public (SecurityConfig). login/register prove identity with a password,
// refresh/logout with the refresh token in the body.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    // Send the CURRENT refresh token, get a new pair. The old refresh token stops working.
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return authService.refresh(req.refreshToken());
    }

    // Ends the session (the refresh-token family). The access token still works until it expires
    // (max 15 min): the app must delete both tokens locally. Trade-off in ADR 0001, decision 4.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req.refreshToken());
    }
}
