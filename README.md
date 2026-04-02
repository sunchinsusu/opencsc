# OpenCSC - Java Spring Boot Library for CSC v2 Remote Signing

A Spring Boot library that provides a client for **Cloud Signature Consortium (CSC) API v2**, enabling remote digital signing through any CSC-compliant server.

## Features

- CSC API v2 client (`credentials/list`, `credentials/info`, `credentials/authorize`, `credentials/getChallenge`, `signatures/signHash`)
- OAuth2 `client_credentials` flow with automatic token management and proactive refresh
- Hash signing (single and batch)
- PDF signing via Apache PDFBox (two-phase external signing)
- CMS/PKCS#7 signature container generation via Bouncy Castle
- SCAL1/SCAL2 automatic handling
- SAD (Signature Activation Data) lifecycle management
- Audit event hooks (pluggable listener interface)
- Spring Boot auto-configuration with `@ConfigurationProperties`
- Retry support (configurable)

## Requirements

- Java 17+
- Spring Boot 3.x

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>com.icebrown.opencsc</groupId>
    <artifactId>opencsc</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- For PDF signing, also add: -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

### 2. Configure `application.yml`

```yaml
opencsc:
  base-url: "https://your-csc-server.com/csc/v2"

  # Optional: separate Authorization Server
  # auth-server-url: "https://auth.your-server.com"

  oauth2:
    token-path: "/oauth2/token"
    client-id: "your-client-id"
    client-secret: "your-client-secret"
    scope: "service"

  pdf:
    enabled: true
```

### 3. Implement `AuthDataProvider`

Required when your credential uses explicit authorization (PIN/OTP):

```java
@Bean
public AuthDataProvider authDataProvider() {
    return context -> {
        // context contains all signing context:
        // context.getCredentialId()      — credential being authorized
        // context.getRequiredObjects()   — auth types required by the credential (PIN, OTP, etc.)
        // context.getCorrelationId()     — correlation ID for tracing
        // context.getHashes()            — hashes to be signed
        // context.getHashAlgorithmOID()  — hash algorithm OID
        // context.getSignAlgo()          — signature algorithm OID
        // context.getClientData()        — optional client data

        return List.of(new AuthObject("PIN", "123456"));
    };
}
```

### 4. Use the facade

```java
@Autowired
CscRemoteSigningFacade cscFacade;

// List credentials
CredentialsListResponse creds = cscFacade.listCredentials(
    CredentialsListRequest.builder().authInfo(true).build());

// Sign hash (authorize + sign in one call)
SignHashResponse result = cscFacade.authorizeAndSignHash(
    "credentialId",
    List.of("base64EncodedHash"),
    "2.16.840.1.101.3.4.2.1",  // SHA-256
    "1.2.840.113549.1.1.11",   // SHA256withRSA
    "optional-client-data"      // clientData forwarded to authorize & signHash
);

// Sign PDF
PdfSigningResult pdfResult = cscFacade.signPdf(
    PdfSigningRequest.builder()
        .pdfBytes(pdfBytes)
        .credentialID("credentialId")
        .reason("Contract signing")
        .clientData("optional-client-data")
        .build());
byte[] signedPdf = pdfResult.getSignedPdfBytes();
```

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `opencsc.base-url` | (required) | CSC Resource Server base URL |
| `opencsc.auth-server-url` | `null` (falls back to `base-url`) | OAuth2 Authorization Server URL |
| `opencsc.connect-timeout` | `5s` | HTTP connection timeout |
| `opencsc.read-timeout` | `30s` | HTTP read timeout |
| `opencsc.oauth2.token-path` | `/oauth2/token` | Token endpoint path (appended to `auth-server-url` or `base-url`) |
| `opencsc.oauth2.client-id` | (required) | OAuth2 client ID |
| `opencsc.oauth2.client-secret` | (required) | OAuth2 client secret |
| `opencsc.oauth2.scope` | `null` | OAuth2 scope |
| `opencsc.oauth2.token-expiry-skew` | `30` | Seconds before expiry to trigger refresh |
| `opencsc.oauth2.refresh-check-interval` | `30000` | Milliseconds between proactive refresh checks |
| `opencsc.credentials.list-path` | `/credentials/list` | Credentials list endpoint path |
| `opencsc.credentials.info-path` | `/credentials/info` | Credentials info endpoint path |
| `opencsc.credentials.authorize-path` | `/credentials/authorize` | Credentials authorize endpoint path |
| `opencsc.credentials.get-challenge-path` | `/credentials/getChallenge` | Get challenge endpoint path |
| `opencsc.signatures.sign-hash-path` | `/signatures/signHash` | Sign hash endpoint path |
| `opencsc.retry.enabled` | `true` | Enable retry on failed API calls |
| `opencsc.retry.max-attempts` | `3` | Maximum retry attempts |
| `opencsc.retry.wait-duration` | `500ms` | Wait between retries |
| `opencsc.circuit-breaker.enabled` | `true` | Enable circuit breaker |
| `opencsc.circuit-breaker.failure-rate-threshold` | `50` | Failure rate % to open circuit |
| `opencsc.pdf.enabled` | `true` | Enable PDF signing (requires PDFBox on classpath) |
| `opencsc.pdf.default-hash-algorithm` | `2.16.840.1.101.3.4.2.1` | Default hash algorithm OID (SHA-256) |
| `opencsc.pdf.signature-reserved-space-bytes` | `32768` | Reserved bytes for CMS container in PDF |

