package io.tolgee.activity.groups.baseModelAssemblers

import io.tolgee.activity.groups.baseModels.ActivityGroupKeyModel
import io.tolgee.activity.groups.data.ActivityEntityView
import io.tolgee.activity.groups.data.getAdditionalDescriptionFieldNullable
import io.tolgee.activity.groups.data.getFieldFromView
import io.tolgee.model.key.Key

class ActivityKeyGroupModelAssembler : GroupModelAssembler<ActivityGroupKeyModel> {
  override fun toModel(entity: ActivityEntityView): ActivityGroupKeyModel {
    return ActivityGroupKeyModel(
      id = entity.entityId,
      name = entity.getFieldFromView(Key::name.name),
      namespace = null,
      baseTranslationText = entity.getAdditionalDescriptionFieldNullable("baseTranslation", "text"),
    )
  }
}
