package io.tolgee.ee.service.qa.checks.lines

data class Line(
  val text: String,
  val index: Int,
  val type: SeparatorType,
)
