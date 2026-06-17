package io.tolgee.hateoas.uploadedImage

import org.springframework.hateoas.RepresentationModel

class UploadedImageMcpModel(
  val uploadedImageId: Long,
) : RepresentationModel<UploadedImageMcpModel>()
