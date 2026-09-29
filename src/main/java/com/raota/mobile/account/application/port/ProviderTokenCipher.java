package com.raota.mobile.account.application.port;

/** 외부 제공자의 갱신 토큰을 보관용 값으로 암호화한다. */
public interface ProviderTokenCipher {

    String encrypt(String plaintext);

    String decrypt(String encrypted);

}
