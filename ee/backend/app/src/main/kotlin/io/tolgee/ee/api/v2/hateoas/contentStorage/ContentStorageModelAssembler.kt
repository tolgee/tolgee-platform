package io.tolgee.ee.api.v2.hateoas.contentStorage

import io.tolgee.ee.api.v2.controllers.ContentStorageController
import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ContentStorageModelAssembler() : RepresentationModelAssemblerSupport<ContentStorage, ContentStorageModel>(
  ContentStorageController::class.java, ContentStorageModel::class.java
) {
  override fun toModel(entity: ContentStorage): ContentStorageModel {
    return ContentStorageModel(
      id = entity.id,
      name = entity.name,
      publicUrlPrefix = entity.publicUrlPrefix,
      s3ContentStorageConfig = entity.s3ContentStorageConfig?.let {
        S3ContentStorageConfigModel(bucketName = it.bucketName, endpoint = it.endpoint, signingRegion = it.signingRegion)
      },
      azureContentStorageConfig = entity.azureContentStorageConfig?.let {
        AzureContentStorageConfigModel(containerName = it.containerName)
      }
    )
  }
}
