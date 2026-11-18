package io.tolgee.service.aiReview

import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation

object SourceChangePolicy {
  private enum class Kind {
    AI_APPROVED,
    HUMAN_REVIEWED,
    MACHINE,
    HUMAN_EDITED,
  }

  fun decide(
    translation: Translation,
    settings: EffectiveSourceChangeSettings,
    protect: Boolean,
    autoTranslated: () -> Boolean,
    rowState: AiReviewRowState,
  ): SourceChangeDecision {
    val kind = kindOf(translation, rowState)
    val keepsState = protect && translation.state == TranslationState.REVIEWED
    if (kind == Kind.MACHINE) return SourceChangeDecision.keepOrTranslated(keepsState)

    var action =
      when (kind) {
        Kind.AI_APPROVED -> settings.aiApproved
        Kind.HUMAN_REVIEWED -> settings.humanReviewed
        else -> settings.humanEdited
      }
    if (action == SourceChangeAction.RETRANSLATE && !autoTranslated()) {
      action = SourceChangeAction.MARK_OUTDATED
    }

    if (action == SourceChangeAction.RETRANSLATE) {
      if (keepsState) return SourceChangeDecision(newState = null)
      return SourceChangeDecision(
        newState = TranslationState.TRANSLATED,
        setAuto = true,
        deleteAiReviewRow = true,
      )
    }

    val dropsToTranslated = translation.state == TranslationState.REVIEWED && !protect
    // "Keep" on an AI approval must clear auto, or softAutoTranslate would still replace the text.
    val clearAuto = kind == Kind.AI_APPROVED && !protect
    return SourceChangeDecision(
      newState =
        when {
          keepsState -> null
          else -> TranslationState.TRANSLATED
        },
      setAuto =
        when {
          clearAuto -> false
          else -> null
        },
      deleteAiReviewRow = dropsToTranslated && rowState.currentApproval,
    )
  }

  private fun kindOf(
    translation: Translation,
    rowState: AiReviewRowState,
  ): Kind {
    if (rowState.aiManaged && translation.auto) return Kind.AI_APPROVED
    if (translation.state == TranslationState.REVIEWED && !rowState.aiManaged) return Kind.HUMAN_REVIEWED
    if (translation.auto) return Kind.MACHINE
    return Kind.HUMAN_EDITED
  }
}
