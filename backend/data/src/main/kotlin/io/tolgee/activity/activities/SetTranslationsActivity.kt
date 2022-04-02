package io.tolgee.activity.activities

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.BaseTranslationsActivity
import org.springframework.stereotype.Component

@Component
class SetTranslationsActivity(
  activityService: ActivityService,
) : BaseTranslationsActivity(activityService) {

  override val type: String = "SET_TRANSLATIONS"
}
