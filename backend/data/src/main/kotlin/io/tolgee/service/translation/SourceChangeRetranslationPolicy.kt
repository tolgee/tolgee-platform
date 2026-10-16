package io.tolgee.service.translation

import io.tolgee.model.translation.Translation

interface SourceChangeRetranslationPolicy {
  /**
   * Returns the ids of the [translations] that auto translation replaces after their source text changed.
   * The others keep their text and are only marked outdated.
   */
  fun selectForRetranslation(
    projectId: Long,
    translations: Collection<Translation>,
  ): Set<Long>

  fun hasCustomBehavior(projectId: Long): Boolean
}
