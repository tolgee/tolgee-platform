package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.data.label.LabelRequest
import io.tolgee.ee.data.translation.TranslationLabelRequest
import io.tolgee.ee.service.LabelServiceImpl
import io.tolgee.hateoas.label.LabelModel
import io.tolgee.hateoas.label.LabelModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Label
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:\\d+}/",
    "/v2/projects/",
  ],
)
@Tag(name = "Translation labels", description = "Operations related to translation labels")
@OpenApiOrderExtension(8)
class TranslationLabelsController(
  private val projectHolder: ProjectHolder,
  private val labelService: LabelServiceImpl,
  private val labelModelAssembler: LabelModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Label>,
  private val translationService: TranslationService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) : IController {
  @GetMapping(value = ["labels"])
  @Operation(summary = "Get available project labels")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAll(
    @RequestParam
    search: String? = null,
    @SortDefault("name")
    @ParameterObject pageable: Pageable,
  ): PagedModel<LabelModel> {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    val data = labelService.getProjectLabels(projectHolder.project.id, pageable, search)
    return pagedResourcesAssembler.toModel(data, labelModelAssembler)
  }

  @GetMapping(value = ["labels/ids"])
  @Operation(summary = "Get labels by ids")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getLabelsByIds(
    @RequestParam("id")
    ids: List<Long>,
  ): List<LabelModel> {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    if (ids.isEmpty()) {
      return emptyList()
    }
    val labels = labelService.getProjectLabelsByIds(projectHolder.project.id, ids)
    return labels.map { it.model }
  }

  @PostMapping(value = ["labels"])
  @Operation(summary = "Create label")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_CREATE)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_MANAGE])
  @AllowApiAccess
  fun createLabel(
    @RequestBody @Valid
    request: LabelRequest,
  ): LabelModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    return labelService.createLabel(projectHolder.project.id, request).model
  }

  @PutMapping(value = ["labels/{labelId:\\d+}"])
  @Operation(summary = "Update label")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_UPDATE)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_MANAGE])
  @AllowApiAccess
  fun updateLabel(
    @PathVariable("labelId")
    labelId: Long,
    @RequestBody @Valid
    request: LabelRequest,
  ): LabelModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    return labelService.updateLabel(projectHolder.project.id, labelId, request).model
  }

  @DeleteMapping(value = ["labels/{labelId:\\d+}"])
  @Operation(summary = "Delete label")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_DELETE)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_MANAGE])
  @AllowApiAccess
  fun deleteLabel(
    @PathVariable("labelId")
    labelId: Long,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    labelService.deleteLabel(projectHolder.project.id, labelId)
  }

  @PutMapping(value = ["translations/label"])
  @Operation(summary = "Add label to translation by key and language id")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_ASSIGN)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_ASSIGN])
  @AllowApiAccess
  fun assignLabel(
    @RequestBody @Valid
    request: TranslationLabelRequest,
  ): LabelModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    val translation =
      translationService.getOrCreate(
        projectHolder.project.id,
        request.keyId,
        request.languageId,
      )
    val label = labelService.assignLabel(projectHolder.project.id, translation, request.labelId).model
    return label
  }

  @PutMapping(value = ["translations/{translationId:\\d+}/label/{labelId:\\d+}"])
  @Operation(summary = "Add label to translation")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_ASSIGN)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_ASSIGN])
  @AllowApiAccess
  fun assignLabel(
    @PathVariable("translationId")
    translationId: Long,
    @PathVariable("labelId")
    labelId: Long,
  ): LabelModel {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    return labelService.assignLabel(projectHolder.project.id, translationId, labelId).model
  }

  @DeleteMapping(value = ["translations/{translationId:\\d+}/label/{labelId:\\d+}"])
  @Operation(summary = "Remove label from translation")
  @RequestActivity(ActivityType.TRANSLATION_LABEL_ASSIGN)
  @RequiresProjectPermissions([Scope.TRANSLATION_LABEL_ASSIGN])
  @AllowApiAccess
  fun unassignLabel(
    @PathVariable("translationId")
    translationId: Long,
    @PathVariable("labelId")
    labelId: Long,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId,
      Feature.TRANSLATION_LABELS,
    )
    labelService.unassignLabel(projectHolder.project.id, translationId, labelId)
  }

  private val Label.model: LabelModel
    get() = labelModelAssembler.toModel(this)
}
