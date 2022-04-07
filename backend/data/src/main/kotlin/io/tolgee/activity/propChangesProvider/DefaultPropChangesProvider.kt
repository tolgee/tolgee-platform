package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.PropertyModification
import org.springframework.stereotype.Component

@Component
class DefaultPropChangesProvider : PropChangesProvider {
  override fun getChanges(old: Any?, new: Any?): PropertyModification? {
    if (old != new) {
      return PropertyModification(old, new)
    }
    return null
  }
}
