package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel

/** The result of rotating an app's webhook signing secret — disclosed once, here only. */
open class AppWebhookSecretModel(
  @Schema(
    description =
      "The new webhook signing secret in plaintext. Tolgee signs later deliveries with it; the " +
        "app verifies against it. Shown once — store it like a password.",
  )
  val secret: String,
  @Schema(
    description =
      "Whether the new secret reached the app over the lifecycle channel. The rotation delivery is " +
        "signed with the previous secret, so an app already running takes the new one automatically.",
  )
  val delivery: AppDeliveryOutcomeModel? = null,
) : RepresentationModel<AppWebhookSecretModel>()
