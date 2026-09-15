package io.tolgee.api

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class OnboardingQuestionnaireVersion(
  @get:JsonValue val wireValue: String,
) {
  DEFAULT("default_v1"),
  INVITED("invited_v1"),
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun fromWireValue(value: String): OnboardingQuestionnaireVersion? = entries.firstOrNull { it.wireValue == value }
  }
}

interface OnboardingSurveyProvider {
  fun resolveVersion(userAccountId: Long): OnboardingQuestionnaireVersion?
}
