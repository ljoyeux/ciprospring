package fr.devlogic.encrypt.spring;

import fr.devlogic.encrypt.util.Encrypt;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

class EncryptTest {

    private static final Logger log = LoggerFactory.getLogger(EncryptTest.class);

    @RepeatedTest(1_00)
    void test() throws GeneralSecurityException, IOException {
        byte[] key = new byte[256 / 8];
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) r.nextInt(256);
        }

        String algo = "AES";


        int length = r.nextInt(100 * 1_024);
        if (length<1) {
           return;
        }

        byte[] data = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) r.nextInt(256);
        }
        log.info("size {}", length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (OutputStream os = Encrypt.cipher(byteArrayOutputStream, Cipher.ENCRYPT_MODE, algo, key)) {
            for (byte datum : data) {
                os.write(datum);
            }
        }

        byteArrayOutputStream.close();
        log.info("encoded {}", byteArrayOutputStream.size());

        InputStream is = Encrypt.cipher(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), Cipher.DECRYPT_MODE, algo, key);
        List<Byte> bytes = new ArrayList<>();
        for (; ; ) {
            int read = is.read();
            if (read < 0) {
                break;
            }
            bytes.add((byte) (read & 0xff));
        }

        Assertions.assertThat(bytes.size()).isEqualTo(data.length);
        for (int i = 0; i < bytes.size(); i++) {
            Assertions.assertThat(data[i]).isEqualTo(bytes.get(i));
        }

        // chiffrement d'un seul coup
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, algo);
        Cipher cipher = Cipher.getInstance(algo);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        Assertions.assertThat(cipher.doFinal(data)).isEqualTo(byteArrayOutputStream.toByteArray());

        cipher = Cipher.getInstance(algo);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        Assertions.assertThat(cipher.doFinal(byteArrayOutputStream.toByteArray())).isEqualTo(data);

    }

    @Test
    void keyBase64() {
        byte[] key = new byte[256 / 8];
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) r.nextInt(256);
        }
        String s = Base64.getEncoder().encodeToString(key);
        System.out.println(s);
        byte[] decode = Base64.getDecoder().decode(s);
        Assertions.assertThat(decode).isEqualTo(key);
    }

    @Test
    void encryptDecrypt() throws GeneralSecurityException {
        byte[] bytes = Encrypt.genKey(256);
        String base64Key = Base64.getEncoder().encodeToString(bytes);
        String str = "Lqfdggliuhg ;lb827";

        String encrypt = Encrypt.encrypt(str, "AES", base64Key);
        System.out.println(encrypt);
        String decrypt = Encrypt.decrypt(encrypt, "AES", base64Key);

        Assertions.assertThat(str).isEqualTo(decrypt);
    }

    @Test
    void generateKey() {
        System.out.println(Base64.getEncoder().encodeToString(Encrypt.genKey(256)));
        org.junit.jupiter.api.Assertions.assertTrue(true);
    }

    @Test
    void base64() {
        byte[] key =
                new byte[] {(byte) 0x60, (byte) 0x40, (byte) 0xfe, (byte) 0x20,
                        (byte) 0x92, (byte) 0x2d, (byte) 0x72, (byte) 0x1c,
                        (byte) 0x69, (byte) 0x3b, (byte) 0x79, (byte) 0x9a,
                        (byte) 0xe5, (byte) 0xb3, (byte) 0xe1, (byte) 0x95,
                        (byte) 0x1d, (byte) 0x90, (byte) 0x46, (byte) 0x24,
                        (byte) 0x25, (byte) 0xf4, (byte) 0x4b, (byte) 0x6e,
                        (byte) 0xbb, (byte) 0xa1, (byte) 0x72, (byte) 0x20,
                        (byte) 0x00, (byte) 0x04, (byte) 0x8e, (byte) 0xf6 };
        System.out.println(Base64.getEncoder().encodeToString(key));

        org.junit.jupiter.api.Assertions.assertTrue(true);

    }

    @Test
    void encrypt() throws GeneralSecurityException {
        String key = Constantes.CRYPTO_KEY;
        for (String mdp : Arrays.asList(Constantes.MOT_DE_PASSE)) {
            System.out.println(mdp + " -> " + Encrypt.encrypt(mdp, "AES", key));
        }

        org.junit.jupiter.api.Assertions.assertTrue(true);
    }

}
