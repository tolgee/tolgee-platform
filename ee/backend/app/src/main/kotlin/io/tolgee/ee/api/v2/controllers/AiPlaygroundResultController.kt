package io.tolgee.ee.api.v2.controllers

import io.tolgee.ee.api.v2.hateoas.assemblers.AiPlaygroundResultModelAssembler
import io.tolgee.ee.data.AiPlaygroundResultRequest
import io.tolgee.ee.service.AiPlaygroundResultServiceEeImpl
import io.tolgee.hateoas.AiPlaygroundResultModel
import io.tolgee.hateoas.NonPagedModel
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/ai-playground-result",
    "/v2/projects/ai-playground-result",
  ],
)
@OpenApiOrderExtension(6)
class AiPlaygroundResultController(
  private val aiPlaygroundResultService: AiPlaygroundResultServiceEeImpl,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val aiPlaygroundResultModelAssembler: AiPlaygroundResultModelAssembler,
) {
  @PostMapping("")
  @RequiresProjectPermissions([io.tolgee.model.enums.Scope.PROMPTS_EDIT])
  fun getAiPlaygroundResult(
    @RequestBody @Valid
    body: AiPlaygroundResultRequest,
  ): NonPagedModel<AiPlaygroundResultModel> {
    val result =
      aiPlaygroundResultService.getResult(
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
        body.keys!!,
        body.languages!!,
      ).map { aiPlaygroundResultModelAssembler.toModel(it) }
    return NonPagedModel(result)
  }
}
