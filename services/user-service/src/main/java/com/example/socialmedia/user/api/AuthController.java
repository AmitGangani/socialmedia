package com.example.socialmedia.user.api;

import java.time.Instant;
import java.util.UUID;

import com.example.socialmedia.user.application.AccountService;
import com.example.socialmedia.user.application.JwtIssuer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final JwtIssuer jwtIssuer;

    @PostMapping("/register")
    ResponseEntity<AccountView> register(@Valid @RequestBody RegisterRequest request) {
        AccountService.PrivateAccount account = accountService.register(request.email(),
                request.username(), request.displayName(), request.bio(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountView.from(account));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        UUID accountId = accountService.authenticate(request.login(), request.password());
        JwtIssuer.IssuedToken issuedToken = jwtIssuer.issue(accountId);
        return new TokenResponse(issuedToken.accessToken(), "Bearer", issuedToken.expiresAt());
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 3, max = 30)
            @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
            @NotBlank @Size(max = 160) String displayName,
            @Size(max = 320) String bio,
            @NotNull String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    record LoginRequest(@NotBlank String login, @NotNull String password) {
    }

    record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {
    }

    record AccountView(UUID id, String email, String username, String displayName, String bio,
            Instant createdAt) {

        private static AccountView from(AccountService.PrivateAccount account) {
            return new AccountView(account.id(), account.email(), account.username(),
                    account.displayName(), account.bio(), account.createdAt());
        }
    }
}
