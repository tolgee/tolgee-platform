package io.tolgee.activity.propChangesProvider

import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.Project
import org.springframework.stereotype.Service

@Service
class GlossaryAssignedProjectsPropChangesProvider : PropChangesProvider {
  override fun getChanges(
    old: Any?,
    new: Any?,
  ): PropertyModification? {
    val baseCollectionChangesProvider =
      BaseCollectionChangesProvider(
        old as Collection<Any?>?,
        new as Collection<Any?>?,
      ) {
        val project = (it as? Project) ?: return@BaseCollectionChangesProvider null
        AssignedProject(project.id, project.name)
      }
    return baseCollectionChangesProvider.provide()
  }

  data class AssignedProject(
    val id: Long,
    val name: String,
  )
}
