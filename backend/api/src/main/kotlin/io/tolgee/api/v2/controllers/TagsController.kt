package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.dtos.request.ComplexTagKeysRequest
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Tag
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.key.TagService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
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
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:\\d+}/",
    "/v2/projects/",
  ],
)
@OpenApiTag(name = "Tags", description = "Manipulates key tags")
@OpenApiOrderExtension(6)
class TagsController(
  private val projectHolder: ProjectHolder,
  private val tagService: TagService,
  private val tagModelAssembler: TagModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Tag>,
) : IController {
  @PutMapping(value = ["keys/{keyId:[0-9]+}/tags"])
  @Operation(
    summary = "Tag key",
    description = "Tags a key with tag. If tag with provided name doesn't exist, it is created",
  )
  @RequestActivity(ActivityType.KEY_TAGS_EDIT)
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @AllowApiAccess
  fun tagKey(
    @PathVariable
    keyId: Long,
    @Valid @RequestBody
    tagKeyDto: TagKeyDto,
  ): TagModel {
    return tagService.tagKey(projectHolder.project.id, keyId, tagKeyDto.name.trim()).model
  }

  @DeleteMapping(value = ["keys/{keyId:[0-9]+}/tags/{tagId:[0-9]+}"])
  @Operation(summary = "Remove tag", description = "Removes tag with provided id from key with provided id")
  @RequestActivity(ActivityType.KEY_TAGS_EDIT)
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @AllowApiAccess
  fun removeTag(
    @PathVariable keyId: Long,
    @PathVariable tagId: Long,
  ) {
    return tagService.removeTag(projectHolder.project.id, keyId, tagId)
  }

  @GetMapping(value = ["tags"])
  @Operation(summary = "Get tags")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAll(
    @RequestParam search: String? = null,
    @SortDefault("name") @ParameterObject pageable: Pageable,
  ): PagedModel<TagModel> {
    val data = tagService.getProjectTags(projectHolder.project.id, search, pageable)
    return pagedResourcesAssembler.toModel(data, tagModelAssembler)
  }

  @PutMapping("tag-complex")
  @Operation(summary = "Execute complex tag operation")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_EDIT])
  @RequestActivity(ActivityType.COMPLEX_TAG_OPERATION)
  fun executeComplexTagOperation(
    @RequestBody req: ComplexTagKeysRequest,
  ) {
    tagService.complexTagOperation(projectHolder.project.id, req)
  }

  private val Tag.model: TagModel
    get() = tagModelAssembler.toModel(this)
}
