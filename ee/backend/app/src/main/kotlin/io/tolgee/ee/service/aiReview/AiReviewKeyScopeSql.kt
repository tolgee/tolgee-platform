package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.matching.KeyScope
import jakarta.persistence.Tuple

object AiReviewKeyScopeSql {
  private const val TAG_SEPARATOR = '\u001F'

  fun Tuple.toKeyScope(keyId: Long): KeyScope =
    KeyScope(
      keyId = keyId,
      namespace = get("namespace_name", String::class.java),
      branchName = get("branch_name", String::class.java),
      tags =
        get("tag_names", String::class.java)
          .split(TAG_SEPARATOR)
          .filter { it.isNotEmpty() }
          .toSet(),
    )
}
