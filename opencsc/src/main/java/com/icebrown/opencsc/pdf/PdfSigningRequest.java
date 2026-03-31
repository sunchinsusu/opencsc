package com.icebrown.opencsc.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PdfSigningRequest {
    private byte[] pdfBytes;
    private String credentialID;
    private String hashAlgorithmOID;
    private String signAlgo;
    private String reason;
    private String location;
    private String contactInfo;
    private String signerName;
    private Integer signatureFieldPage;
    private Float signatureFieldX;
    private Float signatureFieldY;
    private Float signatureFieldWidth;
    private Float signatureFieldHeight;
    private String clientData;
}
