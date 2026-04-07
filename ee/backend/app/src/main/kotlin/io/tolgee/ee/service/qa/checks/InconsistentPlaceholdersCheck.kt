package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class InconsistentPlaceholdersCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.INCONSISTENT_PLACEHOLDERS

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText ->
      checkVariant(text, baseText)
    }
  }

  /**
   * Args from nested select/plural branches (null positions) represent the same logical
   * placeholder repeated across branches — deduplicate them by name so counting works correctly.
   * Top-level args (with positions) are kept as-is since each occurrence is meaningful.
   */
  private fun deduplicateNestedArgs(args: List<ArgInfo>): List<ArgInfo> {
    val (positioned, nested) = args.partition { it.positionStart != null }
    return positioned + nested.distinctBy { it.name }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val baseArgs = deduplicateNestedArgs(extractArgs(base) ?: return emptyList())
    val textArgs = deduplicateNestedArgs(extractArgs(text) ?: return emptyList())

    val baseCounts = baseArgs.groupingBy { it.name }.eachCount()
    val textCounts = textArgs.groupingBy { it.name }.eachCount()

    val results = mutableListOf<QaCheckResult>()

    for ((name, baseCount) in baseCounts) {
      val textCount = textCounts[name] ?: 0
      repeat(maxOf(0, baseCount - textCount)) {
        results.add(
          QaCheckResult(
            type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
            message = QaIssueMessage.QA_PLACEHOLDERS_MISSING,
            replacement = null,
            positionStart = null,
            positionEnd = null,
            params = mapOf("placeholder" to name),
          ),
        )
      }
    }

    for ((name, textCount) in textCounts) {
      val baseCount = baseCounts[name] ?: 0
      val extraCount = maxOf(0, textCount - baseCount)
      if (extraCount > 0) {
        val extraArgs = textArgs.filter { it.name == name }.takeLast(extraCount)
        for (arg in extraArgs) {
          val hasPosition = arg.positionStart != null && arg.positionEnd != null
          results.add(
            QaCheckResult(
              type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
              message = QaIssueMessage.QA_PLACEHOLDERS_EXTRA,
              replacement = if (hasPosition) "" else null,
              positionStart = arg.positionStart,
              positionEnd = arg.positionEnd,
              params = mapOf("placeholder" to arg.name),
            ),
          )
        }
      }
    }

    return results
  }
}
