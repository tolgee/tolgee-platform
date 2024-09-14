package io.tolgee.model.activity

import io.tolgee.activity.data.EntityDescriptionRef

interface ActivityEntityWithDescription {
  var describingRelations: Map<String, EntityDescriptionRef>?

  /**
   * This field is filled by components implementing [io.tolgee.activity.ActivityAdditionalDescriber]
   */
  var additionalDescription: MutableMap<String, Any?>?

  fun initAdditionalDescription(): MutableMap<String, Any?> {
    return additionalDescription ?: let {
      val value = mutableMapOf<String, Any?>()
      additionalDescription = value
      value
    }
  }
}
