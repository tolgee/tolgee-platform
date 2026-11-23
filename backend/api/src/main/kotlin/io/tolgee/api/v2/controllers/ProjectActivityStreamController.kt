package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.stream.ActivityStreamFilter
import io.tolgee.activity.stream.ActivityStreamService
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.exceptions.BadRequestException
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
    @RequestParam(required = false) authorIds: List<Long>? = null,
    @RequestParam(required = false) excludeAuthorIds: List<Long>? = null,
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
    if (from != null && to != null && to - from > MAX_RANGE_MS) {
      throw BadRequestException("activity_stream_range_too_long")
    }
    val views =
      activityStreamService.find(
        projectHolder.project.id,
        ActivityStreamFilter(authorIds, excludeAuthorIds, types, languageIds, from, to),
        pageable,
      )
    return activityPagedResourcesAssembler.toModel(views, projectActivityModelAssembler)
  }

  @Operation(summary = "Get the time bounds for filtering project activity")
  @GetMapping("/bounds")
  @RequiresProjectPermissions([Scope.ACTIVITY_VIEW])
  @AllowApiAccess
  fun getBounds(): ActivityStreamBoundsModel {
    return ActivityStreamBoundsModel(
      since = activityStreamService.findSince(projectHolder.project.id),
      maxRangeDays = MAX_RANGE_DAYS,
      presentTypes = activityStreamService.findPresentTypes(projectHolder.project.id),
    )
  }

  companion object {
    const val MAX_RANGE_DAYS = 366
    private const val MAX_RANGE_MS = MAX_RANGE_DAYS * 24L * 60 * 60 * 1000
  }
}

data class ActivityStreamBoundsModel(
  val since: Long?,
  val maxRangeDays: Int,
  @Schema(description = "Activity types that occur at least once in the project's history")
  val presentTypes: List<String>,
)
