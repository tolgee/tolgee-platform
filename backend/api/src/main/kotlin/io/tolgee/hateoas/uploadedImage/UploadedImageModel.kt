package io.tolgee.hateoas.uploadedImage

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "uploadedImages", itemRelation = "uploadedImage")
open class UploadedImageModel(
  val id: Long,
  val filename: String,
  val fileUrl: String,
  val requestFilename: String,
  val createdAt: Date,
  val location: String? = null,
) : RepresentationModel<UploadedImageModel>()
