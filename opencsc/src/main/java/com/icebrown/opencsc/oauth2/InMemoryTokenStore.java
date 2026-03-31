package com.icebrown.opencsc.oauth2;

import com.icebrown.opencsc.model.response.OAuth2TokenResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTokenStore implements TokenStore {

    private final Map<String, TimestampedToken> store = new ConcurrentHashMap<>();
    private final int expirySkewSeconds;

    public InMemoryTokenStore(int expirySkewSeconds) {
        this.expirySkewSeconds = expirySkewSeconds;
    }

    @Override
    public void store(String key, OAuth2TokenResponse token) {
        store.put(key, new TimestampedToken(token, Instant.now()));
    }

    @Override
    public Optional<OAuth2TokenResponse> get(String key) {
        TimestampedToken entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(entry.token());
    }

    @Override
    public void invalidate(String key) {
        store.remove(key);
    }

    @Override
    public boolean isExpired(String key) {
        TimestampedToken entry = store.get(key);
        if (entry == null) {
            return true;
        }
        if (entry.token().getExpiresIn() == null) {
            return false;
        }
        Instant expiresAt = entry.storedAt()
                .plusSeconds(entry.token().getExpiresIn())
                .minusSeconds(expirySkewSeconds);
        return Instant.now().isAfter(expiresAt);
    }

    private record TimestampedToken(OAuth2TokenResponse token, Instant storedAt) {
    }
}
