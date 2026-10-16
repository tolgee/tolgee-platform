package io.tolgee.service.translation

import io.tolgee.model.translation.Translation

interface SourceChangeRetranslationPolicy {
  fun selectForRetranslation(
    projectId: Long,
    translations: Collection<Translation>,
  ): Set<Long>

  fun hasCustomBehavior(projectId: Long): Boolean
}
