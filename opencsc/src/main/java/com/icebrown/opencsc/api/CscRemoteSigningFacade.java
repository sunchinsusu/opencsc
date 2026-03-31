package com.icebrown.opencsc.api;

import com.icebrown.opencsc.model.request.*;
import com.icebrown.opencsc.model.response.*;
import com.icebrown.opencsc.pdf.PdfSigningRequest;
import com.icebrown.opencsc.pdf.PdfSigningResult;

public interface CscRemoteSigningFacade extends CscCredentialsOperations, CscSignatureOperations {

    /**
     * Orchestrates: credentials/info → credentials/authorize → signatures/signHash
     */
    default SignHashResponse authorizeAndSignHash(String credentialId,
                                                   java.util.List<String> hashes,
                                                   String hashAlgorithmOID,
                                                   String signAlgo) {
        return authorizeAndSignHash(credentialId, hashes, hashAlgorithmOID, signAlgo, null);
    }

    /**
     * Orchestrates: credentials/info → credentials/authorize → signatures/signHash
     *
     * @param clientData opaque value forwarded to credentials/authorize and signatures/signHash
     */
    SignHashResponse authorizeAndSignHash(String credentialId,
                                          java.util.List<String> hashes,
                                          String hashAlgorithmOID,
                                          String signAlgo,
                                          String clientData);

    /**
     * Full PDF signing: extract hash → authorize → sign → embed CMS into PDF
     */
    PdfSigningResult signPdf(PdfSigningRequest request);
}
