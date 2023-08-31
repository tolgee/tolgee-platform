package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.BigMetaDto
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.bigMeta.BigMetaService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@RequestMapping(
  value = [
    "/v2/projects/{projectId:\\d+}",
    "/v2/projects"
  ]
)
@Tag(name = "Big Meta data about the keys in project")
class BigMetaController(
  private val bigMetaService: BigMetaService,
  private val projectHolder: ProjectHolder,
) {
  @PostMapping("/big-meta")
  @Operation(summary = "Stores a bigMeta for a project")
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_EDIT ])
  @AllowApiAccess
  fun store(@RequestBody @Valid data: BigMetaDto) {
    bigMetaService.store(data, projectHolder.projectEntity)
  }
}
