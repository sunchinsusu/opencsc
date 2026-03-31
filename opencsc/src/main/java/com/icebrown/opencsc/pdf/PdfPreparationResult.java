package com.icebrown.opencsc.pdf;

import lombok.Builder;
import lombok.Data;

import java.io.OutputStream;

@Data
@Builder
public class PdfPreparationResult {
    private byte[] dtbsHash;
    private String hashAlgorithmOid;
    private ExternalSigningHandle signingHandle;

    public interface ExternalSigningHandle {
        void embedSignature(byte[] cmsSignature) throws java.io.IOException;
        byte[] getSignedPdf();
    }
}
