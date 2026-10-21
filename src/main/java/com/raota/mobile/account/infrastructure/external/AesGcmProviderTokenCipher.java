package com.raota.mobile.account.infrastructure.external;

import com.raota.mobile.account.application.port.ProviderTokenCipher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 토큰마다 다른 IV를 사용하는 AES-256-GCM 보관 형식이다. */
@Component
public class AesGcmProviderTokenCipher implements ProviderTokenCipher {

    private static final int IV_LENGTH = 12;

    private static final int TAG_BITS = 128;

    private static final String VERSION = "v1:";

    private final SecretKeySpec key;

    private final SecureRandom random = new SecureRandom();

    public AesGcmProviderTokenCipher(MobileOAuthProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.apple().tokenEncryptionKey());
        if (bytes.length != 32) {
            throw new IllegalStateException("Apple 토큰 암호화 키는 32바이트여야 합니다.");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    @Override
    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + Base64.getEncoder()
                .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("제공자 토큰 암호화에 실패했습니다.", exception);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith(VERSION)) {
            throw new IllegalStateException("알 수 없는 제공자 토큰 암호화 버전입니다.");
        }
        try {
            byte[] data = Base64.getDecoder().decode(encrypted.substring(VERSION.length()));
            if (data.length < IV_LENGTH + TAG_BITS / 8) {
                throw new IllegalStateException("암호화된 제공자 토큰이 올바르지 않습니다.");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_LENGTH));
            return new String(cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("암호화된 제공자 토큰이 올바르지 않습니다.", exception);
        }
    }

}
