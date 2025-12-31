package com.eisoo.util;

import sun.security.util.Pem;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
 
public class PasswordEncoder {
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final String ALGORITHM_NAME = "RSA";
    private static final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4E+eiWRwffhRIPQYvlXU" +
            "jf0b3HqCmosiCxbFCYI/gdfDBhrTUzbt3fL3o/gRQQBEPf69vhJMFH2ZMtaJM6oh" +
            "E3yQef331liPVM0YvqMOgvoID+zDa1NIZFObSsjOKhvZtv9esO0REeiVEPKNc+Dp" +
            "6il3x7TV9VKGEv0+iriNjqv7TGAexo2jVtLm50iVKTju2qmCDG83SnVHzsiNj70M" +
            "iviqiLpgz72IxjF+xN4bRw8I5dD0GwwO8kDoJUGWgTds+VckCwdtZA65oui9Osk5" +
            "t1a4pg6Xu9+HFcEuqwJTDxATvGAz1/YW0oUisjM0ObKTRDVSfnTYeaBsN6L+M+8g" +
            "CwIDAQAB";
 
    /**
     * 获取公钥
     *
     * @param publicKey base64加密的公钥字符串
     */
    public static PublicKey getPublicKey(String publicKey) throws Exception {
        byte[] decodedKey = Pem.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_NAME);
        return keyFactory.generatePublic(keySpec);
    }
 
    /**
     * RSA加密
     *
     * @param data 待加密数据
     */
    public static String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        int inputLen = data.getBytes().length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data.getBytes(), offset, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data.getBytes(), offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        // 获取加密内容使用base64进行编码,并以UTF-8为标准转化成字符串
        // 加密后的字符串
        return new String(Base64.getEncoder().encode(encryptedData));
    }
}