package io.tolgee.ee.api.v2.hateoas.cdnStorage

import io.tolgee.ee.api.v2.controllers.CdnStorageController
import io.tolgee.model.cdn.CdnStorage
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class CdnStorageModelAssembler() : RepresentationModelAssemblerSupport<CdnStorage, CdnStorageModel>(
  CdnStorageController::class.java, CdnStorageModel::class.java
) {
  override fun toModel(entity: CdnStorage): CdnStorageModel {
    return CdnStorageModel(
      id = entity.id,
      name = entity.name,
      publicUrlPrefix = entity.publicUrlPrefix,
      s3CdnConfig = entity.s3CdnConfig?.let {
        S3CdnConfigModel(bucketName = it.bucketName, endpoint = it.endpoint, signingRegion = it.signingRegion)
      },
      azureCdnConfig = entity.azureCdnConfig?.let {
        AzureCdnConfigModel(containerName = it.containerName)
      }
    )
  }
}
