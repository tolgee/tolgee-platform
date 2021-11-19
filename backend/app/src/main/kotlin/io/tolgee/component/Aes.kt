/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import io.tolgee.security.JwtSecretProvider
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class Aes(jwtSecretProvider: JwtSecretProvider) {
  private val secretKey: SecretKeySpec? by lazy {
    val sha = MessageDigest.getInstance("SHA-1")
    val key = Arrays.copyOf(sha.digest(jwtSecretProvider.jwtSecret), 16)
    SecretKeySpec(key, "AES")
  }

  fun encrypt(toEncrypt: ByteArray): ByteArray? {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    return cipher.doFinal(toEncrypt)
  }

  fun decrypt(toDecrypt: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    return cipher.doFinal(toDecrypt)
  }
}
