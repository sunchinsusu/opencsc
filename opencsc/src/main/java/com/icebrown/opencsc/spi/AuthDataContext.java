package com.icebrown.opencsc.spi;

import com.icebrown.opencsc.domain.AuthObjectType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AuthDataContext {
    String credentialId;
    List<AuthObjectType> requiredObjects;
    String correlationId;
    List<String> hashes;
    String hashAlgorithmOID;
    String signAlgo;
    String clientData;
}
