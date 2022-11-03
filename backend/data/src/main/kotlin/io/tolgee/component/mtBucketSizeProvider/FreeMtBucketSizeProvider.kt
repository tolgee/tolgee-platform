package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class FreeMtBucketSizeProvider(
  private val machineTranslationProperties: MachineTranslationProperties
) : MtBucketSizeProvider {
  override fun getSize(organization: Organization?): Long = machineTranslationProperties.freeCreditsAmount
}
