package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

/** The result of rotating an app's client secret: the new secret, plus what became of the old one. */
@Relation(itemRelation = "appSecretRotation", collectionRelation = "appSecretRotations")
open class AppSecretRotationModel(
  @Schema(description = "The freshly issued secret, disclosed once here.")
  val secret: AppSecretModel,
  @Schema(
    description =
      "When the previous secret lapses, or null when there was no previous secret to retire.",
  )
  val previousExpiresAt: Long?,
) : RepresentationModel<AppSecretRotationModel>()
