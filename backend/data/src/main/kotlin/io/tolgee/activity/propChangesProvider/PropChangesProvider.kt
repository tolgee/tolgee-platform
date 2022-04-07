package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.PropertyModification

interface PropChangesProvider {
  fun getChanges(old: Any?, new: Any?): PropertyModification? {
    return null
  }
}
