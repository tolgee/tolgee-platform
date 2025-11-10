package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.hateoas.key.KeyModelAssembler
import io.tolgee.hateoas.key.disabledLanguages.KeyDisabledLanguagesModel
import io.tolgee.hateoas.key.disabledLanguages.KeyDisabledLanguagesModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiOrderExtension
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
    "/v2/projects/{projectId:[0-9]+}",
  ],
)
@Tag(name = "All localization keys", description = "All localization keys in the project")
class AllKeysController(
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val keyModelAssembler: KeyModelAssembler,
  private val keyDisabledLanguagesModelAssembler: KeyDisabledLanguagesModelAssembler,
) : IController {
  @OpenApiOrderExtension(order = 0)
  @GetMapping(value = ["/all-keys"])
  @Transactional
  @Operation(summary = "Get all keys in project")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getAllKeys(): CollectionModel<KeyModel> {
    val allKeys = keyService.getAllSortedById(projectHolder.project.id)
    return keyModelAssembler.toCollectionModel(allKeys)
  }

  @OpenApiOrderExtension(order = 1)
  @GetMapping("/all-keys-with-disabled-languages")
  @AllowApiAccess
  @RequiresProjectPermissions([Scope.KEYS_VIEW])
  @Operation(
    summary = "Get disabled languages for all keys in project",
    description =
      "Returns all project key with any disabled language.\n\n" +
        "If key has no disabled language, it is not returned.",
  )
  fun getDisabledLanguages(): CollectionModel<KeyDisabledLanguagesModel> {
    val result = keyService.getDisabledLanguages(projectHolder.project.id)
    return keyDisabledLanguagesModelAssembler.toCollectionModel(result)
  }
}
