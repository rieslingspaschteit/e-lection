package electioncli.utils;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {

    private static final String INVALID_ENCRYPTION = "Error, requested encryption is not supported";
    private static final String INVALID_KEY = "Error, provided key could not be resolved";

    public static String encode(byte[] publicKey, String message, String func) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        KeyFactory keyFactory = KeyFactory.getInstance(func);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
        PublicKey key = keyFactory.generatePublic(publicKeySpec);

        return encode(publicKey, message, func);
    }

    public static String encode(PublicKey publicKey, String message, String func) {
        try {
            Cipher cipher = Cipher.getInstance(func);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessageBytes = cipher.doFinal(secretMessageBytes);
            return Base64.getEncoder().encodeToString(encryptedMessageBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static String decode(byte[] privateKey, String message, String func) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(func);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
            PrivateKey key = keyFactory.generatePrivate(privateKeySpec);
            return decode(key, message, func);
        } catch(Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static String decode(PrivateKey privateKey, String message, String func) {
        try {
            Cipher cipher = Cipher.getInstance(func);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] encryptedMessageBytes = Base64.getDecoder().decode(message);
            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessageBytes);
            return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(INVALID_ENCRYPTION);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(INVALID_KEY);
        }
    }

    public static String keyToString(Key key) {
        byte[] keyBytes = key.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public static PublicKey stringToPublicKey(String encodedKey) {
        byte[] keyByte = Base64.getDecoder().decode(encodedKey);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(INVALID_ENCRYPTION);
        }
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyByte);
        try {
            PublicKey key = keyFactory.generatePublic(publicKeySpec);
            return key;
        } catch(InvalidKeySpecException e) {
            throw new IllegalArgumentException(INVALID_KEY);
        }
    }

    public static PrivateKey stringToPrivateKey(String encodedKey) {
        byte[] keyByte = Base64.getDecoder().decode(encodedKey);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch(NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(keyByte);
        try {
            PrivateKey key = keyFactory.generatePrivate(publicKeySpec);
            return key;
        } catch(InvalidKeySpecException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
