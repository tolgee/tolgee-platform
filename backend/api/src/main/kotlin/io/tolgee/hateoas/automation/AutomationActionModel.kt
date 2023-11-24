package io.tolgee.hateoas.automation

import io.tolgee.hateoas.contentDelivery.ContentDeliveryConfigModel
import io.tolgee.model.automations.AutomationActionType

class AutomationActionModel(
  var id: Long,
  var type: AutomationActionType,
) {
  var contentDeliveryConfig: ContentDeliveryConfigModel? = null
}
