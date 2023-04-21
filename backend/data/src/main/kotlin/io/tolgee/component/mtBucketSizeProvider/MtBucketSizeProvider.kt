package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.model.Organization

interface MtBucketSizeProvider {
  fun getSize(organization: Organization?): Long

  fun getPayAsYouGoAvailableCredits(organization: Organization?): Long

  fun getUsedPayAsYouGoCredits(organization: Organization?): Long
}
