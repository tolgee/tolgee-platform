package io.tolgee.ee.service.qa.checks.unmatchedNewlines

data class Line(
  val text: String,
  val index: Int,
  val type: SeparatorType,
)
