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
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, isVariant ->
      checkVariant(text, baseText, isVariant)
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
    isPluralVariant: Boolean,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank() || text.isBlank()) return emptyList()

    val baseArgs = deduplicateNestedArgs(extractArgs(base, isPluralVariant) ?: return emptyList())
    val textArgs = deduplicateNestedArgs(extractArgs(text, isPluralVariant) ?: return emptyList())

    val baseCounts = baseArgs.groupingBy { it.name }.eachCount()
    val textCounts = textArgs.groupingBy { it.name }.eachCount()

    val missingArgNames = findMissingArgNames(baseCounts, textCounts)
    val extraArgs = findExtraArgs(textArgs, baseCounts, textCounts)

    // Special case - when we can rename placeholder to correct it
    tryRenameArg(baseArgs, missingArgNames, extraArgs)?.let {
      return listOf(it)
    }

    return addMissingArgs(missingArgNames) + removeExtraArgs(extraArgs)
  }

  private fun findMissingArgNames(
    baseCounts: Map<String, Int>,
    textCounts: Map<String, Int>,
  ): List<String> {
    val missing = mutableListOf<String>()
    for ((name, baseCount) in baseCounts) {
      val textCount = textCounts[name] ?: 0
      repeat(maxOf(0, baseCount - textCount)) { missing.add(name) }
    }
    return missing
  }

  private fun findExtraArgs(
    textArgs: List<ArgInfo>,
    baseCounts: Map<String, Int>,
    textCounts: Map<String, Int>,
  ): List<ArgInfo> {
    val extra = mutableListOf<ArgInfo>()
    for ((name, textCount) in textCounts) {
      val baseCount = baseCounts[name] ?: 0
      val extraCount = maxOf(0, textCount - baseCount)
      if (extraCount > 0) {
        extra.addAll(textArgs.filter { it.name == name }.takeLast(extraCount))
      }
    }
    return extra
  }

  private fun tryRenameArg(
    baseArgs: List<ArgInfo>,
    missingArgNames: List<String>,
    extraArgs: List<ArgInfo>,
  ): QaCheckResult? {
    val expectedName = missingArgNames.singleOrNull() ?: return null
    val currentName = extraArgs.singleOrNull() ?: return null
    val source = baseArgs.firstOrNull { it.name == expectedName }?.sourceText ?: return null
    if (currentName.positionStart == null || currentName.positionEnd == null) return null

    return QaCheckResult(
      type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
      message = QaIssueMessage.QA_PLACEHOLDERS_REPLACE,
      replacement = source,
      positionStart = currentName.positionStart,
      positionEnd = currentName.positionEnd,
      params = mapOf("placeholder" to currentName.name, "expected" to expectedName),
    )
  }

  private fun addMissingArgs(missingArgNames: List<String>): List<QaCheckResult> {
    return missingArgNames.map { name ->
      QaCheckResult(
        type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
        message = QaIssueMessage.QA_PLACEHOLDERS_MISSING,
        replacement = null,
        positionStart = null,
        positionEnd = null,
        params = mapOf("placeholder" to name),
      )
    }
  }

  private fun removeExtraArgs(extraArgs: List<ArgInfo>): List<QaCheckResult> {
    return extraArgs.map { arg ->
      val hasPosition = arg.positionStart != null && arg.positionEnd != null
      QaCheckResult(
        type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
        message = QaIssueMessage.QA_PLACEHOLDERS_EXTRA,
        replacement = if (hasPosition) "" else null,
        positionStart = arg.positionStart,
        positionEnd = arg.positionEnd,
        params = mapOf("placeholder" to arg.name),
      )
    }
  }
}
