package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.TagService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/",
    "/v2/projects/"
  ]
)
@OpenApiTag(name = "Tags", description = "Manipulates key tags")
class TagController(
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val tagService: TagService,
  private val tagModelAssembler: TagModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Tag>
) : IController {

  @PutMapping(value = ["keys/{keyId:[0-9]+}/tags"])
  @Operation(summary = "Tags a key with tag. If tag with provided name doesn't exist, it is created")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  fun tagKey(@PathVariable keyId: Long, @Valid @RequestBody tagKeyDto: TagKeyDto): TagModel {
    val key = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    key.checkInProject()
    return tagService.tagKey(key, tagKeyDto.name.trim()).model
  }

  @DeleteMapping(value = ["keys/{keyId:[0-9]+}/tags/{tagId:[0-9]+}"])
  @Operation(summary = "Removes tag with provided id from key with provided id")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.EDIT)
  @AccessWithApiKey(scopes = [ApiScope.KEYS_EDIT])
  fun removeTag(@PathVariable keyId: Long, @PathVariable tagId: Long) {
    val key = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    val tag = tagService.find(tagId) ?: throw NotFoundException()
    tag.checkInProject()
    key.checkInProject()
    return tagService.remove(key, tag)
  }

  @GetMapping(value = ["tags"])
  @Operation(summary = "Returns project tags")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.VIEW)
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  fun getAll(
    @RequestParam search: String? = null,
    @ParameterObject pageable: Pageable
  ): PagedModel<TagModel> {
    val data = tagService.getProjectTags(projectHolder.project.id, search, pageable)
    return pagedResourcesAssembler.toModel(data, tagModelAssembler)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private fun Tag.checkInProject() {
    if (this.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.TAG_NOT_FROM_PROJECT)
    }
  }

  private val Tag.model: TagModel
    get() = tagModelAssembler.toModel(this)
}
