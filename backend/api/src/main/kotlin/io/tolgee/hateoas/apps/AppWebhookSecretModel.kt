package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel

/** The app's webhook signing secret — revealed on read, or freshly minted on rotation. */
open class AppWebhookSecretModel(
  @Schema(
    description =
      "The webhook signing secret in plaintext. Tolgee signs lifecycle deliveries with it; the " +
        "app verifies against it. On rotation this is the new secret — store it like a password.",
  )
  val secret: String,
  @Schema(
    description =
      "Whether the new secret reached the app over the lifecycle channel. Present only on rotation; " +
        "the rotation delivery is signed with the previous secret, so an app already running takes " +
        "the new one automatically.",
  )
  val delivery: AppDeliveryOutcomeModel? = null,
) : RepresentationModel<AppWebhookSecretModel>()
