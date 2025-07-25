package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.translation.Label
import org.springframework.stereotype.Service

@Service
class LabelPropChangesProvider : PropChangesProvider {
  override fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? {
    val baseCollectionChangesProvider =
      BaseCollectionChangesProvider(
        old as Collection<Any?>?,
        (new as Collection<Any?>?)?.sortedBy { (it as Label).name.lowercase() },
      ) { extractLabelProperties(it as? Label) }
    return baseCollectionChangesProvider.provide()
  }

  private fun extractLabelProperties(label: Label?): Map<String, Any?> {
    return mapOf(
      "id" to label?.id,
      "name" to label?.name,
      "color" to label?.color,
      "description" to label?.description,
    )
  }
}
