package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel

/** The result of rolling an app's client secret: the new secret, plus what became of the old one. */
open class AppSecretRotationModel(
  @Schema(description = "The freshly issued secret, disclosed once here, with its delivery outcome.")
  val secret: AppSecretModel,
  @Schema(
    description =
      "When the previous secret lapses, or null when it was revoked at once — because the app took " +
        "the new secret over the lifecycle channel, or no grace window was requested.",
  )
  val previousExpiresAt: Long?,
) : RepresentationModel<AppSecretRotationModel>()
