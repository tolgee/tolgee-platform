package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage

/**
 * Result from a single QA check run.
 *
 * Position convention:
 * - `null` means "no specific position" — the issue applies to the whole translation.
 * - Zero-length ranges where `positionStart == positionEnd` (both non-null) are valid
 *   insertion points (e.g., "add punctuation at end of text").
 */
data class QaCheckResult(
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String? = null,
  val positionStart: Int? = null,
  val positionEnd: Int? = null,
  /**
   * Params are used to format the `message` on the frontend.
   */
  val params: Map<String, String>? = null,
  val pluralVariant: String? = null,
) {
  init {
    require((positionStart == null) xor (positionEnd == null)) {
      "positionStart and positionEnd must be both null or both non-null"
    }
    require(positionStart == null || positionEnd == null || positionStart <= positionEnd) {
      "positionStart must be less than or equal to positionEnd"
    }
    require(replacement == null || positionStart != null) {
      "replacement can only be set if position is non-null"
    }
  }
}
