package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.user_account.PrivateUserAccountModel
import io.tolgee.hateoas.user_account.QuickStartModel
import io.tolgee.hateoas.user_account.QuickStartModelAssembler
import io.tolgee.service.QuickStartService
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/quick-start")
class QuickStartController (
  private val quickStartService: QuickStartService,
  private val quickStartModelAssembler: QuickStartModelAssembler
) {
  @PutMapping("/complete/{step}")
  fun completeGuideStep(@PathVariable("step") step: String): QuickStartModel {
    val entity = quickStartService.completeStep(step) ?: throw NotFoundException()
    return quickStartModelAssembler.toModel(entity)
  }

  @PutMapping("/close")
  fun completeGuide(): QuickStartModel {
    val entity = quickStartService.finish() ?: throw NotFoundException()
    return quickStartModelAssembler.toModel(entity)
  }
}
