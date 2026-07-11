package io.tolgee.ee.component.slackIntegration.slashcommand

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.SlackErrorException
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
  private val slackErrorProvider: SlackErrorProvider,
) : Logging {
  companion object {
    private const val ALGORITHM = "HmacSHA256"
    private val mac = Mac.getInstance(ALGORITHM)
  }

  fun validate(
    slackSignature: String,
    timestamp: String,
    body: String,
  ) {
    val secret = tolgeeProperties.slack.signingSecret ?: return throwError()

    val versionNumber = "v0"
    val baseString = "$versionNumber:$timestamp:$body"
    val requestTimestamp = timestamp.toLongOrNull() ?: return throwError()
    val currentTime = Instant.now().epochSecond
    if (kotlin.math.abs(currentTime - requestTimestamp) > 60 * 5) {
      throwError()
    }

    try {
      val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
      mac.init(secretKeySpec)

      val mySignature = "v0=" + Hex.encodeHexString(mac.doFinal(baseString.toByteArray(StandardCharsets.UTF_8)))

      if (mySignature != slackSignature) {
        throwError()
      }
    } catch (e: Exception) {
      throwError()
    }
  }

  private fun throwError() {
    throw SlackErrorException(slackErrorProvider.getInvalidSignatureError(), message = "Invalid signature")
  }
}
