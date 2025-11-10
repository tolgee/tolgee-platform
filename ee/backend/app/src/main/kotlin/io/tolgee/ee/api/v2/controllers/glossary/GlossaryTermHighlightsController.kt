package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.GlossaryTermHighlightModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermHighlightModel
import io.tolgee.ee.data.glossary.GlossaryHighlightsRequest
import io.tolgee.ee.service.glossary.GlossaryTermService
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiUnstableOperationExtension
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/glossary-highlights")
@OpenApiUnstableOperationExtension
@Tag(name = "Glossary term highlights")
class GlossaryTermHighlightsController(
  private val projectHolder: ProjectHolder,
  private val glossaryTermService: GlossaryTermService,
  private val modelAssembler: GlossaryTermHighlightModelAssembler,
  private val organizationHolder: OrganizationHolder,
) {
  @PostMapping
  @Operation(summary = "Returns glossary term highlights for specified text")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.GLOSSARY)
  fun getHighlights(
    @RequestBody @Valid
    dto: GlossaryHighlightsRequest,
  ): CollectionModel<GlossaryTermHighlightModel> {
    val highlights =
      glossaryTermService.getHighlights(
        organizationHolder.organization.id,
        projectHolder.project.id,
        dto.text,
        dto.languageTag,
      )
    return modelAssembler.toCollectionModel(highlights)
  }
}
