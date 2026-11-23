package io.tolgee.api.v2.controllers

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.model.key.Tag
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.TagService
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TagsFacade(
  private val tagService: TagService,
  private val projectHolder: ProjectHolder,
  private val validator: Validator,
) : McpFacade {
  fun getProjectTags(
    search: String?,
    pageable: Pageable,
  ): Page<Tag> = tagService.getProjectTags(projectHolder.project.id, search, pageable)

  fun tagKey(
    keyId: Long,
    dto: TagKeyDto,
  ): Tag {
    validator.checkValid(dto)
    return tagService.tagKey(projectHolder.project.id, keyId, dto.name.trim())
  }

  fun tagKeysById(keyIdToTags: Map<Long, List<String>>): Map<Long, List<Tag>> {
    val trimmed =
      keyIdToTags.mapValues { (_, tags) ->
        tags.map { tag ->
          validator.checkValid(TagKeyDto(tag))
          tag.trim()
        }
      }
    return tagService.tagKeysById(projectHolder.project.id, trimmed)
  }
}
