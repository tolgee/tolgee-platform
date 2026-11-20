package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.stream.ActivityStreamFilter
import io.tolgee.activity.stream.ActivityStreamService
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.hateoas.activity.ProjectActivityModel
import io.tolgee.hateoas.activity.ProjectActivityModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.activity.ProjectActivityView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.ratelimit.RateLimitService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/activity/stream", "/v2/projects/activity/stream"])
@Tag(name = "Projects")
class ProjectActivityStreamController(
  private val activityStreamService: ActivityStreamService,
  private val projectHolder: ProjectHolder,
  private val activityPagedResourcesAssembler: PagedResourcesAssembler<ProjectActivityView>,
  private val projectActivityModelAssembler: ProjectActivityModelAssembler,
  private val rateLimitService: RateLimitService,
  private val tolgeeProperties: TolgeeProperties,
) {
  @Operation(summary = "Get project activity filtered by author, type, language and time")
  @GetMapping("")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getStream(
    @ParameterObject pageable: Pageable,
    @RequestParam(required = false) authorId: Long? = null,
    @RequestParam(required = false) excludeAuthorId: Long? = null,
    @RequestParam(required = false) types: List<String>? = null,
    @RequestParam(required = false) languageIds: List<Long>? = null,
    @RequestParam(required = false) from: Long? = null,
    @RequestParam(required = false) to: Long? = null,
  ): PagedModel<ProjectActivityModel> {
    rateLimitService.checkPerUserRateLimit(
      "activity",
      limit = tolgeeProperties.rateLimits.activityRequestLimit,
      refillDuration = Duration.ofMillis(tolgeeProperties.rateLimits.activityRequestWindow),
    )
    val views =
      activityStreamService.find(
        projectHolder.project.id,
        ActivityStreamFilter(authorId, excludeAuthorId, types, languageIds, from, to),
        pageable,
      )
    return activityPagedResourcesAssembler.toModel(views, projectActivityModelAssembler)
  }
}
