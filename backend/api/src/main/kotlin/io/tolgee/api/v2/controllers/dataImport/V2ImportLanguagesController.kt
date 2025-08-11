/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.dataImport

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.dataImport.ImportLanguageModel
import io.tolgee.hateoas.dataImport.ImportLanguageModelAssembler
import io.tolgee.hateoas.dataImport.ImportTranslationModel
import io.tolgee.hateoas.dataImport.ImportTranslationModelAssembler
import io.tolgee.model.Language
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.language.LanguageService
import io.tolgee.util.Logging
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:\\d+}/import", "/v2/projects/import"])
@ImportDocsTag
class V2ImportLanguagesController(
  private val importService: ImportService,
  private val importLanguageModelAssembler: ImportLanguageModelAssembler,
  private val importTranslationModelAssembler: ImportTranslationModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedTranslationsResourcesAssembler: PagedResourcesAssembler<ImportTranslationView>,
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
) : Logging {
  @GetMapping("/result/languages/{languageId}")
  @Operation(description = "Returns language prepared to import.", summary = "Get import language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportLanguage(
    @PathVariable("languageId") languageId: Long,
  ): ImportLanguageModel {
    checkImportLanguageInProject(languageId)
    val language = importService.findLanguageView(languageId) ?: throw NotFoundException()
    return importLanguageModelAssembler.toModel(language)
  }

  @GetMapping("/result/languages/{languageId}/translations")
  @Operation(description = "Returns translations prepared to import.", summary = "Get translations")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getImportTranslations(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("languageId") languageId: Long,
    @Parameter(
      description =
        "Whether only translations, which are in conflict " +
          "with existing translations should be returned",
    )
    @RequestParam("onlyConflicts", defaultValue = "false")
    onlyConflicts: Boolean = false,
    @Parameter(
      description =
        "Whether only translations with unresolved conflicts" +
          "with existing translations should be returned",
    )
    @RequestParam("onlyUnresolved", defaultValue = "false")
    onlyUnresolved: Boolean = false,
    @Parameter(description = "String to search in translation text or key")
    @RequestParam("search")
    search: String? = null,
    @ParameterObject
    @SortDefault("keyName")
    pageable: Pageable,
  ): PagedModel<ImportTranslationModel> {
    checkImportLanguageInProject(languageId)
    val translations =
      importService.getTranslationsView(
        languageId,
        pageable,
        onlyConflicts,
        onlyUnresolved,
        search,
      )
    return pagedTranslationsResourcesAssembler.toModel(translations, importTranslationModelAssembler)
  }

  @DeleteMapping("/result/languages/{languageId}")
  @Operation(description = "Deletes language prepared to import.", summary = "Delete language")
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun deleteLanguage(
    @PathVariable("languageId") languageId: Long,
  ) {
    val language = checkImportLanguageInProject(languageId)
    this.importService.deleteLanguage(language)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-override")
  @Operation(
    description = "Resolves translation conflict. The old translation will be overridden.",
    summary = "Resolve conflict (override)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long,
  ) {
    resolveTranslation(languageId, translationId, true)
  }

  @PutMapping("/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing")
  @Operation(
    description = "Resolves translation conflict. The old translation will be kept.",
    summary = "Resolve conflict (keep existing)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
    @PathVariable("translationId") translationId: Long,
  ) {
    resolveTranslation(languageId, translationId, false)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-override")
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be overridden.",
    summary = "Resolve all translation conflicts (override)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetOverride(
    @PathVariable("languageId") languageId: Long,
  ) {
    resolveAllOfLanguage(languageId, true)
  }

  @PutMapping("/result/languages/{languageId}/resolve-all/set-keep-existing")
  @Operation(
    description = "Resolves all translation conflicts for provided language. The old translations will be kept.",
    summary = "Resolve all translation conflicts (keep existing)",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resolveTranslationSetKeepExisting(
    @PathVariable("languageId") languageId: Long,
  ) {
    resolveAllOfLanguage(languageId, false)
  }

  @PutMapping("/result/languages/{importLanguageId}/select-existing/{existingLanguageId}")
  @Operation(
    description =
      "Sets existing language to pair with language to import. " +
        "Data will be imported to selected existing language when applied.",
    summary = "Pair existing language",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun selectExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
    @PathVariable("existingLanguageId") existingLanguageId: Long,
  ) {
    val existingLanguage = checkLanguageFromProject(existingLanguageId)
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, existingLanguage)
  }

  @PutMapping("/result/languages/{importLanguageId}/reset-existing")
  @Operation(
    description = "Resets existing language paired with language to import.",
    summary = "Reset existing language pairing",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun resetExistingLanguage(
    @PathVariable("importLanguageId") importLanguageId: Long,
  ) {
    val importLanguage = checkImportLanguageInProject(importLanguageId)
    this.importService.selectExistingLanguage(importLanguage, null)
  }

  private fun resolveAllOfLanguage(
    languageId: Long,
    override: Boolean,
  ) {
    val language = checkImportLanguageInProject(languageId)
    importService.resolveAllOfLanguage(language, override)
  }

  private fun resolveTranslation(
    languageId: Long,
    translationId: Long,
    override: Boolean,
  ) {
    checkImportLanguageInProject(languageId)
    return importService.resolveTranslationConflict(translationId, languageId, override)
  }

  private fun checkLanguageFromProject(languageId: Long): Language {
    val existingLanguage = languageService.getEntity(languageId)
    if (existingLanguage.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return existingLanguage
  }

  private fun checkImportLanguageInProject(languageId: Long): ImportLanguage {
    val language = importService.findLanguage(languageId) ?: throw NotFoundException()
    val languageProjectId = language.file.import.project.id
    if (languageProjectId != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.IMPORT_LANGUAGE_NOT_FROM_PROJECT)
    }
    return language
  }
}
