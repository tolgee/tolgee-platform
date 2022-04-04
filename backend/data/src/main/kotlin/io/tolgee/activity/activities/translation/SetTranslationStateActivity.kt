package io.tolgee.activity.activities.translation

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.BaseTranslationsActivity
import org.springframework.stereotype.Component

@Component
class SetTranslationStateActivity(
  activityService: ActivityService,
) : BaseTranslationsActivity(activityService) {

  override val type: String = "SET_TRANSLATION_STATE"
}
