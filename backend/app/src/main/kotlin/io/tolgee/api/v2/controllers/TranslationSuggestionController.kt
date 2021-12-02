package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.invitation.TranslationMemoryItemModelAssembler
import io.tolgee.api.v2.hateoas.machineTranslation.SuggestResultModel
import io.tolgee.api.v2.hateoas.translationMemory.TranslationMemoryItemModel
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.TranslationMemoryService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
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
) {
  @PostMapping("/machine-translations")
  @Operation(summary = "Suggests machine translations from enabled services")
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  fun suggestMachineTranslations(@RequestBody @Valid dto: SuggestRequestDto): SuggestResultModel {
    val key = keyService.get(dto.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }
    key.checkInProject()

    val language = languageService.findById(dto.targetLanguageId)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }

    var resultMap: Map<MtServiceType, String?>? = null

    val balanceBefore = mtCreditBucketService.getCreditBalance(projectHolder.projectEntity)

    try {
      resultMap = mtService.getMachineTranslations(projectHolder.projectEntity, key, language)
    } catch (e: OutOfCreditsException) {
      throw BadRequestException(Message.OUT_OF_CREDITS, listOf(balanceBefore))
    }

    val balanceAfter = mtCreditBucketService.getCreditBalance(projectHolder.projectEntity)

    return SuggestResultModel(
      machineTranslations = resultMap,
      translationCreditsBalanceBefore = balanceBefore,
      translationCreditsBalanceAfter = balanceAfter,
    )
  }

  @PostMapping("/translation-memory")
  @Operation(
    summary = "Suggests machine translations from translation memory." +
      "\n\nThe result is always sorted by similarity, so sorting is not supported."
  )
  @AccessWithApiKey([ApiScope.TRANSLATIONS_EDIT])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  fun suggestTranslationMemory(
    @RequestBody @Valid dto: SuggestRequestDto,
    @ParameterObject pageable: Pageable
  ): PagedModel<TranslationMemoryItemModel> {
    val key = keyService.get(dto.keyId).orElseThrow { NotFoundException(Message.KEY_NOT_FOUND) }
    key.checkInProject()
    val language = languageService.findById(dto.targetLanguageId)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }

    val data = translationMemoryService.suggest(key, language, pageable)
    return arraytranslationMemoryItemModelAssembler.toModel(data, translationMemoryItemModelAssembler)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }
}
