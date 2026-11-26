package io.tolgee.api.v2.controllers

import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.mcp.checkValid
import io.tolgee.dtos.BigMetaDto
import io.tolgee.security.ProjectHolder
import io.tolgee.service.bigMeta.BigMetaService
import jakarta.validation.Validator
import org.springframework.stereotype.Service

@Service
class BigMetaFacade(
  private val bigMetaService: BigMetaService,
  private val projectHolder: ProjectHolder,
  private val validator: Validator,
) : McpFacade {
  fun store(dto: BigMetaDto) {
    validator.checkValid(dto)
    bigMetaService.store(dto, projectHolder.projectEntity)
  }
}
