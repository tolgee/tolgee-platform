package io.tolgee.activity.activities

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.BaseTranslationsActivity
import org.springframework.stereotype.Component

@Component
class DismissAutoTranslationStateActivity(
  activityService: ActivityService,
) : BaseTranslationsActivity(activityService) {

  override val type: String = "DISMISS_AUTO_TRANSLATED_STATE"
}
