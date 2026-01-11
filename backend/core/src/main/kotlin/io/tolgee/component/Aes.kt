/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.component

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class Aes(
  @Qualifier("jwt_signing_secret") bytes: ByteArray,
) {
  private val secretKey: SecretKeySpec by lazy {
    val sha = MessageDigest.getInstance("SHA-256")
    val key = sha.digest(bytes)
    SecretKeySpec(key.sliceArray(0 until 16), "AES")
  }

  private val secureRandom = SecureRandom()

  private fun getGcmParameterSpec(): GCMParameterSpec {
    val iv = ByteArray(16)
    secureRandom.nextBytes(iv)
    return GCMParameterSpec(128, iv)
  }

  fun encrypt(toEncrypt: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val cipherInitVector = getGcmParameterSpec()
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, cipherInitVector)
    val encryptedData = cipher.doFinal(toEncrypt)
    return ByteBuffer
      .allocate(4 + cipherInitVector.iv.size + encryptedData.size)
      .putInt(cipherInitVector.iv.size)
      .put(cipherInitVector.iv)
      .put(encryptedData)
      .array()
  }

  fun decrypt(toDecrypt: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val byteBuffer = ByteBuffer.wrap(toDecrypt)
    val ivLength = byteBuffer.int
    val iv = ByteArray(ivLength)
    byteBuffer.get(iv)
    val cipherText = ByteArray(byteBuffer.remaining())
    byteBuffer.get(cipherText)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    return cipher.doFinal(cipherText)
  }
}
