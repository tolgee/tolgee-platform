package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.BigMetaDto
import io.tolgee.hateoas.key.KeyWithBaseTranslationModel
import io.tolgee.hateoas.key.KeyWithBaseTranslationModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.util.Logging
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@RequestMapping(
  value = ["/v2/projects/{projectId:\\d+}", "/v2/projects"],
)
@Tag(
  name = "Big Meta",
  description =
    "Handles big meta (context) for translation keys. Tolgee automatically stores " +
      "contextual data about keys to provide this information to Tolgee AI translator.",
)
class BigMetaController(
  private val bigMetaService: BigMetaService,
  private val projectHolder: ProjectHolder,
  private val keyWithBaseTranslationModelAssembler: KeyWithBaseTranslationModelAssembler,
) : Logging {
  @PostMapping("/big-meta")
  @Operation(summary = "Store Big Meta", description = "Stores a bigMeta for a project")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  fun store(
    @RequestBody @Valid
    data: BigMetaDto,
  ) {
    bigMetaService.store(data, projectHolder.projectEntity)
  }

  @GetMapping("/keys/{id}/big-meta")
  @Operation(summary = "Get Big Meta for key")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getBigMeta(
    @PathVariable("id") id: Long,
  ): CollectionModel<KeyWithBaseTranslationModel> {
    val projectId = projectHolder.project.id
    val result = bigMetaService.getCloseKeysWithBaseTranslation(id, projectId)
    return keyWithBaseTranslationModelAssembler.toCollectionModel(result)
  }
}
