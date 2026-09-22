package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification
import org.springframework.stereotype.Service

@Service
class ValueCollectionPropChangesProvider : PropChangesProvider {
  override fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? =
    BaseCollectionChangesProvider(
      old as Collection<Any?>?,
      new as Collection<Any?>?,
    ) { it }.provide()
}
