package io.tolgee.api

interface OnboardingSurveyProvider {
  fun resolveVersion(userAccountId: Long): String?
}
