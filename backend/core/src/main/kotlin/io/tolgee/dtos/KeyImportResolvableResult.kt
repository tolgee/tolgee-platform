package io.tolgee.dtos

import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.model.Screenshot

data class KeyImportResolvableResult(
  val keys: List<KeyView>,
  val screenshots: Map<Long, Screenshot>,
)
