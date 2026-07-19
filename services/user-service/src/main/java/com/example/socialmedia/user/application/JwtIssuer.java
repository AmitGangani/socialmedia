package com.example.socialmedia.user.application;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtIssuer {

    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public JwtIssuer(@Value("${security.jwt.private-key-location}") Resource privateKeyResource,
            @Value("${security.jwt.public-key-location}") Resource publicKeyResource,
            @Value("${security.jwt.issuer}") String issuer) throws IOException {
        RSAPrivateKey privateKey = (RSAPrivateKey) RsaKeyConverters.pkcs8()
                .convert(privateKeyResource.getInputStream());
        RSAPublicKey publicKey = (RSAPublicKey) RsaKeyConverters.x509()
                .convert(publicKeyResource.getInputStream());
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        ImmutableJWKSet<SecurityContext> keySource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        this.jwtEncoder = new NimbusJwtEncoder(keySource);
        this.issuer = issuer;
    }

    public IssuedToken issue(UUID accountId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(TOKEN_LIFETIME);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountId.toString())
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UuidCreator.getTimeOrderedEpoch().toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String accessToken, Instant expiresAt) {
    }
}
