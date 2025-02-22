package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.model.Organization

interface PayAsYouGoCreditsProvider {
  fun getPayAsYouGoAvailableCredits(organization: Organization?): Long

  fun getUsedPayAsYouGoCredits(organization: Organization?): Long
}
