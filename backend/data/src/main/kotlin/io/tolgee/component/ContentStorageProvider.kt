package io.tolgee.component

import io.tolgee.model.contentDelivery.ContentStorage

interface ContentStorageProvider {
  fun getStorage(
    projectId: Long,
    contentStorageId: Long,
  ): ContentStorage
}
