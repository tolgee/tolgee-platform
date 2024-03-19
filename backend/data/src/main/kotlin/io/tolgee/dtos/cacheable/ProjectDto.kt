package io.tolgee.dtos.cacheable

import io.tolgee.api.ISimpleProject
import io.tolgee.model.Project
import java.io.Serializable

data class ProjectDto(
  override val name: String,
  override val description: String?,
  override val slug: String?,
  override val id: Long,
  val organizationOwnerId: Long,
  var aiTranslatorPromptDescription: String?,
  override var avatarHash: String? = null,
  override var icuPlaceholders: Boolean,
) : Serializable, ISimpleProject {
  companion object {
    fun fromEntity(entity: Project) =
      ProjectDto(
        name = entity.name,
        description = entity.description,
        slug = entity.slug,
        id = entity.id,
        organizationOwnerId = entity.organizationOwner.id,
        aiTranslatorPromptDescription = entity.aiTranslatorPromptDescription,
        avatarHash = entity.avatarHash,
        icuPlaceholders = entity.icuPlaceholders,
      )
  }
}
