package com.icebrown.opencsc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertInfo {
    private String status;
    private List<String> certificates;
    private String issuerDN;
    private String serialNumber;
    private String subjectDN;
    private String validFrom;
    private String validTo;
}
