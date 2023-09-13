package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.invitation.TranslationMemoryItemModelAssembler
import io.tolgee.constants.Message
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.hateoas.machineTranslation.SuggestResultModel
import io.tolgee.hateoas.machineTranslation.TranslationItemModel
import io.tolgee.hateoas.translationMemory.TranslationMemoryItemModel
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationMemoryService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}/suggest", "/v2/projects/suggest"])
@Tag(name = "Translation suggestion")
@Suppress("SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection")
class TranslationSuggestionController(
  private val projectHolder: ProjectHolder,
  private val languageService: LanguageService,
  private val keyService: KeyService,
  private val mtService: MtService,
  private val mtCreditBucketService: MtCreditBucketService,
  private val translationMemoryService: TranslationMemoryService,
  private val translationMemoryItemModelAssembler: TranslationMemoryItemModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val arraytranslationMemoryItemModelAssembler: PagedResourcesAssembler<TranslationMemoryItemView>,
  private val securityService: SecurityService
) {
  @PostMapping("/machine-translations")
  @Operation(summary = "Suggests machine translations from enabled services")
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_EDIT ])
  @AllowApiAccess
  fun suggestMachineTranslations(@RequestBody @Valid dto: SuggestRequestDto): SuggestResultModel {
    val targetLanguage = languageService.get(dto.targetLanguageId)
    securityService.checkLanguageTranslatePermission(projectHolder.project.id, listOf(targetLanguage.id))

    val balanceBefore = mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)
    try {
      val resultMap = dto.baseText?.ifBlank { null }?.let {
        mtService.getMachineTranslations(projectHolder.projectEntity, it, targetLanguage, dto.services)
      } ?: let {
        val key = keyService.findOptional(dto.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }
        key.checkInProject()
        mtService.getMachineTranslations(key, targetLanguage, dto.services)
      }

      val resultData = resultMap
        ?.map { (key, value) ->
          key to TranslationItemModel(value?.translatedText.orEmpty(), value?.contextDescription)
        }?.toMap()

      val balanceAfter = mtCreditBucketService.getCreditBalances(projectHolder.projectEntity)

      return SuggestResultModel(
        machineTranslations = resultData?.map { it.key to it.value.output }?.toMap(),
        result = resultData,
        translationCreditsBalanceBefore = balanceBefore.creditBalance / 100,
        translationCreditsBalanceAfter = balanceAfter.creditBalance / 100,
        translationExtraCreditsBalanceBefore = balanceBefore.extraCreditBalance / 100,
        translationExtraCreditsBalanceAfter = balanceAfter.extraCreditBalance / 100,
      )
    } catch (e: OutOfCreditsException) {
      if (e.reason == OutOfCreditsException.Reason.SPENDING_LIMIT_EXCEEDED) {
        throw BadRequestException(
          Message.CREDIT_SPENDING_LIMIT_EXCEEDED,
        )
      }
      throw BadRequestException(
        Message.OUT_OF_CREDITS,
        listOf(balanceBefore.creditBalance, balanceBefore.extraCreditBalance)
      )
    }
  }

  @PostMapping("/translation-memory")
  @Operation(
    summary = "Suggests machine translations from translation memory." +
      "\n\nThe result is always sorted by similarity, so sorting is not supported."
  )
  @RequiresProjectPermissions([ Scope.TRANSLATIONS_EDIT ])
  @AllowApiAccess
  fun suggestTranslationMemory(
    @RequestBody @Valid dto: SuggestRequestDto,
    @ParameterObject pageable: Pageable
  ): PagedModel<TranslationMemoryItemModel> {
    val targetLanguage = languageService.get(dto.targetLanguageId)

    securityService.checkLanguageTranslatePermission(projectHolder.project.id, listOf(targetLanguage.id))

    val data = dto.baseText?.let { baseText -> translationMemoryService.suggest(baseText, targetLanguage, pageable) }
      ?: let {
        val key = keyService.findOptional(dto.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }
        key.checkInProject()
        translationMemoryService.suggest(key, targetLanguage, pageable)
      }
    return arraytranslationMemoryItemModelAssembler.toModel(data, translationMemoryItemModelAssembler)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }
}
