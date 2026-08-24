package io.tolgee.dtos.request.apps

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/** Owner-driven roll of an app's client secret. */
data class RollAppSecretRequest(
  @Schema(
    description =
      "How long the outgoing secret keeps working — the window the app (or an operator copying the " +
        "secret in by hand) has to move to the new one. There is no immediate cutover: whether the " +
        "app really adopted a delivered secret is unknowable, so the old one always lives out its " +
        "window unless revoked explicitly. Capped at 7 days.",
  )
  @field:Min(1)
  @field:Max(SEVEN_DAYS_SECONDS)
  val graceSeconds: Long = ONE_DAY_SECONDS,
) {
  companion object {
    const val ONE_DAY_SECONDS = 86_400L
    const val SEVEN_DAYS_SECONDS = 604_800L
  }
}
