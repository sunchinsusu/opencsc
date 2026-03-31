package com.icebrown.opencsc.demo.controller;

import com.icebrown.opencsc.api.CscRemoteSigningFacade;
import com.icebrown.opencsc.model.request.CredentialsInfoRequest;
import com.icebrown.opencsc.model.request.CredentialsListRequest;
import com.icebrown.opencsc.model.response.CredentialsInfoResponse;
import com.icebrown.opencsc.model.response.CredentialsListResponse;
import com.icebrown.opencsc.model.response.SignHashResponse;
import com.icebrown.opencsc.pdf.PdfSigningRequest;
import com.icebrown.opencsc.pdf.PdfSigningResult;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SigningController {

    private final CscRemoteSigningFacade cscFacade;

    public SigningController(CscRemoteSigningFacade cscFacade) {
        this.cscFacade = cscFacade;
    }

    @GetMapping("/credentials")
    public CredentialsListResponse listCredentials(
            @RequestParam(required = false, name = "userID") String userID) {
        return cscFacade.listCredentials(
                CredentialsListRequest.builder()
                        .userID(userID)
                        .credentialInfo(true)
                        .certificates("chain")
                        .certInfo(true)
                        .authInfo(true)
                        .onlyValid(true)
                        .build());
    }

    @GetMapping("/credentials/{credentialID}")
    public CredentialsInfoResponse getCredentialInfo(
            @PathVariable String credentialID) {
        return cscFacade.getCredentialInfo(
                CredentialsInfoRequest.builder()
                        .credentialID(credentialID)
                        .certificates("chain")
                        .certInfo(true)
                        .authInfo(true)
                        .build());
    }

    @PostMapping("/sign-hash")
    public SignHashResponse signHash(@RequestBody SignHashDemoRequest request) {
        return cscFacade.authorizeAndSignHash(
                request.getCredentialID(),
                request.getHashes(),
                request.getHashAlgorithmOID(),
                request.getSignAlgo(),
                request.getClientData());
    }

    @PostMapping("/sign-pdf")
    public ResponseEntity<byte[]> signPdf(
            @RequestParam String credentialID,
            @RequestParam(required = false) String signAlgo,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String clientData,
            @RequestPart("file") MultipartFile file) throws IOException {

        PdfSigningResult result = cscFacade.signPdf(
                PdfSigningRequest.builder()
                        .pdfBytes(file.getBytes())
                        .credentialID(credentialID)
                        .signAlgo(signAlgo)
                        .reason(reason != null ? reason : "Digitally signed")
                        .location(location)
                        .clientData(clientData)
                        .build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("signed_" + file.getOriginalFilename())
                .build());

        return new ResponseEntity<>(result.getSignedPdfBytes(), headers, HttpStatus.OK);
    }

    @Data
    public static class SignHashDemoRequest {
        private String credentialID;
        private List<String> hashes;
        private String hashAlgorithmOID;
        private String signAlgo;
        private String clientData;
    }
}
