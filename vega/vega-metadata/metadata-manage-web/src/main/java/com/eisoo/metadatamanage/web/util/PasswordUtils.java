package com.eisoo.metadatamanage.web.util;

import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.standardization.common.exception.AiShuException;
import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.util
 * @Date: 2023/3/31 14:33
 */
public class PasswordUtils {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtils.class);

    private static final Base64 BASE64 = new Base64();

    public static final String DATASOURCE_ENCRYPTION_ENABLE = "datasource.encryption.enable";
    public static final String DATASOURCE_ENCRYPTION_SALT = "datasource.encryption.salt";
    public static final String DATASOURCE_ENCRYPTION_SALT_DEFAULT = "!@#$%^&*";
    private static final String privateKeyPath = "rsa/privatekey.pem";
    private static final String publicKeyPath = "rsa/publickey.pem";
    private PasswordUtils() {
        throw new UnsupportedOperationException("Construct PasswordUtils");
    }

    /**
     * encode password
     */
    public static String encodePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }
        //if encryption is not turned on, return directly
        boolean encryptionEnable = getBoolean(DATASOURCE_ENCRYPTION_ENABLE, false);
        if (!encryptionEnable) {
            return password;
        }

        // Using Base64 + salt to process password
        String salt = getString(DATASOURCE_ENCRYPTION_SALT, DATASOURCE_ENCRYPTION_SALT_DEFAULT);
        String passwordWithSalt = salt + new String(BASE64.encode(password.getBytes(
                StandardCharsets.UTF_8)));
        return new String(BASE64.encode(passwordWithSalt.getBytes(StandardCharsets.UTF_8)));
    }
    /**
     * encode password
     */
    public static String encodePasswordBase64NoSalt(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }
        String passwordNoSalt = new String(BASE64.encode(password.getBytes(
                StandardCharsets.UTF_8)));
        return passwordNoSalt;
    }

    /**
     * get property value
     *
     * @param key property name
     * @return property value
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * get property value
     *
     * @param key property name
     * @param defaultValue default value
     * @return property value
     */
    public static Boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        return Strings.isNullOrEmpty(value) ? defaultValue : Boolean.parseBoolean(value);
    }
    public static String getString(String key, String defaultVal) {
        String val = getString(key);
        return Strings.isNullOrEmpty(val) ? defaultVal : val;
    }
    public static String getString(String key) {
        return null;
    }

    /**
     * decode password
     */
    public static String decodePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }

        //if encryption is not turned on, return directly
        boolean encryptionEnable = getBoolean(DATASOURCE_ENCRYPTION_ENABLE, false);
        if (!encryptionEnable) {
            return password;
        }

        // Using Base64 + salt to process password
        String salt = getString(DATASOURCE_ENCRYPTION_SALT, DATASOURCE_ENCRYPTION_SALT_DEFAULT);
        String passwordWithSalt = new String(BASE64.decode(password), StandardCharsets.UTF_8);
        if (!passwordWithSalt.startsWith(salt)) {
            logger.warn("There is a password and salt mismatch: {} ", password);
            return password;
        }
        return new String(BASE64.decode(passwordWithSalt.substring(salt.length())), StandardCharsets.UTF_8);
    }

    /**
     * decode password
     */
    public static String decodePasswordNoSalt(String password) {
        if (StringUtils.isEmpty(password)) {
            return StringUtils.EMPTY;
        }

        // Using Base64 process password
        password = new String(BASE64.decode(password), StandardCharsets.UTF_8);

        return password;
    }

    public static String decodePasswordRSA(String password) {
        try {
            PrivateKey keyF = readPrivateKey(privateKeyPath);
            return decryptRSA(keyF, password);
        } catch (IOException e) {
            throw new AiShuException(ErrorCodeEnum.InternalError, "数据源密码解析错误！", "请检查：密钥文件是否正确配置");
        }
//        try {
//            byte[] keybyte = FileUtil.readAsByteArray(keyFile);
//        } catch (IOException e){
//            e.printStackTrace();
//        }
    }

    /**
     * 返回 RSA 解密的结果
     */
    public static String decryptRSA(Key privateKey, String content) {
        try {
            byte[] text = java.util.Base64.getDecoder().decode(content);
            Cipher rsa = Cipher.getInstance(Constants.CIPHER_ALGORITHM);
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            //大于128时进行分段 解密
            int subLength = text.length / 128;
            StringBuilder finalString = new StringBuilder();
            for (int i = 0; i < subLength; i++) {
                finalString.append(new String(rsa.doFinal(text, i * 128, 128), StandardCharsets.UTF_8));
            }
            return finalString.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String encodePasswordBase64(String password) {
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        return encoder.encodeToString(password.getBytes());
    }

    public static String encodePasswordRSA(String password) {
        try {
            PublicKey keyF = readPublicKey(publicKeyPath);
            return encryptRSA(keyF, password);
        }  catch (Exception e) {
            throw new AiShuException(ErrorCodeEnum.InternalError, "数据源密码加密错误！", e.toString());
        }
    }

    //使用RSA加密
    public static String encryptRSA(Key publicKey, String content) {
        try {
            Cipher rsa = Cipher.getInstance(Constants.CIPHER_ALGORITHM);
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedByte = rsa.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encryptedByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 根据文件路径返回私匙
    public static PrivateKey readPrivateKey(String filePath) throws IOException {
//        ClassPathResource classPathResource = new ClassPathResource(filePath);
        //获得文件流时，因为读取的文件是在打好jar文件里面，不能直接通过文件资源路径拿到文件，但是可以在jar包中拿到文件流
//ResourcePatternResolver的实现方法，可以匹配到各种部署时的各种文件类型例如war，jar，zip等等findPathMatchingResources
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(filePath);
        Resource resource = resources[0];
        return readPrivateKey(resource.getInputStream());
    }

    // 根据输入流返回私匙
    public static PrivateKey readPrivateKey(InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        int n;
        final byte[] buffer = new byte[1024 * 4];
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        String privateKeyPEM = output.toString()
                .replace("-----BEGIN RSA Private Key-----", "")
                .replaceAll("\n", "")
                .replace("-----END RSA Private Key-----", "");
        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(privateKeyPEM.getBytes(StandardCharsets.UTF_8)));
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 根据文件返回公匙
    public static PublicKey readPublicKey(String filePath) throws IOException {
//        PublicKey classPathResource = new ClassPathResource(filePath);
        //获得文件流时，因为读取的文件是在打好jar文件里面，不能直接通过文件资源路径拿到文件，但是可以在jar包中拿到文件流
//ResourcePatternResolver的实现方法，可以匹配到各种部署时的各种文件类型例如war，jar，zip等等findPathMatchingResources
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(filePath);
        Resource resource = resources[0];
        return readPublicKey(resource.getInputStream());
    }

    // 根据输入流返回公匙
    public static PublicKey readPublicKey(InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        int n;
        final byte[] buffer = new byte[1024 * 4];
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        String publicKeyPEM = output.toString()
                .replace("-----BEGIN RSA Public Key-----", "")
                .replaceAll("\n", "")
                .replace("-----END RSA Public Key-----", "");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(publicKeyPEM.getBytes(StandardCharsets.UTF_8)));
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

}
