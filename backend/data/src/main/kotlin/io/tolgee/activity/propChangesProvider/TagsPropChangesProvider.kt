package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.PropertyModification
import io.tolgee.model.key.Tag
import org.springframework.stereotype.Service

@Service
class TagsPropChangesProvider : PropChangesProvider {

  override fun getChanges(old: Any?, new: Any?): PropertyModification? {
    if (old is Collection<*> && new is Collection<*>) {
      val oldTagNames = mapSetToTagNames(old)
      val newTagNames = mapSetToTagNames(new)
      if (oldTagNames.containsAll(newTagNames) && newTagNames.containsAll(oldTagNames)) {
        return null
      }

      return PropertyModification(oldTagNames, newTagNames)
    }
    return null
  }

  private fun mapSetToTagNames(collection: Collection<*>) = collection.mapNotNull { (it as? Tag)?.name }
}
