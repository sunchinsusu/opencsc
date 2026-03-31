package com.icebrown.opencsc.spi;

import com.icebrown.opencsc.domain.AuthObject;

import java.util.List;

@FunctionalInterface
public interface AuthDataProvider {
    List<AuthObject> provideAuthData(AuthDataContext context);
}
