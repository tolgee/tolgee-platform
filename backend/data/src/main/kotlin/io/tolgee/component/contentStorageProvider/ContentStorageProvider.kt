package io.tolgee.component.contentStorageProvider

import io.tolgee.model.contentDelivery.ContentStorage

interface ContentStorageProvider {
  fun getStorage(
    projectId: Long,
    contentStorageId: Long,
  ): ContentStorage
}
