package io.tolgee.ee.api.v2.controllers.translationMemory

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory.ProjectTranslationMemoryAssignmentModelAssembler
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.ProjectTranslationMemoryAssignmentModel
import io.tolgee.ee.data.translationMemory.AssignSharedTranslationMemoryRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTmSettingsRequest
import io.tolgee.ee.data.translationMemory.UpdateProjectTranslationMemoryAssignmentRequest
import io.tolgee.ee.service.translationMemory.ProjectTranslationMemoryConfigService
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.RequiresFeatures
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translation-memories",
    "/v2/projects/translation-memories",
  ],
)
@Tag(name = "Translation Memory")
class ProjectTranslationMemoryController(
  private val projectHolder: ProjectHolder,
  private val projectTranslationMemoryConfigService: ProjectTranslationMemoryConfigService,
  private val assignmentAssembler: ProjectTranslationMemoryAssignmentModelAssembler,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
) {
  @GetMapping
  @Operation(summary = "List all translation memory assignments for the project")
  @ReadOnlyOperation
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  fun list(): CollectionModel<ProjectTranslationMemoryAssignmentModel> {
    val assignments = projectTranslationMemoryConfigService.getAssignments(projectHolder.project.id)
    return assignmentAssembler.toCollectionModel(assignments)
  }

  @PostMapping("/{translationMemoryId:[0-9]+}")
  @Operation(summary = "Assign a shared translation memory to the project")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_ASSIGN_PROJECT)
  @AllowApiAccess
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun assign(
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: AssignSharedTranslationMemoryRequest,
  ): ProjectTranslationMemoryAssignmentModel {
    val assignment =
      projectTranslationMemoryConfigService.assignSharedTm(projectHolder.project.id, translationMemoryId, dto)
    return assignmentAssembler.toModel(assignment)
  }

  @DeleteMapping("/{translationMemoryId:[0-9]+}")
  @Operation(
    summary = "Unassign a shared translation memory from the project",
    description =
      "Removes the assignment between the project and the shared translation memory. " +
        "When `keepData=true`, entries from the shared TM are first snapshotted into the project's " +
        "own TM so the project retains the data after the disconnect. When `false` (default), the " +
        "assignment is simply removed; the shared TM and its entries remain intact for other projects.",
  )
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_UNASSIGN_PROJECT)
  @AllowApiAccess
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun unassign(
    @PathVariable translationMemoryId: Long,
    @RequestParam(defaultValue = "false") keepData: Boolean,
  ) {
    projectTranslationMemoryConfigService.unassignSharedTm(
      projectHolder.project.id,
      translationMemoryId,
      keepData,
    )
  }

  @PutMapping("/{translationMemoryId:[0-9]+}")
  @Operation(summary = "Update project's translation memory assignment (read/write/priority)")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_UPDATE_PROJECT_CONFIG)
  @AllowApiAccess
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun updateAssignment(
    @PathVariable translationMemoryId: Long,
    @RequestBody @Valid dto: UpdateProjectTranslationMemoryAssignmentRequest,
  ): ProjectTranslationMemoryAssignmentModel {
    val assignment =
      projectTranslationMemoryConfigService.updateAssignment(projectHolder.project.id, translationMemoryId, dto)
    return assignmentAssembler.toModel(assignment)
  }

  @PutMapping("/project-tm-settings")
  @Operation(
    summary = "Update the project's own TM settings",
    description =
      "Sets TM-level flags on the project's own PROJECT-type TM. The shared-TM update endpoint " +
        "rejects PROJECT TMs; this narrow endpoint exists so project admins can toggle the " +
        "`writeOnlyReviewed` flag without org-level privileges.",
  )
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequestActivity(ActivityType.TRANSLATION_MEMORY_UPDATE_PROJECT_CONFIG)
  @AllowApiAccess
  @RequiresFeatures(Feature.TRANSLATION_MEMORY)
  @Transactional
  fun updateProjectTmSettings(
    @RequestBody @Valid dto: UpdateProjectTmSettingsRequest,
  ) {
    translationMemoryManagementService.setProjectTmWriteOnlyReviewed(
      projectHolder.project.id,
      dto.writeOnlyReviewed,
    ) ?: throw NotFoundException()
  }
}
