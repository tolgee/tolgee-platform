package io.tolgee.api.v2.controllers.keys

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.controllers.IController
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.hateoas.key.KeyModelAssembler
import io.tolgee.hateoas.key.trash.TrashedKeyModelAssembler
import io.tolgee.hateoas.key.trash.TrashedKeyWithTranslationsModelAssembler
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeySearchResultView
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.transaction.annotation.Transactional
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
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/keys/trash",
  ],
)
@KeysDocsTag
class KeyTrashController(
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val trashedKeyModelAssembler: TrashedKeyModelAssembler,
  private val keyModelAssembler: KeyModelAssembler,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
  private val authenticationFacade: AuthenticationFacade,
  private val tagService: TagService,
  private val screenshotService: ScreenshotService,
  private val tagModelAssembler: TagModelAssembler,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<KeySearchResultView>,
) : IController {
  @GetMapping("")
  @Transactional
  @Operation(summary = "List trashed keys")
  @RequiresProjectPermissions([Scope.KEYS_VIEW])
  @AllowApiAccess
  fun list(
    @ParameterObject
    @SortDefault("deletedAt")
    pageable: Pageable,
    @RequestParam
    branch: String? = null,
    @RequestParam
    search: String? = null,
    @RequestParam
    languages: Set<String>? = null,
  ): PagedModel<*> {
    val data =
      if (!search.isNullOrBlank()) {
        keyService.searchKeys(search, null, projectHolder.project.id, branch, trashed = true, pageable)
      } else {
        keyService.getSoftDeletedKeys(projectHolder.project.id, branch, pageable)
      }

    if (languages != null) {
      val languageDtos =
        languageService.getLanguagesForTranslationsView(
          languages,
          projectHolder.project.id,
          authenticationFacade.authenticatedUser.id,
        )
      val languageIds = languageDtos.map { it.id }
      val keyIds = data.content.map { it.id }
      val translations =
        if (keyIds.isNotEmpty() && languageIds.isNotEmpty()) {
          translationService.getTranslations(keyIds, languageIds)
        } else {
          emptyList()
        }
      val assembler =
        TrashedKeyWithTranslationsModelAssembler(
          tagModelAssembler = tagModelAssembler,
          screenshotModelAssembler = screenshotModelAssembler,
          translationsByKeyId = translations.groupBy { it.key.id },
          tagsByKeyId = tagService.getTagsForKeyIds(keyIds),
          screenshotsByKeyId =
            if (keyIds.isNotEmpty()) screenshotService.getScreenshotsForKeys(keyIds) else emptyMap(),
        )
      return pagedResourcesAssembler.toModel(data, assembler)
    }

    return pagedResourcesAssembler.toModel(data, trashedKeyModelAssembler)
  }

  @PutMapping("/{keyId}/restore")
  @Transactional
  @Operation(summary = "Restore a trashed key")
  @RequestActivity(ActivityType.KEY_RESTORE)
  @RequiresProjectPermissions([Scope.KEYS_CREATE])
  @AllowApiAccess
  fun restore(
    @PathVariable keyId: Long,
  ): KeyModel {
    val key = keyService.restoreKey(projectHolder.project.id, keyId)
    val view =
      KeyView(
        key.id,
        key.name,
        key.namespace?.name,
        key.keyMeta?.description,
        key.keyMeta?.custom,
        key.branch?.name,
      )
    return keyModelAssembler.toModel(view)
  }

  @DeleteMapping("/{keyId}")
  @Transactional
  @Operation(summary = "Permanently delete a trashed key")
  @RequestActivity(ActivityType.KEY_HARD_DELETE)
  @RequiresProjectPermissions([Scope.KEYS_DELETE])
  @AllowApiAccess
  fun permanentlyDelete(
    @PathVariable keyId: Long,
  ) {
    val key =
      keyService.findSoftDeletedByIdsAndProjectId(listOf(keyId), projectHolder.project.id).firstOrNull()
        ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.hardDeleteMultiple(listOf(key.id))
  }
}
