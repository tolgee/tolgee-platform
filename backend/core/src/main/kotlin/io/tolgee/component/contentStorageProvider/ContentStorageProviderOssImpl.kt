package io.tolgee.component.contentStorageProvider

import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.stereotype.Component

@Component
class ContentStorageProviderOssImpl : ContentStorageProvider {
  override fun getStorage(
    projectId: Long,
    contentStorageId: Long,
  ): ContentStorage {
    throw NotImplementedInOss()
  }
}
