package com.icebrown.opencsc.crypto;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class HashAlgorithmRegistry {

    private static final Map<String, String> OID_TO_JCA = Map.of(
            "2.16.840.1.101.3.4.2.1", "SHA-256",
            "2.16.840.1.101.3.4.2.2", "SHA-384",
            "2.16.840.1.101.3.4.2.3", "SHA-512",
            "1.3.14.3.2.26", "SHA-1"
    );

    private static final Map<String, ASN1ObjectIdentifier> OID_TO_ASN1 = Map.of(
            "2.16.840.1.101.3.4.2.1", NISTObjectIdentifiers.id_sha256,
            "2.16.840.1.101.3.4.2.2", NISTObjectIdentifiers.id_sha384,
            "2.16.840.1.101.3.4.2.3", NISTObjectIdentifiers.id_sha512,
            "1.3.14.3.2.26", OIWObjectIdentifiers.idSHA1
    );

    public String toJcaName(String oid) {
        String name = OID_TO_JCA.get(oid);
        if (name == null) {
            throw new IllegalArgumentException("Unknown hash algorithm OID: " + oid);
        }
        return name;
    }

    public AlgorithmIdentifier toAlgorithmIdentifier(String oid) {
        ASN1ObjectIdentifier asn1Oid = OID_TO_ASN1.get(oid);
        if (asn1Oid == null) {
            throw new IllegalArgumentException("Unknown hash algorithm OID: " + oid);
        }
        return new AlgorithmIdentifier(asn1Oid);
    }

    public MessageDigest createDigest(String oid) {
        try {
            return MessageDigest.getInstance(toJcaName(oid));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported hash algorithm: " + oid, e);
        }
    }
}
