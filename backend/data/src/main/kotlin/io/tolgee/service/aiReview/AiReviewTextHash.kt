package io.tolgee.service.aiReview

import io.tolgee.model.translation.Translation
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import org.apache.commons.codec.digest.DigestUtils.sha256Hex

/** The Kotlin, native SQL and Criteria forms must give the same hash, or rows silently stop matching. */
object AiReviewTextHash {
  const val CURRENT_TEXT_SQL = "encode(sha256(convert_to(coalesce(t.text, ''), 'UTF8')), 'hex')"

  const val CURRENT_TEXT_JPQL =
    "function('encode', function('sha256', function('convert_to', coalesce(t.text, ''), 'UTF8')), 'hex')"

  fun of(translation: Translation): String = sha256Hex(translation.text ?: "")

  fun criteria(
    cb: CriteriaBuilder,
    text: Expression<String?>,
  ): Expression<String> =
    cb.function(
      "encode",
      String::class.java,
      cb.function(
        "sha256",
        ByteArray::class.java,
        cb.function("convert_to", ByteArray::class.java, cb.coalesce(text, ""), cb.literal("UTF8")),
      ),
      cb.literal("hex"),
    )
}
