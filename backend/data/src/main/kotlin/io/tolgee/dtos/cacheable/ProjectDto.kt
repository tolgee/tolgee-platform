package io.tolgee.dtos.cacheable

import io.tolgee.model.Project
import java.io.Serializable

data class ProjectDto(
  val name: String,
  val description: String?,
  val slug: String?,
  val id: Long,
  val organizationOwnerId: Long,
) : Serializable {
  companion object {
    fun fromEntity(entity: Project) =
      ProjectDto(
        name = entity.name,
        description = entity.description,
        slug = entity.slug,
        id = entity.id,
        organizationOwnerId = entity.organizationOwner.id,
      )
  }
}
