package com.encryption;

import org.apache.commons.crypto.cipher.CryptoCipher;
import org.apache.commons.crypto.utils.Utils;
// import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.CryptoServicesRegistrar;

// ! each import libary is considered a bussiness application
// import org.jasypt.util.text.BasicTextEncryptor;
// import javax.crypto.Cipher;
// import javax.crypto.spec.SecretKeySpec;
// import java.security.Security;
// import java.util.Base64;
import java.util.Properties;

public class App {
    // static {
    // Security.addProvider(new BouncyCastleProvider());
    // }

    private static void sayHello() {
        System.out.println("Hello World!");
    }

    private static void sayGoodbye() {
        System.out.println("Bye!");
    }

    public static void main(String[] args) throws Exception {
        String text = "Hello World!";
        String key = "1234567812345678"; // 16 chars=128 bits
        sayHello();
        // // Using BouncyCastle
        // Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
        // SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
        // System.out.println("original text: " + text);

        // // Encryption with BouncyCastle
        // cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        // byte[] encrypted = cipher.doFinal(text.getBytes());
        // String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
        // System.out.println("BouncyCastle Encrypted: " + encryptedBase64);

        // // Decryption with BouncyCastle
        // cipher.init(Cipher.DECRYPT_MODE, keySpec);
        // byte[] decrypted =
        // cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        // System.out.println("BouncyCastle Decrypted: " + new String(decrypted));

        // // ! Using Jasypt
        // BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        // textEncryptor.setPassword(key);

        // // Encryption with Jasypt
        // String encryptedText = textEncryptor.encrypt(text);
        // System.out.println("Jasypt Encrypted: " + encryptedText);

        // // Decryption with Jasypt
        // String decryptedText = textEncryptor.decrypt(encryptedText);
        // System.out.println("Jasypt Decrypted: " + decryptedText);

        // * Using Apache Commons Crypto
        Properties properties = new Properties();
        properties.setProperty("CryptoCipher.transformation", "AES/CBC/PKCS5Padding");
        CryptoCipher encipher = Utils.getCipherInstance("AES/CBC/PKCS5Padding", properties);
        byte[] initVector = "RandomInitVector".getBytes(); // 16 bytes IV, just for demonstration
        byte[] output = new byte[32]; // Here 32 is just a sample size. Ideally, you should calculate or handle the
                                      // size dynamically.
        // ! test the nested ability of meta ast, focus at the getP function
        Object[] sizedProperty = CryptoServicesRegistrar
                .getSizedProperty(CryptoServicesRegistrar.Property.DH_DEFAULT_PARAMS);
        // Encryption with Apache Commons Crypto
        // encipher.init(Cipher.ENCRYPT_MODE, keySpec, new
        // javax.crypto.spec.IvParameterSpec(initVector));
        // int updateBytes = encipher.update(text.getBytes(), 0, text.getBytes().length,
        // output, 0);
        // int finalBytes = encipher.doFinal(text.getBytes(), 0, 0, output,
        // updateBytes);
        // byte[] finalEncrypted = new byte[updateBytes + finalBytes];
        // System.arraycopy(output, 0, finalEncrypted, 0, updateBytes + finalBytes);
        // String encryptedBase64Commons =
        // Base64.getEncoder().encodeToString(finalEncrypted);
        // System.out.println("Apache Commons Crypto Encrypted: " +
        // encryptedBase64Commons);

        // Decryption with Apache Commons Crypto
        // CryptoCipher decipher = Utils.getCipherInstance("AES/CBC/PKCS5Padding",
        // properties);
        // decipher.init(Cipher.DECRYPT_MODE, keySpec, new
        // javax.crypto.spec.IvParameterSpec(initVector)); // Initialization
        // // of the
        // // decipher
        // byte[] decryptedOutput = new byte[32];
        // updateBytes =
        // decipher.update(Base64.getDecoder().decode(encryptedBase64Commons), 0,
        // Base64.getDecoder().decode(encryptedBase64Commons).length, decryptedOutput,
        // 0);
        // finalBytes =
        // decipher.doFinal(Base64.getDecoder().decode(encryptedBase64Commons), 0, 0,
        // decryptedOutput,
        // updateBytes);
        // byte[] finalDecryptedCommons = new byte[updateBytes + finalBytes];
        // System.arraycopy(decryptedOutput, 0, finalDecryptedCommons, 0, updateBytes +
        // finalBytes);
        // System.out.println("Apache Commons Crypto Decrypted: " + new
        // String(finalDecryptedCommons).trim());

        sayGoodbye();
    }
}
