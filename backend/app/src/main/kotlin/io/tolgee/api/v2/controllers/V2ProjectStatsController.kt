/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.service.ProjectStatsService
import org.springframework.hateoas.MediaTypes
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId}/stats", "/v2/projects/stats"])
@Tag(name = "Projects")
class V2ProjectStatsController(
  private val projectStatsService: ProjectStatsService
) {

  @Operation(summary = "Returns project stats")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  fun getProjectStats(projectId: Long) {
    val projectStats = projectStatsService.getProjectStats(projectId)
    val languageStats = projectStatsService.getProjectStats(projectId)
  }
}
