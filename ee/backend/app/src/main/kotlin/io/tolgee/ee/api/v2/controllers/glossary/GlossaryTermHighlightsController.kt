package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermWithTranslationsModelAssembler
import io.tolgee.ee.data.glossary.GlossaryTermHighlightDto
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/glossary-highlights")
@OpenApiUnstableOperationExtension
@Tag(name = "Glossary term highlights")
class GlossaryTermHighlightsController(
  private val projectHolder: ProjectHolder,
  private val glossaryTermService: GlossaryTermService,
  private val modelAssembler: GlossaryTermWithTranslationsModelAssembler,
  private val organizationHolder: OrganizationHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @GetMapping
  @Operation(summary = "Returns glossary term highlights for specified text")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getHighlights(
    @RequestParam("text")
    text: String,
    @RequestParam("languageTag")
    languageTag: String,
  ): CollectionModel<GlossaryTermHighlightDto> {
    enabledFeaturesProvider.checkFeatureEnabled(
      organizationHolder.organization.id,
      Feature.GLOSSARY,
    )

    return glossaryTermService.getHighlights(projectHolder.project.id, text, languageTag).map {
      GlossaryTermHighlightDto(it.position, modelAssembler.toModel(it.value.term))
    }.let { CollectionModel.of(it) }
  }
}