---

## Authorization Server Configuration

By default, the OAuth2 token endpoint is resolved as:
```
{base-url} + {oauth2.token-path}
```

If your CSC server uses a **separate Authorization Server** (e.g., Keycloak, dedicated IAM):
```yaml
opencsc:
  base-url: "https://csc.example.com/csc/v2"           # Resource Server
  auth-server-url: "https://auth.example.com"           # Authorization Server
  oauth2:
    token-path: "/realms/csc/protocol/openid-connect/token"
```

Token endpoint becomes: `https://auth.example.com/realms/csc/protocol/openid-connect/token`

---

## Audit Events

Implement `CscAuditListener` to receive events:

```java
@Bean
public CscAuditListener myAuditListener() {
    return event -> {
        switch (event) {
            case TokenAcquiredEvent e -> log.info("Token acquired, expires_in={}s", e.expiresIn());
            case CredentialAuthorizedEvent e -> log.info("Credential {} authorized", e.credentialId());
            case HashSignedEvent e -> log.info("Signed {} hashes for {}", e.hashCount(), e.credentialId());
            case PdfSignedEvent e -> log.info("PDF signed for {}, size={}", e.credentialId(), e.pdfSizeBytes());
            case CscApiErrorEvent e -> log.error("API error on {}: {}", e.endpoint(), e.errorMessage());
            default -> log.info("CSC event: {}", event);
        }
    };
}
```

Available events:
- `TokenAcquiredEvent` — OAuth2 token obtained
- `CredentialAuthorizedEvent` — Credential authorization (SAD) obtained
- `HashSignedEvent` — Hash(es) signed successfully
- `PdfSignedEvent` — PDF signed successfully
- `CscApiErrorEvent` — CSC API returned an error

---

## PDF Signing

PDF signing uses Apache PDFBox's **ExternalSigningSupport** for a two-phase approach:

1. **Prepare**: Open PDF, add signature field, reserve space, compute hash of content to be signed
2. **Sign**: Send hash to CSC server → get signature → build CMS container → embed into PDF

PDFBox is an **optional** dependency. Add it to your project only if you need PDF signing:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              Your Application               │
│         (implements AuthDataProvider)        │
└────────────────────┬────────────────────────┘
                     │ @Autowired
                     ▼
┌─────────────────────────────────────────────┐
│         CscRemoteSigningFacade              │
│  ┌──────────────────────────────────────┐   │
│  │ authorizeAndSignHash()               │   │
│  │   1. credentials/info                │   │
│  │   2. AuthDataProvider.provideAuthData│   │
│  │   3. credentials/authorize → SAD     │   │
│  │   4. signatures/signHash             │   │
│  ├──────────────────────────────────────┤   │
│  │ signPdf()                            │   │
│  │   1. PdfHashExtractor (PDFBox)       │   │
│  │   2. authorizeAndSignHash()          │   │
│  │   3. CmsSignatureContainerBuilder    │   │
│  │   4. Embed CMS into PDF             │   │
│  └──────────────────────────────────────┘   │
└────────────────────┬────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│  OAuth2  │  │  CSC API  │  │  Audit   │
│  Token   │  │  Client   │  │Publisher │
│  Manager │  │(RestClient│  │          │
└──────────┘  └──────────┘  └──────────┘
        │            │
        ▼            ▼
   ┌─────────────────────┐
   │   CSC Server (RS)   │
   └─────────────────────┘
   ┌─────────────────────┐
   │ Auth Server (AS)    │  ← optional, separate
   └─────────────────────┘
```

---

## Running the Demo

```bash
cd opencsc-demo
# Edit src/main/resources/application.yml with your CSC server config
mvn spring-boot:run
```

Test endpoints:
```bash
# List credentials
curl http://localhost:8080/api/v1/credentials

# Get credential info
curl http://localhost:8080/api/v1/credentials/{credentialID}

# Sign hash
curl -X POST http://localhost:8080/api/v1/sign-hash \
  -H "Content-Type: application/json" \
  -d '{
    "credentialID": "your-credential-id",
    "hashes": ["base64EncodedHash"],
    "hashAlgorithmOID": "2.16.840.1.101.3.4.2.1",
    "signAlgo": "1.2.840.113549.1.1.11"
  }'

# Sign PDF
curl -X POST http://localhost:8080/api/v1/sign-pdf \
  -F "credentialID=your-credential-id" \
  -F "file=@document.pdf" \
  -o signed_document.pdf
```

## License

Apache License 2.0
