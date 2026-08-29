package io.tolgee.hateoas.apps

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Whether Tolgee managed to hand the just-disclosed credentials to the app. Present only in the
 * registration and rotation responses, so the dialog can tell the operator whether the app got them
 * or whether they still have to copy the secret by hand.
 */
@Schema(description = "Outcome of pushing the disclosed credentials to the app's base URL")
data class AppDeliveryOutcomeModel(
  @Schema(description = "False when there was nothing to deliver to")
  val attempted: Boolean,
  val delivered: Boolean,
  @Schema(description = "Short reason when the delivery was attempted and failed; null otherwise")
  val error: String?,
)
