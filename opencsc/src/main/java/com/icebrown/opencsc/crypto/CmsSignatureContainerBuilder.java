package com.icebrown.opencsc.crypto;

import com.icebrown.opencsc.exception.CscSignatureException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

public class CmsSignatureContainerBuilder {

    private final HashAlgorithmRegistry hashAlgorithmRegistry;

    public CmsSignatureContainerBuilder(HashAlgorithmRegistry hashAlgorithmRegistry) {
        this.hashAlgorithmRegistry = hashAlgorithmRegistry;
    }

    /**
     * Builds a CMS/PKCS#7 SignedData container using a pre-computed signature from the CSC server.
     */
    public byte[] buildCmsContainer(byte[] signatureBytes,
                                     List<X509Certificate> certificateChain,
                                     String hashAlgorithmOid,
                                     byte[] signedContent) {
        try {
            X509Certificate signerCert = certificateChain.get(0);
            X509CertificateHolder certHolder = new X509CertificateHolder(signerCert.getEncoded());

            DigestCalculatorProvider digestCalcProvider = new JcaDigestCalculatorProviderBuilder()
                    .setProvider("BC")
                    .build();

            // Build a pre-computed content signer that returns the CSC-provided signature
            ContentSigner preComputedSigner = new PreComputedContentSigner(
                    hashAlgorithmOid, signatureBytes);

            JcaSignerInfoGeneratorBuilder signerInfoBuilder = new JcaSignerInfoGeneratorBuilder(digestCalcProvider);
            SignerInfoGenerator signerInfoGenerator = signerInfoBuilder.build(preComputedSigner, certHolder);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(signerInfoGenerator);
            generator.addCertificates(new JcaCertStore(certificateChain));

            CMSTypedData cmsContent = new CMSProcessableByteArray(signedContent);
            CMSSignedData signedData = generator.generate(cmsContent, false);

            return signedData.getEncoded();
        } catch (Exception e) {
            throw new CscSignatureException("Failed to build CMS container", e);
        }
    }

    /**
     * Parse Base64-encoded DER certificates from CSC response into X509Certificate objects.
     */
    public List<X509Certificate> parseCertificateChain(List<String> base64Certs) {
        List<X509Certificate> chain = new ArrayList<>();
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (String base64Cert : base64Certs) {
                byte[] certBytes = Base64.getDecoder().decode(base64Cert);
                X509Certificate cert = (X509Certificate) factory.generateCertificate(
                        new ByteArrayInputStream(certBytes));
                chain.add(cert);
            }
        } catch (CertificateException e) {
            throw new CscSignatureException("Failed to parse certificate chain", e);
        }
        return chain;
    }

    /**
     * A ContentSigner that uses pre-computed signature bytes from the remote CSC server.
     */
    private static class PreComputedContentSigner implements ContentSigner {
        private final ASN1ObjectIdentifier algorithmOid;
        private final byte[] signatureBytes;
        private final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

        PreComputedContentSigner(String hashAlgorithmOid, byte[] signatureBytes) {
            // Map hash OID to signature algorithm OID (RSA with SHA)
            this.algorithmOid = new ASN1ObjectIdentifier(hashAlgorithmOid);
            this.signatureBytes = signatureBytes;
        }

        @Override
        public org.bouncycastle.asn1.x509.AlgorithmIdentifier getAlgorithmIdentifier() {
            return new org.bouncycastle.asn1.x509.AlgorithmIdentifier(algorithmOid);
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public byte[] getSignature() {
            return signatureBytes;
        }
    }
}
