package com.example.socialmedia.user.api;

import java.time.Instant;
import java.util.UUID;

import com.example.socialmedia.user.application.AccountService;
import com.example.socialmedia.user.integration.ProfileCountClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final AccountService accountService;
    private final ProfileCountClient profileCountClient;

    @GetMapping("/api/v1/users/me")
    AccountView ownAccount(@AuthenticationPrincipal Jwt jwt) {
        return AccountView.from(accountService.getPrivateAccount(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/api/v1/profiles/{username}")
    PublicProfile publicProfile(@PathVariable String username) {
        AccountService.PublicAccount account = accountService.getPublicAccount(username);
        ProfileCountClient.ProfileCounts counts = profileCountClient.getCounts(account.id());
        return new PublicProfile(account.id(), account.username(), account.displayName(), account.bio(),
                counts.followerCount(), counts.followingCount(), counts.postCount());
    }

    @GetMapping("/internal/v1/users/{userId}/exists")
    ExistsResult userExists(@PathVariable UUID userId) {
        return new ExistsResult(accountService.exists(userId));
    }

    record AccountView(UUID id, String email, String username, String displayName, String bio,
            Instant createdAt) {

        private static AccountView from(AccountService.PrivateAccount account) {
            return new AccountView(account.id(), account.email(), account.username(),
                    account.displayName(), account.bio(), account.createdAt());
        }
    }

    record PublicProfile(UUID id, String username, String displayName, String bio,
            long followerCount, long followingCount, long postCount) {
    }

    record ExistsResult(boolean exists) {
    }
}
