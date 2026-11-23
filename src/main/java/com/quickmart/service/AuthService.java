package com.quickmart.service;

import com.quickmart.dto.LoginRequest;
import com.quickmart.dto.RegisterRequest;
import com.quickmart.dto.TokenResponse;
import com.quickmart.entity.Customer;
import com.quickmart.entity.User;
import com.quickmart.exception.BadRequestException;
import com.quickmart.exception.DuplicateResourceException;
import com.quickmart.repository.CustomerRepository;
import com.quickmart.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    // One message for "no such email" AND "wrong password". Two different messages tell an attacker
    // which emails have accounts (user enumeration).
    private static final String BAD_LOGIN = "Invalid email or password";
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    // A real BCrypt hash we compare against when the email doesn't exist. Without it, "unknown email"
    // returns in ~1 ms and "wrong password" in ~250 ms: the response time leaks which emails exist.
    private final String dummyHash;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // customers row + users row + first session, all or nothing.
    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (tooLongForBcrypt(req.password())) {
            throw new BadRequestException("Password must be at most " + BCRYPT_MAX_BYTES + " bytes");
        }
        // Friendly 409s. Two racing registrations can both pass these checks; the unique
        // indexes then reject the second insert and GlobalExceptionHandler turns that into 409 too.
        if (userRepository.existsByEmail(email) || customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (customerRepository.existsByPhone(req.phone())) {
            throw new DuplicateResourceException("Phone is already registered");
        }

        Customer customer = customerRepository.save(new Customer(req.name().trim(), email, req.phone()));
        User user = userRepository.save(
                User.customer(email, passwordEncoder.encode(req.password()), customer.getId()));
        log.info("Registered user {} (customer {})", user.getId(), customer.getId());
        return tokensFor(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        Optional<User> found = userRepository.findByEmail(normalize(req.email()));
        if (found.isEmpty() || tooLongForBcrypt(req.password())) {
            // Burn the same BCrypt time as a real check. The input doesn't matter: the cost is fixed by the hash.
            passwordEncoder.matches("not-the-password", dummyHash);
            throw new BadCredentialsException(BAD_LOGIN);
        }
        User user = found.get();
        // Check the password BEFORE "enabled": otherwise a disabled account answers fast and
        // differently, which again tells an attacker the email exists.
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()) || !user.isEnabled()) {
            log.info("Failed login for user {}", user.getId());   // id, never the email or password
            throw new BadCredentialsException(BAD_LOGIN);
        }
        return tokensFor(user);
    }

    // Joins RefreshTokenService.rotate's transaction. noRollbackFor keeps a reuse revocation committed.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse refresh(String refreshToken) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(rotated.userId()).orElseThrow();
        if (!user.isEnabled()) {
            // The account was disabled after login: end the session instead of extending it.
            refreshTokenService.endSession(rotated.familyId());
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        // Fresh access token from the CURRENT user row, so role changes apply at the next refresh.
        return TokenResponse.bearer(jwtTokenService.issueAccessToken(user), rotated.refreshToken(),
                jwtTokenService.accessTokenTtlSeconds());
    }

    public void logout(String refreshToken) {
        refreshTokenService.endSession(refreshToken);
    }

    private TokenResponse tokensFor(User user) {
        return TokenResponse.bearer(jwtTokenService.issueAccessToken(user),
                refreshTokenService.startSession(user.getId()),
                jwtTokenService.accessTokenTtlSeconds());
    }

    // Emails are case-insensitive in practice. Locale.ROOT: in a Turkish locale "I".toLowerCase() is "ı".
    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean tooLongForBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
