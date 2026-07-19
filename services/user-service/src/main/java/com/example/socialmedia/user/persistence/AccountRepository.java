package com.example.socialmedia.user.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.socialmedia.user.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByNormalizedEmailOrNormalizedUsername(
            String normalizedEmail, String normalizedUsername);

    Optional<Account> findByNormalizedUsername(String normalizedUsername);

    boolean existsByNormalizedEmail(String normalizedEmail);

    boolean existsByNormalizedUsername(String normalizedUsername);
}
