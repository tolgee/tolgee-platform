package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.AiPlaygroundResultModelAssembler
import io.tolgee.ee.data.AiPlaygroundResultRequest
import io.tolgee.ee.service.AiPlaygroundResultServiceEeImpl
import io.tolgee.hateoas.AiPlaygroundResultModel
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/ai-playground-result",
    "/v2/projects/ai-playground-result",
  ],
)
@Tag(name = "Ai Playground result controller")
@OpenApiOrderExtension(6)
class AiPlaygroundResultController(
  private val aiPlaygroundResultService: AiPlaygroundResultServiceEeImpl,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val aiPlaygroundResultModelAssembler: AiPlaygroundResultModelAssembler,
) {
  @PostMapping("")
  @RequiresProjectPermissions([io.tolgee.model.enums.Scope.PROMPTS_EDIT])
  @Operation(summary = "Get ai playground result")
  fun getAiPlaygroundResult(
    @RequestBody @Valid
    body: AiPlaygroundResultRequest,
  ): CollectionModel<AiPlaygroundResultModel> {
    val result =
      aiPlaygroundResultService.getResult(
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
        body.keys!!,
        body.languages!!,
      )
    return aiPlaygroundResultModelAssembler.toCollectionModel(result)
  }
}
