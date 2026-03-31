package com.icebrown.opencsc.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignHashRequest {
    @NotBlank
    private String credentialID;
    @JsonProperty("SAD")
    private String sad;
    @NotEmpty
    private List<String> hashes;
    private String hashAlgorithmOID;
    @NotBlank
    private String signAlgo;
    private String signAlgoParams;
    private String operationMode;
    @JsonProperty("validity_period")
    private Integer validityPeriod;
    @JsonProperty("response_uri")
    private String responseUri;
    private String clientData;
}
