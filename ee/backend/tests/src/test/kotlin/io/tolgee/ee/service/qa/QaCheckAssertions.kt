package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat

@DslMarker
annotation class QaCheckAssertDsl

fun List<QaCheckResult>.assertNoIssues() {
  assertThat(this)
    .withFailMessage { "Expected no QA issues but got ${this.size}:\n${formatResults(this)}" }
    .isEmpty()
}

fun List<QaCheckResult>.assertSingleIssue(block: QaIssueSpec.() -> Unit) {
  assertIssues { issue(block) }
}

fun List<QaCheckResult>.assertIssues(block: QaIssuesAssertScope.() -> Unit) {
  val scope = QaIssuesAssertScope()
  scope.block()
  scope.verify(this)
}

fun List<QaCheckResult>.assertAllHaveType(type: QaCheckType) {
  assertThat(this)
    .withFailMessage { "Expected non-empty results for assertAllHaveType" }
    .isNotEmpty()
  val mismatched = this.filter { it.type != type }
  assertThat(mismatched)
    .withFailMessage {
      "Expected all results to have type $type but ${mismatched.size} of ${this.size} differ:\n" +
        mismatched.joinToString("\n") { "  - type=${it.type}, message=${it.message}" }
    }.isEmpty()
}

@QaCheckAssertDsl
class QaIssuesAssertScope {
  private val specs = mutableListOf<QaIssueSpec>()

  fun issue(block: QaIssueSpec.() -> Unit) {
    val spec = QaIssueSpec()
    spec.block()
    specs.add(spec)
  }

  internal fun verify(actual: List<QaCheckResult>) {
    assertThat(actual)
      .withFailMessage {
        "Expected ${specs.size} issue(s) but got ${actual.size}.\n" +
          "Expected:\n${specs.joinToString("\n") { "  - $it" }}\n" +
          "Actual:\n${formatResults(actual)}"
      }.hasSize(specs.size)

    if (!findMatching(specs, actual.toMutableList(), 0)) {
      val description = buildUnmatchedDescription(specs, actual)
      throw AssertionError(description)
    }
  }
}

@QaCheckAssertDsl
class QaIssueSpec {
  private var expectedMessage: QaIssueMessage? = null
  private var expectedType: QaCheckType? = null
  private var expectedPositionStart: Int? = null
  private var expectedPositionEnd: Int? = null
  private var positionAsserted = false
  private var expectedReplacement: String? = null
  private var replacementAsserted = false
  private var expectedParams: MutableMap<String, String>? = null
  private var expectedPluralVariant: String? = null
  private var pluralVariantAsserted = false

  fun message(msg: QaIssueMessage) {
    expectedMessage = msg
  }

  fun type(type: QaCheckType) {
    expectedType = type
  }

  fun position(
    start: Int,
    end: Int,
  ) {
    expectedPositionStart = start
    expectedPositionEnd = end
    positionAsserted = true
  }

  fun noPosition() {
    expectedPositionStart = null
    expectedPositionEnd = null
    positionAsserted = true
  }

  fun replacement(text: String) {
    expectedReplacement = text
    replacementAsserted = true
  }

  fun noReplacement() {
    expectedReplacement = null
    replacementAsserted = true
  }

  fun param(
    key: String,
    value: String,
  ) {
    ensureParams()[key] = value
  }

  fun params(vararg pairs: Pair<String, String>) {
    ensureParams().putAll(pairs)
  }

  private fun ensureParams(): MutableMap<String, String> {
    if (expectedParams == null) expectedParams = mutableMapOf()
    return expectedParams!!
  }

  fun pluralVariant(variant: String) {
    expectedPluralVariant = variant
    pluralVariantAsserted = true
  }

  internal fun matches(result: QaCheckResult): Boolean {
    if (expectedMessage != null && result.message != expectedMessage) return false
    if (expectedType != null && result.type != expectedType) return false
    if (positionAsserted) {
      if (result.positionStart != expectedPositionStart) return false
      if (result.positionEnd != expectedPositionEnd) return false
    }
    if (replacementAsserted) {
      if (result.replacement != expectedReplacement) return false
    }
    if (expectedParams != null) {
      for ((key, value) in expectedParams!!) {
        if (result.params?.get(key) != value) return false
      }
    }
    if (pluralVariantAsserted) {
      if (result.pluralVariant != expectedPluralVariant) return false
    }
    return true
  }

