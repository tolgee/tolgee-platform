package io.tolgee.fixtures

import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun verifyHeader(
  payload: String, sigHeader: String, secret: String, tolerance: Long, currentTimeInSeconds: Long
): Boolean {
  // Get timestamp and signatures from header
  val timestamp: Long = getTimestamp(sigHeader)
  val signatures: List<String> =
    getSignatures(sigHeader, "v1")
  if (timestamp <= 0) {
    throw SignatureVerificationException(
      "Unable to extract timestamp and signatures from header", sigHeader
    )
  }
  if (signatures.size == 0) {
    throw SignatureVerificationException(
      "No signatures found with expected scheme", sigHeader
    )
  }

  // Compute expected signature
  val signedPayload = String.format("%d.%s", timestamp, payload)
  val expectedSignature: String
  expectedSignature = try {
    computeSignature(signedPayload, secret)
  } catch (e: Exception) {
    throw SignatureVerificationException(
      "Unable to compute signature for payload", sigHeader
    )
  }

  // Check if expected signature is found in list of header's signatures
  var signatureFound = false
  for (signature in signatures) {
    if (secureCompare(expectedSignature, signature)) {
      signatureFound = true
      break
    }
  }
  if (!signatureFound) {
    throw SignatureVerificationException(
      "No signatures found matching the expected signature for payload", sigHeader
    )
  }

  // Check tolerance
  if (tolerance > 0 && timestamp < currentTimeInSeconds - tolerance) {
    throw SignatureVerificationException("Timestamp outside the tolerance zone", sigHeader)
  }
  return true
}

class SignatureVerificationException(message: String, val sigHeader: String) : Exception(message)

private fun getTimestamp(sigHeader: String): Long {
  val items = sigHeader.split(",".toRegex()).toTypedArray()
  for (item in items) {
    val itemParts = item.split("=".toRegex(), limit = 2).toTypedArray()
    if (itemParts[0] == "t") {
      return itemParts[1].toLong()
    }
  }
  return -1
}

/**
 * Extracts the signatures matching a given scheme in a signature header.
 *
 * @param sigHeader the signature header
 * @param scheme the signature scheme to look for.
 * @return the list of signatures matching the provided scheme.
 */
private fun getSignatures(sigHeader: String, scheme: String): List<String> {
  val signatures: MutableList<String> = ArrayList()
  val items = sigHeader.split(",".toRegex()).toTypedArray()
  for (item in items) {
    val itemParts = item.split("=".toRegex(), limit = 2).toTypedArray()
    if (itemParts[0] == scheme) {
      signatures.add(itemParts[1])
    }
  }
  return signatures
}

fun secureCompare(a: String, b: String): Boolean {
  val digesta = a.toByteArray(StandardCharsets.UTF_8)
  val digestb = b.toByteArray(StandardCharsets.UTF_8)
  return MessageDigest.isEqual(digesta, digestb)
}

@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
private fun computeSignature(payload: String, secret: String): String {
  return computeHmacSha256(secret, payload)
}

private fun computeHmacSha256(key: String, message: String): String {
  val hasher = Mac.getInstance("HmacSHA256")
  hasher.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
  val hash = hasher.doFinal(message.toByteArray(StandardCharsets.UTF_8))
  var result = ""
  for (b in hash) {
    result += ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
  }
  return result
}
