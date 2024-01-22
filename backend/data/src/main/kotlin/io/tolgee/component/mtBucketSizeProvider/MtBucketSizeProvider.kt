package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.model.Organization

interface MtBucketSizeProvider {
  fun getSize(organization: Organization?): Long

  fun getPayAsYouGoAvailableCredits(organization: Organization?): Long

  fun isPayAsYouGo(organization: Organization?): Boolean

  fun getUsedPayAsYouGoCredits(organization: Organization?): Long
}
