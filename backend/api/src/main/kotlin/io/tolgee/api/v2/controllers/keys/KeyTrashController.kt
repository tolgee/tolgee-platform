package io.tolgee.api.v2.controllers.keys

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.v2.controllers.IController
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.KeyView
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.key.KeyModel
import io.tolgee.hateoas.key.KeyModelAssembler
import io.tolgee.hateoas.key.trash.TrashedKeyModel
import io.tolgee.hateoas.key.trash.TrashedKeyModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.key.KeyService
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
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<Key>,
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
  ): PagedModel<TrashedKeyModel> {
    val data = keyService.getSoftDeletedKeys(projectHolder.project.id, branch, pageable)
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
