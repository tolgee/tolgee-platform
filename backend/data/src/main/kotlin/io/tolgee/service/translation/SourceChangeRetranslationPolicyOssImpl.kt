package io.tolgee.service.translation

import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Service

@Service
class SourceChangeRetranslationPolicyOssImpl : SourceChangeRetranslationPolicy {
  override fun selectForRetranslation(
    projectId: Long,
    translations: Collection<Translation>,
    includeHumanText: Boolean,
  ): Set<Long> = translations.filter { it.auto }.map { it.id }.toSet()

  override fun hasCustomBehavior(projectId: Long): Boolean = false
}
