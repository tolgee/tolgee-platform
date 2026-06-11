package io.tolgee.service.aiMatchStats

import java.util.Date

/**
 * One Translation activity revision, flattened from `activity_modified_entity.modifications`.
 *
 * [textModified] distinguishes "text not touched in this revision" from "text explicitly set to
 * null" — both surface as a null [textNew] through Postgres `->>`, but only a real string updates
 * the reconstructed current text.
 */
data class AiMatchActivityRow(
  val timestamp: Date,
  val textModified: Boolean,
  val textNew: String?,
  val stateNew: String?,
  val autoNew: Boolean?,
  val mtProviderNew: String?,
  val promptIdNew: Long?,
)