  internal fun describeMismatch(result: QaCheckResult): String {
    val mismatches = mutableListOf<String>()
    if (expectedMessage != null && result.message != expectedMessage) {
      mismatches.add("message: expected=$expectedMessage, actual=${result.message}")
    }
    if (expectedType != null && result.type != expectedType) {
      mismatches.add("type: expected=$expectedType, actual=${result.type}")
    }
    if (positionAsserted) {
      if (result.positionStart != expectedPositionStart || result.positionEnd != expectedPositionEnd) {
        mismatches.add(
          "position: expected=($expectedPositionStart, $expectedPositionEnd), " +
            "actual=(${result.positionStart}, ${result.positionEnd})",
        )
      }
    }
    if (replacementAsserted && result.replacement != expectedReplacement) {
      mismatches.add(
        "replacement: expected=${formatNullable(expectedReplacement)}, actual=${formatNullable(result.replacement)}",
      )
    }
    if (expectedParams != null) {
      for ((key, value) in expectedParams!!) {
        val actual = result.params?.get(key)
        if (actual != value) {
          mismatches.add("param[$key]: expected=\"$value\", actual=${formatNullable(actual)}")
        }
      }
    }
    if (pluralVariantAsserted && result.pluralVariant != expectedPluralVariant) {
      mismatches.add("pluralVariant: expected=$expectedPluralVariant, actual=${result.pluralVariant}")
    }
    return mismatches.joinToString(", ")
  }

  override fun toString(): String {
    val parts = mutableListOf<String>()
    expectedMessage?.let { parts.add("message=$it") }
    expectedType?.let { parts.add("type=$it") }
    if (positionAsserted) {
      if (expectedPositionStart != null) {
        parts.add("position=($expectedPositionStart, $expectedPositionEnd)")
      } else {
        parts.add("noPosition")
      }
    }
    if (replacementAsserted) {
      if (expectedReplacement != null) {
        parts.add("replacement=\"$expectedReplacement\"")
      } else {
        parts.add("noReplacement")
      }
    }
    expectedParams?.let { parts.add("params=$it") }
    if (pluralVariantAsserted) {
      parts.add("pluralVariant=$expectedPluralVariant")
    }
    return "QaIssue(${parts.joinToString(", ")})"
  }
}

private fun findMatching(
  specs: List<QaIssueSpec>,
  remaining: MutableList<QaCheckResult>,
  specIndex: Int,
): Boolean {
  if (specIndex == specs.size) return true

  val spec = specs[specIndex]
  for (i in remaining.indices) {
    val result = remaining[i]
    if (spec.matches(result)) {
      remaining.removeAt(i)
      if (findMatching(specs, remaining, specIndex + 1)) return true
      remaining.add(i, result)
    }
  }
  return false
}

private fun buildUnmatchedDescription(
  specs: List<QaIssueSpec>,
  actual: List<QaCheckResult>,
): String {
  val sb = StringBuilder()
  sb.appendLine("Could not find a valid matching between expected issues and actual results.")
  sb.appendLine()
  sb.appendLine("Expected issues:")
  specs.forEachIndexed { i, spec ->
    sb.appendLine("  [$i] $spec")
  }
  sb.appendLine()
  sb.appendLine("Actual results:")
  actual.forEachIndexed { i, result ->
    sb.appendLine("  [$i] ${formatResult(result)}")
  }
  sb.appendLine()
  sb.appendLine("Mismatch details (each spec vs each result):")
  specs.forEachIndexed { specIdx, spec ->
    val candidates =
      actual.mapIndexedNotNull { resultIdx, result ->
        val mismatch = spec.describeMismatch(result)
        if (mismatch.isEmpty()) {
          "  spec[$specIdx] matches result[$resultIdx]"
        } else {
          "  spec[$specIdx] vs result[$resultIdx]: $mismatch"
        }
      }
    candidates.forEach { sb.appendLine(it) }
  }
  return sb.toString()
}

private fun formatResults(results: List<QaCheckResult>): String =
  results.joinToString("\n") {
    "  - ${formatResult(it)}"
  }

private fun formatResult(result: QaCheckResult): String {
  val parts = mutableListOf<String>()
  parts.add("message=${result.message}")
  parts.add("type=${result.type}")
  if (result.positionStart != null) {
    parts.add("position=(${result.positionStart}, ${result.positionEnd})")
  }
  result.replacement?.let { parts.add("replacement=\"$it\"") }
  result.params?.let { parts.add("params=$it") }
  result.pluralVariant?.let { parts.add("pluralVariant=$it") }
  return "QaCheckResult(${parts.joinToString(", ")})"
}

private fun formatNullable(value: String?): String = if (value != null) "\"$value\"" else "null"
