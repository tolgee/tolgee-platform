package io.tolgee.fixtures

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.charset.StandardCharsets
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun verifyWebhookSignatureHeader(
  payload: String,
  sigHeader: String,
  secret: String,
  tolerance: Long,
  currentTimeInMs: Long,
): Boolean {
  val headerMap = jacksonObjectMapper().readValue<Map<String, Any>>(sigHeader)

  // Get timestamp and signatures from header
  val timestamp = headerMap["timestamp"] as? Long
  val signature = headerMap["signature"] as? String

  if (timestamp == null || timestamp <= 0 || signature == null) {
    throw SignatureVerificationException(
      "Unable to extract timestamp and signature from header",
    )
  }

  val signedPayload = "$timestamp.$payload"
  val expectedSignature: String =
    try {
      computeHmacSha256(secret, signedPayload)
    } catch (e: Exception) {
      throw SignatureVerificationException(
        "Unable to compute signature for payload",
      )
    }

  if (signature != expectedSignature) {
    throw SignatureVerificationException(
      "Wrong signature",
    )
  }

  // Check tolerance
  if (tolerance > 0 && timestamp < currentTimeInMs - tolerance) {
    throw SignatureVerificationException("Timestamp outside the tolerance zone")
  }
  return true
}

class SignatureVerificationException(
  message: String,
) : Exception(message)

fun computeHmacSha256(
  key: String,
  message: String,
): String {
  val hasher = Mac.getInstance("HmacSHA256")
  hasher.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
  val hash = hasher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
  return HexFormat.of().formatHex(hash)
}
