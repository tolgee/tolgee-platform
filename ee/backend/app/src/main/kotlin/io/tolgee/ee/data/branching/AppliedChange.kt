package io.tolgee.ee.data.branching

import io.tolgee.model.key.Key

data class AppliedChange(
  val sourceKey: Key?,
  val targetKey: Key?,
)
