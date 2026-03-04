package io.tolgee.api.v2.controllers.keys

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.controllers.IController
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.hateoas.key.KeyModelAssembler
import io.tolgee.hateoas.key.trash.TrashedKeyWithTranslationsModel
import io.tolgee.hateoas.key.trash.TrashedKeyWithTranslationsModelAssembler
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.KeyTrashService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
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
    "/v2/projects/keys/trash",
  ],
)
@KeysDocsTag
class KeyTrashController(
  private val keyService: KeyService,
  private val keyTrashService: KeyTrashService,
  private val projectHolder: ProjectHolder,
  private val keyModelAssembler: KeyModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
  private val trashedKeyWithTranslationsModelAssembler: TrashedKeyWithTranslationsModelAssembler,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<KeyWithTranslationsView>,
) : IController {
  @GetMapping("")
  @Operation(summary = "List trashed keys")
  @RequiresProjectPermissions([Scope.KEYS_VIEW])
  @AllowApiAccess
  fun list(
    @ParameterObject
    @SortDefault("deletedAt")
    pageable: Pageable,
    @ParameterObject
    @ModelAttribute
    params: TranslationFilters,
  ): PagedModel<TrashedKeyWithTranslationsModel> {
    val data =
      keyTrashService.getTrashedKeysWithTranslations(
        projectId = projectHolder.project.id,
        userId = authenticationFacade.authenticatedUser.id,
        pageable = pageable,
        params = params,
      )
    return pagedResourcesAssembler.toModel(data, trashedKeyWithTranslationsModelAssembler)
  }

  @GetMapping("/deleters")
  @Operation(summary = "List users who deleted keys")
  @RequiresProjectPermissions([Scope.KEYS_VIEW])
  @AllowApiAccess
  fun listDeleters(
    @RequestParam(required = false) branch: String?,
  ): CollectionModel<SimpleUserAccountModel> {
    val users = keyService.findDistinctDeleters(projectHolder.project.id, branch)
    return simpleUserAccountModelAssembler.toCollectionModel(users)
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
    val projectIds = keyService.getSoftDeletedProjectIdsForKeyIds(listOf(keyId))
    if (projectIds.isEmpty() || projectIds.single() != projectHolder.project.id) {
      throw NotFoundException(Message.KEY_NOT_FOUND)
    }
    keyService.hardDeleteMultiple(listOf(keyId))
  }
}
