package io.tolgee.component

import com.esotericsoftware.minlog.Log
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.Logging
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class SlackRequestValidation(
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  companion object {
    val algorithm = "HmacSHA256"
    val mac = Mac.getInstance(algorithm)
  }

  fun isValid(
    slackSignature: String,
    timestamp: String,
    body: String,
  ): Boolean {
    val secret = tolgeeProperties.slack.secretKey ?: return false

    val versionNumber = "v0"
    val baseString = "$versionNumber:$timestamp:$body"
    val requestTimestamp = timestamp.toLongOrNull() ?: return false
    val currentTime = Instant.now().epochSecond
    if (kotlin.math.abs(currentTime - requestTimestamp) > 60 * 5) {
      return false
    }

    return try {
      val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), algorithm)
      mac.init(secretKeySpec)

      val mySignature = "v0=" + Hex.encodeHexString(mac.doFinal(baseString.toByteArray(StandardCharsets.UTF_8)))
      mySignature == slackSignature
    } catch (e: Exception) {
      Log.info("Error validating request from Slack: $e")
      false
    }
  }
}
