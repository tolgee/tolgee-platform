package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.EntityDescriptionProvider
import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.EntityWithId
import org.springframework.stereotype.Component

@Component
class DefaultPropChangesProvider(
  val entityDescriptionProvider: EntityDescriptionProvider,
) : PropChangesProvider {
  override fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? {
    if (old != new) {
      return PropertyModification(old.description, new.description)
    }
    return null
  }

  private val Any?.description: Any?
    get() {
      return (this as? EntityWithId)?.let { entityDescriptionProvider.getDescription(this) } ?: this
    }
}
