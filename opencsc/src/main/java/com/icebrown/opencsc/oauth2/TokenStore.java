package com.icebrown.opencsc.oauth2;

import com.icebrown.opencsc.model.response.OAuth2TokenResponse;

import java.util.Optional;

public interface TokenStore {
    void store(String key, OAuth2TokenResponse token);
    Optional<OAuth2TokenResponse> get(String key);
    void invalidate(String key);
    boolean isExpired(String key);
}
