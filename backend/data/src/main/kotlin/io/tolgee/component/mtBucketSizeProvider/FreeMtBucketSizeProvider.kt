package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class FreeMtBucketSizeProvider(private val tolgeeProperties: TolgeeProperties) : MtBucketSizeProvider {
  override fun getSize(organization: Organization?): Long = tolgeeProperties.machineTranslation.freeCreditsAmount
}
