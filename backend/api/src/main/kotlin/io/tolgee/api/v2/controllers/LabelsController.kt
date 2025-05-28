package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.dtos.request.label.LabelRequest
import io.tolgee.hateoas.label.LabelModel
import io.tolgee.hateoas.label.LabelModelAssembler
import io.tolgee.model.translation.Label
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.label.LabelService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:\\d+}/",
    "/v2/projects/",
  ],
)
@OpenApiTag(name = "Labels", description = "Operations related to labels")
@OpenApiOrderExtension(8)
class LabelsController(
  private val projectHolder: ProjectHolder,
  private val labelService: LabelService,
  private val labelModelAssembler: LabelModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<Label>,
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
    val data = labelService.getProjectLabels(projectHolder.project.id, pageable, search)
    return pagedResourcesAssembler.toModel(data, labelModelAssembler)
  }

  @PostMapping(value = ["labels"])
  @Operation(summary = "Create label")
  @UseDefaultPermissions
  @AllowApiAccess
  fun createLabel(
    @RequestBody @Valid
    request: LabelRequest
  ): LabelModel {
    return labelService.createLabel(projectHolder.project.id, request).model
  }

  @PutMapping(value = ["labels/{labelId:\\d+}"])
  @Operation(summary = "Update label")
  @UseDefaultPermissions
  @AllowApiAccess
  fun updateLabel(
    @PathVariable("labelId")
    labelId: Long,
    @RequestBody @Valid
    request: LabelRequest,
  ): LabelModel {
    return labelService.updateLabel(projectHolder.project.id, labelId, request).model
  }

  @DeleteMapping(value = ["labels/{labelId:\\d+}"])
  @Operation(summary = "Delete label")
  @UseDefaultPermissions
  @AllowApiAccess
  fun deleteLabel(
    @PathVariable("labelId")
    labelId: Long,
  ) {
    labelService.deleteLabel(projectHolder.project.id, labelId)
  }

  @PutMapping(value = ["translations/{translationId:\\d+}/label/{labelId:\\d+}"])
  @Operation(summary = "Add label to translation")
  @UseDefaultPermissions
  @AllowApiAccess
  fun assignLabel(
    @PathVariable("translationId")
    translationId: Long,
    @PathVariable("labelId")
    labelId: Long
  ) {
    labelService.assignLabel(projectHolder.project.id, translationId, labelId)
  }

  @DeleteMapping(value = ["translations/{translationId:\\d+}/label/{labelId:\\d+}"])
  @Operation(summary = "Remove label from translation")
  @UseDefaultPermissions
  @AllowApiAccess
  fun unassignLabel(
    @PathVariable("translationId")
    translationId: Long,
    @PathVariable("labelId")
    labelId: Long
  ) {
    labelService.unassignLabel(projectHolder.project.id, translationId, labelId)
  }

  private val Label.model: LabelModel
    get() = labelModelAssembler.toModel(this)
}
