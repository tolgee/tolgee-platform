package io.tolgee.service.aiReview

import io.tolgee.model.translation.Translation
import org.apache.commons.codec.digest.DigestUtils.sha256Hex

object AiReviewTextHash {
  fun of(translation: Translation): String = sha256Hex(translation.text ?: "")
}
