package io.tolgee.component

import io.tolgee.model.cdn.CdnStorage

interface CdnStorageProvider {
  fun getStorage(projectId: Long, cdnStorageId: Long): CdnStorage
}
