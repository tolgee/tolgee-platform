/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.activity.ActivityService
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.ProjectTranslationLastModifiedManager
import io.tolgee.dtos.queryResults.TranslationHistoryView
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.translations.KeysWithTranslationsPageModel
import io.tolgee.hateoas.translations.KeysWithTranslationsPagedResourcesAssembler
import io.tolgee.hateoas.translations.SetTranslationsResponseModel
import io.tolgee.hateoas.translations.TranslationHistoryModel
import io.tolgee.hateoas.translations.TranslationHistoryModelAssembler
import io.tolgee.hateoas.translations.TranslationModel
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.KeyTaskView
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.ITaskService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.queryBuilders.CursorUtil
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.beans.propertyeditors.CustomCollectionEditor
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import java.util.concurrent.TimeUnit

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translations",
    "/v2/projects/translations",
  ],
)
@Tags(
  value = [
    Tag(name = "Translations", description = "Operations related to translations in project"),
  ],
)
@OpenApiOrderExtension(4)
class TranslationsController(
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
  private val pagedAssembler: KeysWithTranslationsPagedResourcesAssembler,
  private val historyPagedAssembler: PagedResourcesAssembler<TranslationHistoryView>,
  private val historyModelAssembler: TranslationHistoryModelAssembler,
  private val translationModelAssembler: TranslationModelAssembler,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val screenshotService: ScreenshotService,
  private val activityService: ActivityService,
  private val projectTranslationLastModifiedManager: ProjectTranslationLastModifiedManager,
  private val createOrUpdateTranslationsFacade: CreateOrUpdateTranslationsFacade,
  private val taskService: ITaskService,
) : IController {
  @GetMapping(value = ["/{languages}"])
  @Operation(
    summary = "Get all translations",
    description = "Returns all translations for specified languages",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            schema =
              Schema(
                example =
                  """{"en": {"what a key": "Translated value", "another key": "Another key translated"},""" +
                    """"cs": {"what a key": "Překlad", "another key": "Další překlad"}}""",
              ),
          ),
        ],
      ),
    ],
  )
  @UseDefaultPermissions // Security: check performed in the handler
  @AllowApiAccess
  @OpenApiOrderExtension(1)
  fun getAllTranslations(
    @Parameter(
      description =
        "Comma-separated language tags to return translations in. " +
          "Languages you are not permitted to see will be silently dropped and not returned.",
      example = "en,de,fr",
    )
    @PathVariable("languages")
    languages: Set<String>,
    @Parameter(description = "Namespace to return")
    ns: String? = "",
    @Parameter(
      description = """Delimiter to structure response content. 

e.g. For key "home.header.title" would result in {"home": {"header": {"title": "Hello"}}} structure.

When null, resulting file will be a flat key-value object.
    """,
    )
    @RequestParam(value = "structureDelimiter", defaultValue = ".", required = false)
    structureDelimiter: Char?,
    request: WebRequest,
  ): ResponseEntity<Map<String, Any>>? {
    val lastModified: Long = projectTranslationLastModifiedManager.getLastModified(projectHolder.project.id)

    if (request.checkNotModified(lastModified)) {
      return null
    }

    val permittedTags =
      securityService
        .filterViewPermissionByTag(projectId = projectHolder.project.id, languageTags = languages)

    val response =
      translationService.getTranslations(
        languageTags = permittedTags,
        namespace = ns,
        projectId = projectHolder.project.id,
        structureDelimiter = request.getStructureDelimiter(),
      )

    return ResponseEntity.ok()
      .lastModified(lastModified)
      .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS))
      .body(
        response,
      )
  }

  @PutMapping("")
  @Operation(summary = "Update translations for existing key", description = "Sets translations for existing key")
  @RequestActivity(ActivityType.SET_TRANSLATIONS)
  @UseDefaultPermissions
  @AllowApiAccess
  @OpenApiOrderExtension(2)
  fun setTranslations(
    @RequestBody @Valid
    dto: SetTranslationsWithKeyDto,
  ): SetTranslationsResponseModel {
    return createOrUpdateTranslationsFacade.setTranslations(dto)
  }

  @PostMapping("")
  @Operation(
    summary = "Create key or update translations",
    description = "Sets translations for existing key or creates new key and sets the translations to it.",
  )
  @RequestActivity(ActivityType.SET_TRANSLATIONS)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_EDIT])
  @AllowApiAccess
  @Transactional
  @OpenApiOrderExtension(3)
  fun createOrUpdateTranslations(
    @RequestBody @Valid
    dto: SetTranslationsWithKeyDto,
  ): SetTranslationsResponseModel {
    return createOrUpdateTranslationsFacade.createOrUpdateTranslations(dto)
  }

  @PutMapping("/{translationId}/set-state/{state}")
  @Operation(summary = "Set translation state")
  @RequestActivity(ActivityType.SET_TRANSLATION_STATE)
  @UseDefaultPermissions
  @AllowApiAccess
  fun setTranslationState(
    @PathVariable translationId: Long,
    @PathVariable state: AssignableTranslationState,
  ): TranslationModel {
    val translation = translationService.get(translationId)
    translation.checkFromProject()
    securityService.checkStateChangePermission(translation)
    return translationModelAssembler.toModel(translationService.setStateBatch(translation, state.translationState))
  }

  @InitBinder("translationFilters")
  fun customizeBinding(binder: WebDataBinder) {
    binder.registerCustomEditor(
      List::class.java,
      TranslationFilters::filterKeyName.name,
      CustomCollectionEditor(List::class.java),
    )
  }

  @GetMapping(value = [""])
  @Operation(summary = "Get translations in project")
  @RequiresProjectPermissions(scopes = [Scope.KEYS_VIEW]) // Security: check done internally
  @AllowApiAccess
  @Transactional
  @OpenApiOrderExtension(5)
  fun getTranslations(
    @ParameterObject
    @ModelAttribute("translationFilters")
    params: GetTranslationsParams,
    @ParameterObject pageable: Pageable,
  ): KeysWithTranslationsPageModel {
    val languages =
      languageService.getLanguagesForTranslationsView(
        params.languages,
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
      )

    val pageableWithSort = getSafeSortPageable(pageable)

    val data =
      translationService
        .getViewData(projectHolder.project.id, pageableWithSort, params, languages)

    addScreenshotsToResponse(data)
    addTasksToResponse(data)

    val cursor = if (data.content.isNotEmpty()) CursorUtil.getCursor(data.content.last(), data.sort) else null
    return pagedAssembler.toTranslationModel(data, languages, cursor)
  }

  private fun addScreenshotsToResponse(data: Page<KeyWithTranslationsView>) {
    val canViewScreenshots = securityService.currentPermittedScopesContain(Scope.SCREENSHOTS_VIEW)

    if (!canViewScreenshots) {
      return
    }

    val keysWithScreenshots = screenshotService.getScreenshotsForKeys(data.map { it.keyId }.content)

    data.content.forEach { it.screenshots = keysWithScreenshots[it.keyId] ?: listOf() }
  }

  private fun addTasksToResponse(data: Page<KeyWithTranslationsView>) {
    val user = authenticationFacade.authenticatedUser
    val keyIds = data.content.map { key -> key.keyId }

    val translationsWithTasks = taskService.getKeysWithTasks(user.id, keyIds)

    data.content.forEach { key ->
      key.tasks =
        translationsWithTasks[key.keyId]?.map {
          KeyTaskView(
            it.taskNumber,
            it.languageId,
            it.languageTag,
            it.taskDone,
            it.taskAssigned,
            it.taskType,
          )
        }
    }
  }

  @PutMapping(value = ["/{translationId:[0-9]+}/dismiss-auto-translated-state"])
  @Operation(summary = "Dismiss auto-translated", description = """Removes "auto translated" indication""")
  @RequestActivity(ActivityType.DISMISS_AUTO_TRANSLATED_STATE)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT])
  @AllowApiAccess
  fun dismissAutoTranslatedState(
    @PathVariable translationId: Long,
  ): TranslationModel {
    val translation = translationService.get(translationId)
    translation.checkFromProject()
    securityService.checkStateChangePermission(translation)
    translationService.dismissAutoTranslated(translation)
    return translationModelAssembler.toModel(translation)
  }

  @PutMapping(value = ["/{translationId:[0-9]+}/set-outdated-flag/{state}"])
  @Operation(
    summary = "Set outdated value",
    description =
      """Set's "outdated" flag indicating the base translation """ +
        """was changed without updating current translation.""",
  )
  @RequestActivity(ActivityType.SET_OUTDATED_FLAG)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_STATE_EDIT])
  @AllowApiAccess
  fun setOutdated(
    @PathVariable translationId: Long,
    @PathVariable state: Boolean,
  ): TranslationModel {
    val translation = translationService.get(translationId)
    translation.checkFromProject()
    translationService.setOutdated(translation, state)
    return translationModelAssembler.toModel(translation)
  }

  @GetMapping(value = ["/{translationId:[0-9]+}/history"])
  @Operation(
    summary = """Get translation history""",
    description = """Sorting is not supported for supported. It is automatically sorted from newest to oldest.""",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW])
  @AllowApiAccess
  fun getTranslationHistory(
    @PathVariable translationId: Long,
    @ParameterObject
    @SortDefault(sort = ["timestamp"], direction = Sort.Direction.DESC)
    pageable: Pageable,
  ): PagedModel<TranslationHistoryModel> {
    val translation = translationService.get(translationId)
    translation.checkFromProject()
    securityService.checkLanguageViewPermission(projectHolder.project.id, listOf(translation.language.id))
    val translations = activityService.getTranslationHistory(translation.id, pageable)
    return historyPagedAssembler.toModel(translations, historyModelAssembler)
  }

  private fun getSafeSortPageable(pageable: Pageable): Pageable {
    var sort = pageable.sort
    if (sort.getOrderFor(KeyWithTranslationsView::keyId.name) == null) {
      sort = sort.and(Sort.by(Sort.Direction.ASC, KeyWithTranslationsView::keyId.name))
    }

    return PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
  }

  private fun Translation.checkFromProject() {
    if (this.key.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.TRANSLATION_NOT_FROM_PROJECT)
    }
  }

  /**
   * It has to be handled manually since spring returns default value even when empty value provided
   */
  private fun WebRequest.getStructureDelimiter(): Char? {
    val structureDelimiterParam = this.parameterMap["structureDelimiter"]?.first() ?: return '.'
    if (structureDelimiterParam == "") {
      return null
    }
    return structureDelimiterParam.toCharArray().first()
  }
}
