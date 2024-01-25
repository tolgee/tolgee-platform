package io.tolgee.api.v2.controllers

import io.tolgee.activity.ActivityHolder
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.SlackCommandDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.project.ProjectService
import io.tolgee.service.slackIntegration.SlackConfigService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/slack/events"])
class SlackIntegrationController(
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
  private val activityHolder: ActivityHolder,
  private val slackConfigService: SlackConfigService
) {

  @PostMapping("/subscribe")
  @UseDefaultPermissions
  @AllowApiAccess
  fun subscribe(
    @ModelAttribute payload: SlackCommandDto
  ): SlackMessageDto {
    val project = projectService.get(payload.text.toLong())


    projectHolder.project = ProjectDto.fromEntity(project)
    activityHolder.activityRevision.projectId = projectHolder.project.id
    slackConfigService.create(project = project, payload)
    return SlackMessageDto(
      text = "subscribed"
    )
    //TODO handle error
  }

}
