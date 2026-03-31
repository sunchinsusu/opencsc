# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenCSC is a **Spring Boot library** (not an application) that provides a CSC API v2 client for remote digital signing. It is structured as a Maven multi-module project:

- `opencsc/` — the library itself (published as `com.icebrown.opencsc:opencsc`)
- `opencsc-demo/` — a Spring Boot REST app that demonstrates usage of the library

## Build & Test Commands

```bash
# Build everything (from repo root)
mvn clean install

# Build library only
cd opencsc && mvn clean install

# Run demo application
cd opencsc-demo && mvn spring-boot:run

# Run tests for a specific module
cd opencsc && mvn test

# Run a single test class
cd opencsc && mvn test -Dtest=ClassName

# Skip tests during build
mvn clean install -DskipTests
```

There are currently no test sources under `opencsc/src/test/`.

## Architecture

### Auto-Configuration Entry Point
`CscAutoConfiguration` (`autoconfigure/`) is the Spring Boot auto-configuration class, registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. It wires all beans conditionally. The entire library activates only when `opencsc.base-url` is set.

### Key Layers

| Layer | Package | Role |
|---|---|---|
| **Facade** | `api/`, `facade/` | `CscRemoteSigningFacade` (interface) + `CscRemoteSigningFacadeImpl` (impl) — the primary entry point for callers |
| **API Client** | `client/` | `CscApiClient` — low-level HTTP calls to CSC endpoints; implements `CscCredentialsOperations` + `CscSignatureOperations` |
| **OAuth2** | `oauth2/` | `OAuth2ClientCredentialsManager` + `InMemoryTokenStore` — token acquisition and proactive refresh via `@Scheduled` |
| **Crypto** | `crypto/` | `CmsSignatureContainerBuilder` (Bouncy Castle CMS/PKCS#7), `HashAlgorithmRegistry` |
| **PDF** | `pdf/` | `PdfHashExtractor` (PDFBox two-phase signing), optional via `@ConditionalOnClass` |
| **SPI** | `spi/` | `AuthDataProvider` (PIN/OTP supply), `CscAuditListener` / `AuditPublisher` (audit events) |
| **Config** | `autoconfigure/CscProperties` | `@ConfigurationProperties(prefix = "opencsc")` — all configuration |

### Key Design Patterns

- **PDFBox is optional**: The `pdf/` package beans are only created when `org.apache.pdfbox.pdmodel.PDDocument` is on the classpath and `opencsc.pdf.enabled=true`.
- **Resilience4j is optional**: Retry/circuit-breaker support is `optional` scope in the library POM.
- **SCAL1/SCAL2 handling**: `CscRemoteSigningFacadeImpl.authorizeAndSignHash()` checks `credInfo.getScal()` to include hashes in authorize request only for SCAL2 credentials.
- **Separate Auth Server**: `opencsc.auth-server-url` can point to a separate OAuth2 AS (e.g., Keycloak); otherwise the token endpoint is resolved against `base-url`.

### PDF Signing Flow (Two-Phase)
1. `PdfHashExtractor.prepareAndExtractHash()` — opens PDF, adds signature field, reserves space, returns content hash
2. `authorizeAndSignHash()` — sends hash to CSC server, gets raw signature bytes
3. `CmsSignatureContainerBuilder.buildCmsContainer()` — wraps raw signature in CMS/PKCS#7 container with cert chain
4. `preparation.getSigningHandle().embedSignature(cmsContainer)` — embeds CMS into reserved PDF space

## Demo App

The demo (`opencsc-demo/`) exposes REST endpoints at `http://localhost:8080/api/v1/`:
- `GET /credentials` — list credentials
- `GET /credentials/{credentialID}` — credential info
- `POST /sign-hash` — sign a hash
- `POST /sign-pdf` — sign a PDF (multipart)

Configure the demo in `opencsc-demo/src/main/resources/application.yml` before running.

## SPI Extension Points

- `AuthDataProvider` — implement and register as `@Bean` to supply PIN/OTP for explicit-mode credentials
- `CscAuditListener` — implement and register as `@Bean` to receive audit events (`TokenAcquiredEvent`, `CredentialAuthorizedEvent`, `HashSignedEvent`, `PdfSignedEvent`, `CscApiErrorEvent`)
