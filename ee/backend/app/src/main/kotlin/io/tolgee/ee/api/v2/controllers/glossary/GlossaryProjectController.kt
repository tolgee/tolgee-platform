package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.glossary.SimpleGlossaryModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryModel
import io.tolgee.ee.service.glossary.GlossaryService
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/projects/{projectId:[0-9]+}/glossaries")
@Tag(name = "Glossary")
class GlossaryProjectController(
  private val projectHolder: ProjectHolder,
  private val glossaryService: GlossaryService,
  private val simpleGlossaryModelAssembler: SimpleGlossaryModelAssembler,
) {
  @GetMapping
  @Operation(summary = "Get glossaries assigned to project")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.GLOSSARY)
  fun getAssignedGlossaries(): CollectionModel<SimpleGlossaryModel> {
    val project = projectHolder.project
    val organizationId = project.organizationOwnerId
    val glossaries = glossaryService.findAssignedToProject(organizationId, project.id)
    return simpleGlossaryModelAssembler.toCollectionModel(glossaries)
  }
}
