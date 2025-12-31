package com.eisoo.metadatamanage.web.util;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {
    private static final Logger log = LoggerFactory.getLogger(RSAUtil.class);
    private static final String RSA_ALGORITHM = "RSA";

    // 加密/解密密钥对
    private static final String ENCRYPT_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA22GOSQ1jeDhpdzxhJddS\n" +
            "f+U10F4Ivut7giYhchFAIJgRonMamDT86MSqQUc8DdTFdPGLm7M3GUKcsG1qbC3S\n" +
            "qk4XJ9NjmQXbs7IMWyWEWQrN7Iv7S2QjDYJI+ppvIN03I0Km3WKsmnrle2bLzT/V\n" +
            "G8e72YX69dfXAeiX6uDhht1va/JxZVFMIV3pHa6AQQ9gn5SAUTX2akEhRfe1bPJj\n" +
            "fVyoM+dfNtvgdfaraqV1rOhVDEqd0NlOWt2RHwETQwU8gIJib2baj2MtyIAY+fQw\n" +
            "KlKWxUs1GcFbECnhVPiVN6BEhXD7OhRt9QE/cuYl5v4a6ypugGaMBK6VKOqFHDvf\n" +
            "mwIDAQAB\n" +
            "-----END PUBLIC KEY-----";
    private static final String ENCRYPT_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEwAIBADANBgkqhkiG9w0BAQEFAASCBKowggSmAgEAAoIBAQDbYY5JDWN4OGl3\n" +
            "PGEl11J/5TXQXgi+63uCJiFyEUAgmBGicxqYNPzoxKpBRzwN1MV08YubszcZQpyw\n" +
            "bWpsLdKqThcn02OZBduzsgxbJYRZCs3si/tLZCMNgkj6mm8g3TcjQqbdYqyaeuV7\n" +
            "ZsvNP9Ubx7vZhfr119cB6Jfq4OGG3W9r8nFlUUwhXekdroBBD2CflIBRNfZqQSFF\n" +
            "97Vs8mN9XKgz51822+B19qtqpXWs6FUMSp3Q2U5a3ZEfARNDBTyAgmJvZtqPYy3I\n" +
            "gBj59DAqUpbFSzUZwVsQKeFU+JU3oESFcPs6FG31AT9y5iXm/hrrKm6AZowErpUo\n" +
            "6oUcO9+bAgMBAAECggEBAMXsiwlfeemBw60enWsdi8H1koqN/Af7vi9apXwbEicV\n" +
            "63sLq+e8jpyWqiBA226DEy6BqfnsQ36XuXP3EzfMU67wyzVUIxxwy5mgvkMRYwlO\n" +
            "lSCf3jVTf8h1TdBCupYE3vUB8jf0CVNKI3Yk9SQVPfhVSCZlGVjpxYJkTYNMJkyc\n" +
            "GMYAdZFCEV43mIm+ev4GaepR+d/syeXL/SZfFa0uEy8SFChrehRDhdVVkn+dRzeg\n" +
            "O6tbDkTFYtOpi+UI5obcGsVXEN3ZAZzaOKrB2TPwU1Ei5sIcWZhvKEfJkpiKIdpe\n" +
            "eLztYSaRB6gjCqhYQ3wzaJQCnoNVz+XqVaRTcPdZfBECgYEA+34Xo43WjhSdWR/P\n" +
            "laqleXfcwCmsF4Za+2qZjXLW//D2SXQylRv6hMAcVg7qCJM5a95X5VTr5H7pQNHN\n" +
            "ungE5Oi9lvYlZYb+pmG2wRn+/ufBs6OjwR6aDw/bsqeDHVjPeFIrxFPeXNllEHe1\n" +
            "xtZuhXvxFjDXqIwzQa2WijT8hjMCgYEA31Ag3lj9bAF7dBTJn8yRPhZX4v3I5N3x\n" +
            "H7G5XVj75cwMk4RB1s4WN/uLsuVDzXmG7NXjZ2c6kMYk658TTPKznQwhDx3Jq7Mh\n" +
            "HSJklWDtcPOioFZzFkikfqHseAWGf9s/HxBgieLa3IuGR9hEJ4EjDHa43UDXGQD2\n" +
            "90QGX7qlSPkCgYEAh9FL8N8LzQVjCJu+XqSe4t+RjxGyR64eeoLSVGp9pBE84ORo\n" +
            "4NAQVhrt8qfxShpAO3oDW+2ly2uiiogDo71nXzw2D031WkQySCajLNveM0lz+ZDZ\n" +
            "QdVF+/ZjfrMqgvHQcblmu4tTni8lfmQ3/h8V5u7Nf193SCYXFFQr5Y3CBrMCgYEA\n" +
            "wBgWXg3g2WKhBqbHFd4L5oOj0FAM2ssMGv5vfJwJ+4++FbtEQ3n95ORONHJBE+SB\n" +
            "KwOGXTGQUG8R3Vl2ac+wr9x6J52xGDC7wGsQaOr69RmvAAu9biLI1WGGn2vpWdyI\n" +
            "fLlCwfnR2LtwpCal4fGU66jItxKKtSh+SQ9MCFbuzUkCgYEAvUZaQDKmjdSbtR7J\n" +
            "yRXWfXPf0DpUqYKDzP40VoPcoQVGBmZAmq92yl1DMqFBfYCueCv1aA7Ozt+RFgyV\n" +
            "bMdUcJ0qzhKdCnEaonlpJPlnZkfATj5vOLs+nwfsmyO0iwcjA2zjJHmBZM+Xg+tl\n" +
            "enZgox36xuiZZrGQd0jXRt134QM=\n" +
            "-----END PRIVATE KEY-----";


    private static String cleanPemKey(String pemKey) {
        return pemKey.replaceAll("-----BEGIN (RSA )?(PUBLIC|PRIVATE) KEY-----", "")
                .replaceAll("-----END (RSA )?(PUBLIC|PRIVATE) KEY-----", "")
                .replaceAll("\\s+", "");
    }

    /**
     * RSA加密
     */
    public static String encrypt(String data) {
        try {
            String cleanPublicKey = cleanPemKey(ENCRYPT_PUBLIC_KEY);
            byte[] keyBytes = Base64.decodeBase64(cleanPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encryptedData);
        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new RuntimeException("RSA加密失败", e);
        }
    }

    /**
     * RSA解密
     */
    public static String decrypt(String encryptedData) {
        try {
            String cleanPrivateKey = cleanPemKey(ENCRYPT_PRIVATE_KEY);
            byte[] keyBytes = Base64.decodeBase64(cleanPrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            byte[] decodedData = Base64.decodeBase64(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("RSA解密失败", e);
        }
    }
}
