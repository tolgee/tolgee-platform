/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.groups.ActivityGroupService
import io.tolgee.activity.groups.viewProviders.createKey.CreateKeyGroupItemModel
import io.tolgee.activity.groups.viewProviders.createKey.CreateKeyGroupModelProvider
import io.tolgee.activity.groups.viewProviders.generic.GenericGroupItemModel
import io.tolgee.activity.groups.viewProviders.generic.GenericGroupModelProvider
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.PagedModel.PageMetadata
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/activity/group-items", "/v2/projects/activity/group-items"])
@Tag(name = "Activity Groups")
class ProjectActivityGroupItemsController(
  private val createKeyGroupModelProvider: CreateKeyGroupModelProvider,
  private val genericGroupModelProvider: GenericGroupModelProvider,
  private val activityGroupService: ActivityGroupService,
  private val projectHolder: ProjectHolder,
) {
  @Operation(
    summary = "Get CREATE_KEY group items",
  )
  @GetMapping("/create-key/{groupId}", produces = [MediaTypes.HAL_JSON_VALUE])
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getCreateKeyItems(
    @ParameterObject pageable: Pageable,
    @PathVariable groupId: Long,
  ): PagedModel<CreateKeyGroupItemModel> {
    val data = createKeyGroupModelProvider.provideItems(groupId, pageable)
    return PagedModel.of(
      data.content,
      PageMetadata(data.pageable.pageSize.toLong(), data.pageable.pageNumber.toLong(), data.totalElements),
    )
  }

  @Operation(
    summary = "Get group items",
    description = "Returns the modified entities of the group as generic items (any group type).",
  )
  @GetMapping("/generic/{groupId}", produces = [MediaTypes.HAL_JSON_VALUE])
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getGenericItems(
    @ParameterObject pageable: Pageable,
    @PathVariable groupId: Long,
  ): PagedModel<GenericGroupItemModel> {
    val group = activityGroupService.getGroupInProject(groupId, projectHolder.project.id)
    val data = genericGroupModelProvider.provideItems(group.type, groupId, pageable)
    return PagedModel.of(
      data.content,
      PageMetadata(data.pageable.pageSize.toLong(), data.pageable.pageNumber.toLong(), data.totalElements),
    )
  }
}
