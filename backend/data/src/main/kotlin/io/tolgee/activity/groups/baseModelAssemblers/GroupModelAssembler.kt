package io.tolgee.activity.groups.baseModelAssemblers

import io.tolgee.activity.groups.data.ActivityEntityView

interface GroupModelAssembler<T> {
  fun toModel(entity: ActivityEntityView): T
}
