package com.icebrown.opencsc.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PdfSigningResult {
    private byte[] signedPdfBytes;
    private String signerSubjectDN;
    private String signatureTimestamp;
}
