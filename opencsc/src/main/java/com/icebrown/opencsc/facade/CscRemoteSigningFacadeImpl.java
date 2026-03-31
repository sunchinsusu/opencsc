package com.icebrown.opencsc.facade;

import com.icebrown.opencsc.api.CscCredentialsOperations;
import com.icebrown.opencsc.api.CscRemoteSigningFacade;
import com.icebrown.opencsc.api.CscSignatureOperations;
import com.icebrown.opencsc.client.CscApiClient;
import com.icebrown.opencsc.crypto.CmsSignatureContainerBuilder;
import com.icebrown.opencsc.exception.CscSignatureException;
import com.icebrown.opencsc.model.request.*;
import com.icebrown.opencsc.model.response.*;
import com.icebrown.opencsc.pdf.*;
import com.icebrown.opencsc.spi.AuditPublisher;
import com.icebrown.opencsc.spi.AuthDataContext;
import com.icebrown.opencsc.spi.AuthDataProvider;
import com.icebrown.opencsc.spi.event.CredentialAuthorizedEvent;
import com.icebrown.opencsc.spi.event.HashSignedEvent;
import com.icebrown.opencsc.spi.event.PdfSignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CscRemoteSigningFacadeImpl implements CscRemoteSigningFacade {

    private static final Logger log = LoggerFactory.getLogger(CscRemoteSigningFacadeImpl.class);

    private final CscApiClient apiClient;
    private final AuthDataProvider authDataProvider;
    private final AuditPublisher auditPublisher;
    private final CmsSignatureContainerBuilder cmsBuilder;
    private final PdfHashExtractor pdfHashExtractor;

    public CscRemoteSigningFacadeImpl(CscApiClient apiClient,
                                       AuthDataProvider authDataProvider,
                                       AuditPublisher auditPublisher,
                                       CmsSignatureContainerBuilder cmsBuilder,
                                       PdfHashExtractor pdfHashExtractor) {
        this.apiClient = apiClient;
        this.authDataProvider = authDataProvider;
        this.auditPublisher = auditPublisher;
        this.cmsBuilder = cmsBuilder;
        this.pdfHashExtractor = pdfHashExtractor;
    }

    // === Delegate low-level operations to apiClient ===

    @Override
    public CredentialsListResponse listCredentials(CredentialsListRequest request) {
        return apiClient.listCredentials(request);
    }

    @Override
    public CredentialsInfoResponse getCredentialInfo(CredentialsInfoRequest request) {
        return apiClient.getCredentialInfo(request);
    }

    @Override
    public CredentialsAuthorizeResponse authorizeCredential(CredentialsAuthorizeRequest request) {
        return apiClient.authorizeCredential(request);
    }

    @Override
    public CredentialsGetChallengeResponse getChallenge(CredentialsGetChallengeRequest request) {
        return apiClient.getChallenge(request);
    }

    @Override
    public SignHashResponse signHash(SignHashRequest request) {
        return apiClient.signHash(request);
    }

    // === High-level orchestrated operations ===

    @Override
    public SignHashResponse authorizeAndSignHash(String credentialId,
                                                  List<String> hashes,
                                                  String hashAlgorithmOID,
                                                  String signAlgo,
                                                  String clientData) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting authorizeAndSignHash for credential={}, hashes={}, corrId={}",
                credentialId, hashes.size(), correlationId);

        // Step 1: Get credential info
        CredentialsInfoResponse credInfo = apiClient.getCredentialInfo(
                CredentialsInfoRequest.builder()
                        .credentialID(credentialId)
                        .certificates("chain")
                        .certInfo(true)
                        .authInfo(true)
                        .build());

        // Step 2: Resolve sign algorithm if not provided
        String resolvedSignAlgo = signAlgo;
        if (resolvedSignAlgo == null || resolvedSignAlgo.isBlank()) {
            if (credInfo.getKey() != null && credInfo.getKey().getAlgo() != null
                    && !credInfo.getKey().getAlgo().isEmpty()) {
                resolvedSignAlgo = credInfo.getKey().getAlgo().get(0);
            } else {
                throw new CscSignatureException("No signAlgo provided and credential has no key algorithms");
            }
        }

        // Step 3: Authorize credential (get SAD)
        CredentialsAuthorizeRequest.CredentialsAuthorizeRequestBuilder authReqBuilder =
                CredentialsAuthorizeRequest.builder()
                        .credentialID(credentialId)
                        .numSignatures(hashes.size());

        // SCAL2 requires hashes in authorize request
        if ("2".equals(credInfo.getScal())) {
            authReqBuilder.hashes(hashes);
            authReqBuilder.hashAlgorithmOID(hashAlgorithmOID);
        }

        // Provide auth data if mode is explicit
        if (credInfo.getAuth() != null && "explicit".equals(credInfo.getAuth().getMode())) {
            var ctx = AuthDataContext.builder()
                    .credentialId(credentialId)
                    .requiredObjects(credInfo.getAuth().getObjects())
                    .correlationId(correlationId)
                    .hashes(hashes)
                    .hashAlgorithmOID(hashAlgorithmOID)
                    .signAlgo(resolvedSignAlgo)
                    .clientData(clientData)
                    .build();
            authReqBuilder.authData(authDataProvider.provideAuthData(ctx));
        }

        if (clientData != null) {
            authReqBuilder.clientData(clientData);
        }

        CredentialsAuthorizeResponse authResponse = apiClient.authorizeCredential(authReqBuilder.build());
        log.debug("Got SAD for credential={}, expiresIn={}s", credentialId, authResponse.getExpiresIn());

        auditPublisher.publish(new CredentialAuthorizedEvent(
                Instant.now(), correlationId, credentialId, hashes.size()));

        // Step 4: Sign hash
        SignHashRequest signRequest = SignHashRequest.builder()
                .credentialID(credentialId)
                .sad(authResponse.getSad())
                .hashes(hashes)
                .hashAlgorithmOID(hashAlgorithmOID)
                .signAlgo(resolvedSignAlgo)
                .clientData(clientData)
                .build();

        SignHashResponse signResponse = apiClient.signHash(signRequest);
        log.info("Successfully signed {} hash(es) for credential={}", hashes.size(), credentialId);

        auditPublisher.publish(new HashSignedEvent(
                Instant.now(), correlationId, credentialId, hashes.size()));

        return signResponse;
    }

    @Override
    public PdfSigningResult signPdf(PdfSigningRequest request) {
        if (pdfHashExtractor == null) {
            throw new CscSignatureException(
                    "PDF signing is not available. Ensure PDFBox is on the classpath and opencsc.pdf.enabled=true");
        }

        String correlationId = UUID.randomUUID().toString();
        log.info("Starting PDF signing for credential={}, corrId={}", request.getCredentialID(), correlationId);

        String hashAlgoOid = request.getHashAlgorithmOID() != null
                ? request.getHashAlgorithmOID()
                : "2.16.840.1.101.3.4.2.1"; // default SHA-256

        // Phase 1: Prepare PDF and extract hash
        PdfPreparationResult preparation = pdfHashExtractor.prepareAndExtractHash(
                request.getPdfBytes(),
                hashAlgoOid,
                request.getReason(),
                request.getLocation(),
                request.getContactInfo(),
                request.getSignerName());

        String base64Hash = Base64.getEncoder().encodeToString(preparation.getDtbsHash());

        // Phase 2: Authorize and sign the hash
        SignHashResponse signResponse = authorizeAndSignHash(
                request.getCredentialID(),
                List.of(base64Hash),
                hashAlgoOid,
                request.getSignAlgo(),
                request.getClientData());

        if (signResponse.getSignatures() == null || signResponse.getSignatures().isEmpty()) {
            throw new CscSignatureException("No signatures returned from CSC server");
        }

        // Phase 3: Get certificate chain and build CMS container
        CredentialsInfoResponse credInfo = apiClient.getCredentialInfo(
                CredentialsInfoRequest.builder()
                        .credentialID(request.getCredentialID())
                        .certificates("chain")
                        .certInfo(true)
                        .build());

        byte[] rawSignature = Base64.getDecoder().decode(signResponse.getSignatures().get(0));
        List<X509Certificate> certChain = cmsBuilder.parseCertificateChain(
                credInfo.getCert().getCertificates());

        byte[] cmsContainer = cmsBuilder.buildCmsContainer(
                rawSignature,
                certChain,
                hashAlgoOid,
                preparation.getDtbsHash());

        // Phase 4: Embed CMS into PDF
        try {
            preparation.getSigningHandle().embedSignature(cmsContainer);
        } catch (Exception e) {
            throw new CscSignatureException("Failed to embed signature into PDF", e);
        }

        byte[] signedPdf = preparation.getSigningHandle().getSignedPdf();
        String subjectDN = certChain.isEmpty() ? "" : certChain.get(0).getSubjectX500Principal().getName();

        auditPublisher.publish(new PdfSignedEvent(
                Instant.now(), correlationId, request.getCredentialID(), signedPdf.length));

        log.info("PDF signing completed for credential={}, signedPdfSize={} bytes",
                request.getCredentialID(), signedPdf.length);

        return PdfSigningResult.builder()
                .signedPdfBytes(signedPdf)
                .signerSubjectDN(subjectDN)
                .build();
    }
}
