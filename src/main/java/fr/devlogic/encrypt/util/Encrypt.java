package fr.devlogic.encrypt.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Random;

public final class Encrypt {
    private Encrypt() {
    }

    /**
     * Input Stream encryption
     *
     * @param is
     * Stream
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key
     * @return
     * Stream to encrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static InputStream encrypt(InputStream is, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(is, Cipher.ENCRYPT_MODE, algo, keyBase64);
    }

    /**
     * Input stream decryption.
     *
     * @param is
     * Stream
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key
     * @return
     * Stream to decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static InputStream decrypt(InputStream is, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(is, Cipher.DECRYPT_MODE, algo, keyBase64);
    }

    /**
     * Encryption of output stream
     *
     * @param os
     * Stream
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key
     * @return
     * Stream to encrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static OutputStream encrypt(OutputStream os, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(os, Cipher.ENCRYPT_MODE, algo, keyBase64);
    }

    /**
     * Decryption of output stream
     *
     * @param os
     * Stream
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key
     * @return
     * Stream to decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static OutputStream decrypt(OutputStream os, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(os, Cipher.DECRYPT_MODE, algo, keyBase64);
    }

    /**
     * Encryption or decryption of input stream
     *
     * @param is
     * Stream to encrypt or decrypt
     * @param mode
     * mode {@link Cipher#DECRYPT_MODE} ou {@link Cipher#ENCRYPT_MODE}
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key
     * @return
     * Stream to decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static InputStream cipher(InputStream is, int mode, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(is, mode, algo, Base64.getDecoder().decode(keyBase64));
    }

    /**
     * Encryption or decryption of input stream
     *
     * @param is
     * Stream to encrypt or decrypt
     * @param mode
     * The mode {@link Cipher#DECRYPT_MODE} ou {@link Cipher#ENCRYPT_MODE}
     * @param algo
     * algorithm (e.g. "AES")
     * @param key
     * Key with size compatible with used algorithm
     * @return
     * Stream to encrypt or decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static InputStream cipher(InputStream is, int mode, String algo, byte[] key) throws GeneralSecurityException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, algo);
        Cipher cipher = Cipher.getInstance(algo);
        cipher.init(mode, secretKeySpec);

        return new InputStream() {

            final byte[] readBuffer = new byte[8192];
            byte[] writeBuffer = null;
            int writeBufferIndex = 0;
            boolean eof = false;

            @Override
            public int read() throws IOException {
                if ((writeBuffer == null) || (writeBufferIndex == writeBuffer.length)) {
                    if (eof) {
                        return -1;
                    }

                    writeBufferIndex = 0;

                    int readLength = is.read(readBuffer);
                    if (readLength < 0) {
                        try {
                            writeBuffer = cipher.doFinal();
                            eof = true;
                        } catch (IllegalBlockSizeException | BadPaddingException e) {
                            throw new EncryptException(e);
                        }
                    } else {
                        writeBuffer = cipher.update(readBuffer, 0, readLength);
                        if (writeBuffer.length == 0) {
                            try {
                                writeBuffer = cipher.doFinal();
                                eof = true;
                            } catch (IllegalBlockSizeException | BadPaddingException e) {
                                throw new EncryptException(e);
                            }
                        }
                    }

                    if (writeBuffer.length == 0) {
                        return -1;
                    }
                }

                return writeBuffer[writeBufferIndex++] & 0xff;
            }
        };
    }

    /**
     * Encryption or decryption of output stream
     *
     * @param os
     * Stream to encrypt or decrypt
     * @param mode
     * mode {@link Cipher#DECRYPT_MODE} ou {@link Cipher#ENCRYPT_MODE}
     * @param algo
     * algorithm (e.g. "AES")
     * @param keyBase64
     * Base 64 coded key with sized compatible with used algorithm
     * @return
     * Stream to encrypt or decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static OutputStream cipher(OutputStream os, int mode, String algo, String keyBase64) throws GeneralSecurityException {
        return cipher(os, mode, algo, Base64.getDecoder().decode(keyBase64));
    }

    /**
     * Encryption or decryption of output stream
     *
     * @param os
     * Stream to encrypt or decrypt
     * @param mode
     * Mode {@link Cipher#DECRYPT_MODE} ou {@link Cipher#ENCRYPT_MODE}
     * @param algo
     * algorithm (e.g. "AES")
     * @param key
     * Key with size compatible with used algorithm
     * @return
     * Stream to encrypt or decrypt
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static OutputStream cipher(OutputStream os, int mode, String algo, byte[] key) throws GeneralSecurityException {

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, algo);
        Cipher cipher = Cipher.getInstance(algo);
        cipher.init(mode, secretKeySpec);

        return new OutputStream() {

            final byte[] readBuffer = new byte[8192];
            byte[] writeBuffer = null;
            int readBufferIndex = 0;

            @Override
            public void write(int b) throws IOException {
                readBuffer[readBufferIndex++] = (byte) b;
                if (readBufferIndex == readBuffer.length) {
                    writeBuffer = cipher.update(readBuffer);
                    readBufferIndex = 0;
                    os.write(writeBuffer);
                }
            }

            @Override
            public void close() throws IOException {
                try {
                    if (readBufferIndex > 0) {
                        writeBuffer = cipher.doFinal(readBuffer, 0, readBufferIndex);
                    } else {
                        writeBuffer = cipher.doFinal();
                    }
                    os.write(writeBuffer);
                } catch (GeneralSecurityException ex) {
                    throw new EncryptException(ex);
                }
            }
        };
    }

    /**
     * String encryption
     *
     * @param data
     * String
     * @param algo
     * algorithm (e.g. "AES")
     * @param key
     * Base 64 coded key
     * @return
     * Encrypted string coded in base 64
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static String encrypt(String data, String algo, String key) throws GeneralSecurityException {
        return Base64.getEncoder().encodeToString(cipher(data.getBytes(), Cipher.ENCRYPT_MODE, algo, key));
    }

    /**
     * Decryption of base 64 coded data
     *
     * @param data
     * Base 64 data
     * @param algo
     * algorithm (e.g. "AES")
     * @param key
     * Base 64 coded key
     * @return
     * Decryption string
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static String decrypt(String data, String algo, String key) throws GeneralSecurityException {
        return new String(cipher(Base64.getDecoder().decode(data), Cipher.DECRYPT_MODE, algo, key));
    }

    /**
     * Data decryption or encryption.
     *
     * @param data
     * Data
     * @param mode
     * Mode {@link Cipher#DECRYPT_MODE} ou {@link Cipher#ENCRYPT_MODE}
     * @param algo
     * algorithm (e.g. "AES")
     * @param key
     * Key
     * @return
     * Decrypted or encrypted data
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static byte[] cipher(byte[] data, int mode, String algo, String key) throws GeneralSecurityException {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] keyBytes = decoder.decode(key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, algo);
        Cipher cipher = Cipher.getInstance(algo);
        cipher.init(mode, secretKeySpec);

        return cipher.doFinal(data);
    }

    /**
     * Key generator
     *
     * @param nbBits
     * Number of bits
     * @return
     * key stored in an array.
     */
    public static byte[] genKey(int nbBits) {
        byte[] key = new byte[nbBits / 8];
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) r.nextInt(256);
        }

        return key;
    }

    /**
     * Key generation or string encryption
     * @param args
     * Parameters list (-genKey sizeInBits OU -crypt algo key data1 [data2...])
     * @throws GeneralSecurityException
     * The exception is thrown when the algorithm, key or data is incorrect
     */
    public static void main(String[] args) throws GeneralSecurityException {
        if (args.length == 0) {
            System.out.println("usage:\n\t-genKey sizeInBits\n\t-crypt algo key data1 [data2...]");
            return;
        }

        String cmd = args[0];
        switch (cmd) {
            case "-genKey": {
                int size = Integer.parseInt(args[1]);
                System.out.println(Base64.getEncoder().encodeToString(Encrypt.genKey(size)));
                break;
            }
            case "-crypt": {
                String algo = args[1];
                String keyBase64 = args[2];
                for (int i = 3; i < args.length; i++) {
                    String data = args[i];
                    System.out.println(data + " -> \"" + Encrypt.encrypt(data, algo, keyBase64) + "\"");
                }
                break;
            }
            default:
                System.err.println("Unknown command " + cmd);
        }
    }
}
