package io.tolgee.service.aiReview

import io.tolgee.dtos.cacheable.ProjectDto
import org.springframework.stereotype.Service

@Service
class SourceChangeSettingsProviderOssStub : SourceChangeSettingsProvider {
  override fun getEffective(project: ProjectDto): EffectiveSourceChangeSettings = EffectiveSourceChangeSettings.DEFAULTS
}
