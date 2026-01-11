package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification

interface PropChangesProvider {
  fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? {
    return null
  }
}
