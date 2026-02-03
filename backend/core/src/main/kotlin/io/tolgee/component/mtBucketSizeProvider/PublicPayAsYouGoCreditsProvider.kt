package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class PublicPayAsYouGoCreditsProvider : PayAsYouGoCreditsProvider {
  override fun getPayAsYouGoAvailableCredits(organization: Organization?): Long = 0

  override fun getUsedPayAsYouGoCredits(organization: Organization?): Long = 0
}
