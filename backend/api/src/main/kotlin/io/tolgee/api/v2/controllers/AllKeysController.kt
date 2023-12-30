package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.hateoas.key.KeyModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeyService
import org.springframework.hateoas.CollectionModel
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId}/all-keys",
  ],
)
@Tag(name = "All localization keys", description = "All localization keys in the project")
class AllKeysController(
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val keyModelAssembler: KeyModelAssembler,
) : IController {
  @GetMapping(value = [""])
  @Transactional
  @Operation(summary = "Get all keys in project")
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_VIEW ])
  @AllowApiAccess
  fun getAllKeys(): CollectionModel<KeyModel> {
    val allKeys = keyService.getAllSortedById(projectHolder.project.id)
    return keyModelAssembler.toCollectionModel(allKeys)
  }
}
