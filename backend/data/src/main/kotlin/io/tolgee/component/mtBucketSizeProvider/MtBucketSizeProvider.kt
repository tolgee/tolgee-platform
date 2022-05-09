package io.tolgee.component.mtBucketSizeProvider

import io.tolgee.model.Organization

interface MtBucketSizeProvider {
  fun getSize(organization: Organization?): Long
}
