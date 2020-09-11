package ru.demetriuzz.token.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

/**
 * Генерация JWT
 */
public class TokenGenerator {

    private static final String JKS_TYPE = "PKCS12";
    private static final String HASH_TYPE = "SHA-256";

    private KeyStore keyStore;
    private Key privateKey;
    private X509Certificate certificate;

    private String keyStorePath;
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    private void loadKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance(JKS_TYPE);
            try (FileInputStream in = new FileInputStream(keyStorePath)) {
                keyStore.load(in, keyStorePassword.toCharArray());
            }
        }
    }

    private PrivateKey getPrivateKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        if (privateKey == null) {
            privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray());
        }
        return (PrivateKey) privateKey;
    }

    private String getSHA256() throws KeyStoreException, NoSuchAlgorithmException, CertificateEncodingException {
        if (certificate == null) {
            certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        }
        MessageDigest mdSHA256 = MessageDigest.getInstance(HASH_TYPE);
        mdSHA256.update(certificate.getEncoded());
        return String.format("%032x", new BigInteger(1, mdSHA256.digest()));
    }

    /**
     * Генерация JWT для указанного пользователя и проекта.<br />
     * Дополнительно указывается дата и время генерации в формате Unix Timestamp (UTC).
     */
    public synchronized Token generateToken(String login,
                                            String project,
                                            String machine,
                                            long date) throws Exception {
        loadKeyStore();

        String token = JWT
                .create()
                .withClaim(TokenParameter.login.name(), login)
                .withClaim(TokenParameter.project.name(), project)
                .withClaim(TokenParameter.machine.name(), machine)
                .withClaim(TokenParameter.date.name(), date)
                .withClaim(TokenParameter.sha256.name(), getSHA256())
                .sign(Algorithm.RSA512(null, (RSAPrivateKey) getPrivateKey()));

        return new Token(token, date);
    }

}