package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.service.TranslationSuggestionServiceEeImpl
import io.tolgee.hateoas.translations.TranslationSuggestionModel
import io.tolgee.hateoas.translations.TranslationSuggestionModelAssembler
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translation-suggestion",
    "/v2/projects/translation-suggestion",
  ],
)
@Tag(name = "Translation suggestion")
class SuggestionController(
  private val translationSuggestionService: TranslationSuggestionServiceEeImpl,
  private val projectHolder: ProjectHolder,
  private val translationSuggestionModelAssembler: TranslationSuggestionModelAssembler,
  private val arrayResourcesAssembler: PagedResourcesAssembler<TranslationSuggestion>,
) {
  @PostMapping("")
  @Operation(summary = "Create translation suggestion")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_SUGGEST])
  fun createSuggestion(
    @RequestBody @Valid
    dto: CreateTranslationSuggestionRequest,
  ): TranslationSuggestionModel {
    val projectId = projectHolder.project.id
    val suggestion = translationSuggestionService.createSuggestion(projectId, dto)
    return translationSuggestionModelAssembler.toModel(suggestion)
  }

  @GetMapping("")
  @Operation(summary = "Get suggestions for project")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  fun getSuggestions(
    @ParameterObject
    pageable: Pageable,
    @ParameterObject
    filters: SuggestionFilters,
  ): PagedModel<TranslationSuggestionModel> {
    val projectId = projectHolder.project.id
    val suggestions = translationSuggestionService.getSuggestionsPaged(pageable, projectId, filters)
    return arrayResourcesAssembler.toModel(
      suggestions,
      translationSuggestionModelAssembler
    )
  }
}
