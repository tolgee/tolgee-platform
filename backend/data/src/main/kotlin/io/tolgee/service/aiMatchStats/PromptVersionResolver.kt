package io.tolgee.service.aiMatchStats

import java.util.Date

/**
 * Attributes an AI translation to the prompt version that was live when it was produced.
 *
 * Tolgee keeps one row per prompt id (editing a prompt mutates in place), but `Prompt` is
 * activity-logged, so each output-affecting edit is a dated boundary. The version for a translation
 * is the latest boundary at or before the moment its AI text was produced. This is historically
 * stable: a later edit only appends a future boundary, it never moves a past translation.
 *
 * @param boundariesByPrompt prompt id -> ascending list of version-boundary timestamps.
 */
class PromptVersionResolver(
  private val boundariesByPrompt: Map<Long, List<Date>>,
) {
  fun resolve(
    promptId: Long?,
    producedAt: Date?,
  ): Date? {
    if (promptId == null) return null
    val boundaries = boundariesByPrompt[promptId]
    if (boundaries.isNullOrEmpty()) return null
    if (producedAt == null) return boundaries.first()
    return boundaries.lastOrNull { !it.after(producedAt) } ?: boundaries.first()
  }
}
