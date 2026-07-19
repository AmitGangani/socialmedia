package com.example.socialmedia.user.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.example.socialmedia.user.domain.Account;
import com.example.socialmedia.user.persistence.AccountRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final String INVALID_CREDENTIALS = "Invalid login or password";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password");
    }

    @Transactional
    public PrivateAccount register(String emailInput, String usernameInput, String displayNameInput,
            String bioInput, String rawPassword) {
        String email = emailInput.trim();
        String username = usernameInput.trim();
        String displayName = displayNameInput.trim();
        String bio = bioInput == null ? "" : bioInput.trim();
        String normalizedEmail = normalize(email);
        String normalizedUsername = normalize(username);

        requireCodePoints(email, 3, 254, "Email must contain at most 254 Unicode code points");
        requireCodePoints(displayName, 1, 80,
                "Display name must contain between 1 and 80 Unicode code points");
        requireCodePoints(bio, 0, 160, "Bio must contain at most 160 Unicode code points");
        requirePasswordBytes(rawPassword);

        if (accountRepository.existsByNormalizedEmail(normalizedEmail)
                || accountRepository.existsByNormalizedUsername(normalizedUsername)) {
            throw identityConflict();
        }

        Account account = new Account(UuidCreator.getTimeOrderedEpoch(), email, normalizedEmail,
                username, normalizedUsername, displayName, bio,
                passwordEncoder.encode(rawPassword), Instant.now());
        try {
            return privateProjection(accountRepository.saveAndFlush(account));
        }
        catch (DataIntegrityViolationException exception) {
            throw identityConflict();
        }
    }

    @Transactional(readOnly = true)
    public UUID authenticate(String loginInput, String rawPassword) {
        String normalizedLogin = normalize(loginInput);
        Account account = accountRepository
                .findByNormalizedEmailOrNormalizedUsername(normalizedLogin, normalizedLogin)
                .orElse(null);
        String candidateHash = account == null ? dummyPasswordHash : account.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, candidateHash);
        if (account == null || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        return account.getId();
    }

    @Transactional(readOnly = true)
    public PrivateAccount getPrivateAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountService::privateProjection)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found"));
    }

    @Transactional(readOnly = true)
    public PublicAccount getPublicAccount(String username) {
        return accountRepository.findByNormalizedUsername(normalize(username))
                .map(AccountService::publicProjection)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Profile not found"));
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID accountId) {
        return accountRepository.existsById(accountId);
    }

    private static PrivateAccount privateProjection(Account account) {
        return new PrivateAccount(account.getId(), account.getEmail(), account.getUsername(),
                account.getDisplayName(), account.getBio(), account.getCreatedAt());
    }

    private static PublicAccount publicProjection(Account account) {
        return new PublicAccount(account.getId(), account.getUsername(), account.getDisplayName(),
                account.getBio());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireCodePoints(String value, int minimum, int maximum, String message) {
        int count = value.codePointCount(0, value.length());
        if (count < minimum || count > maximum) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static void requirePasswordBytes(String password) {
        int byteCount = password.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount < 12 || byteCount > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain between 12 and 72 UTF-8 bytes");
        }
    }

    private static ResponseStatusException identityConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Email or username is already registered");
    }

    public record PrivateAccount(UUID id, String email, String username, String displayName,
            String bio, Instant createdAt) {
    }

    public record PublicAccount(UUID id, String username, String displayName, String bio) {
    }
}
