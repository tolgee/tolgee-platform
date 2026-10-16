package io.tolgee.util

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import org.springframework.util.DigestUtils

fun aiReviewTextHash(text: String?): String = DigestUtils.md5DigestAsHex((text ?: "").toByteArray(Charsets.UTF_8))

fun aiReviewTextHashSql(textColumn: String): String = "md5(coalesce($textColumn, ''))"

/**
 * HQL `md5` renders as `decode(md5(..), 'hex')` (bytea), so it has to be encoded back to the hex text
 * [aiReviewTextHash] produces. Plain `md5(text)` would never equal a stored hash.
 */
fun CriteriaBuilder.aiReviewTextHash(text: Expression<String>): Expression<String> =
  function(
    "encode",
    String::class.java,
    function("md5", ByteArray::class.java, coalesce(text, "")),
    literal("hex"),
  )
