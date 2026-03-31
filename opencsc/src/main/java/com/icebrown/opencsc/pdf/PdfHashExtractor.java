package com.icebrown.opencsc.pdf;

import com.icebrown.opencsc.crypto.HashAlgorithmRegistry;
import com.icebrown.opencsc.exception.CscSignatureException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.util.Calendar;

public class PdfHashExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfHashExtractor.class);
    private static final int DEFAULT_RESERVED_SPACE = 32768;

    private final HashAlgorithmRegistry hashAlgorithmRegistry;
    private final int reservedSpaceBytes;

    public PdfHashExtractor(HashAlgorithmRegistry hashAlgorithmRegistry, int reservedSpaceBytes) {
        this.hashAlgorithmRegistry = hashAlgorithmRegistry;
        this.reservedSpaceBytes = reservedSpaceBytes > 0 ? reservedSpaceBytes : DEFAULT_RESERVED_SPACE;
    }

    public PdfPreparationResult prepareAndExtractHash(byte[] inputPdf,
                                                       String hashAlgorithmOid,
                                                       String reason,
                                                       String location,
                                                       String contactInfo,
                                                       String signerName) {
        try {
            PDDocument document = Loader.loadPDF(inputPdf);
            ByteArrayOutputStream signedPdfOutputStream = new ByteArrayOutputStream();

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setSignDate(Calendar.getInstance());

            if (reason != null && !reason.isBlank()) {
                signature.setReason(reason);
            }
            if (location != null && !location.isBlank()) {
                signature.setLocation(location);
            }
            if (contactInfo != null && !contactInfo.isBlank()) {
                signature.setContactInfo(contactInfo);
            }
            if (signerName != null && !signerName.isBlank()) {
                signature.setName(signerName);
            }

            document.addSignature(signature);

            ExternalSigningSupport externalSigning = document.saveIncrementalForExternalSigning(signedPdfOutputStream);

            // Read content to be signed and compute hash
            InputStream content = externalSigning.getContent();
            MessageDigest digest = hashAlgorithmRegistry.createDigest(hashAlgorithmOid);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = content.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] dtbsHash = digest.digest();

            log.debug("Extracted PDF hash: {} bytes, algorithm={}", dtbsHash.length,
                    hashAlgorithmRegistry.toJcaName(hashAlgorithmOid));

            // Create handle for embedding signature later
            PdfPreparationResult.ExternalSigningHandle handle = new PdfPreparationResult.ExternalSigningHandle() {
                private byte[] signedPdf;

                @Override
                public void embedSignature(byte[] cmsSignature) throws IOException {
                    externalSigning.setSignature(cmsSignature);
                    document.close();
                    this.signedPdf = signedPdfOutputStream.toByteArray();
                }

                @Override
                public byte[] getSignedPdf() {
                    return signedPdf;
                }
            };

            return PdfPreparationResult.builder()
                    .dtbsHash(dtbsHash)
                    .hashAlgorithmOid(hashAlgorithmOid)
                    .signingHandle(handle)
                    .build();

        } catch (IOException e) {
            throw new CscSignatureException("Failed to prepare PDF for signing", e);
        }
    }
}
