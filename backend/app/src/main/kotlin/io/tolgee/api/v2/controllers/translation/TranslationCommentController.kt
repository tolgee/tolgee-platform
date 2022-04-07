/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.activity.ActivityType
import io.tolgee.activity.RequestActivity
import io.tolgee.api.v2.hateoas.translations.TranslationModelAssembler
import io.tolgee.api.v2.hateoas.translations.comments.TranslationCommentModel
import io.tolgee.api.v2.hateoas.translations.comments.TranslationCommentModelAssembler
import io.tolgee.api.v2.hateoas.translations.comments.TranslationWithCommentModel
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.SecurityService
import io.tolgee.service.TranslationCommentService
import io.tolgee.service.TranslationService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translations/",
    "/v2/projects/translations/"
  ]
)
@Tags(
  value = [
    Tag(name = "Translation Comments", description = "Operations related to translation comments"),
  ]
)
class TranslationCommentController(
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
  private val translationCommentService: TranslationCommentService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<TranslationComment>,
  private val translationCommentModelAssembler: TranslationCommentModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val translationModelAssembler: TranslationModelAssembler
) {

  @GetMapping(value = ["{translationId}/comments"])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Operation(summary = "Returns translation comments of translation")
  fun getAll(
    @PathVariable translationId: Long,
    @ParameterObject pageable: Pageable
  ): PagedModel<TranslationCommentModel> {
    val translation = translationService.find(translationId) ?: throw NotFoundException()
    translation.checkFromProject()
    return pagedResourcesAssembler.toModel(
      translationCommentService.getPaged(translation, pageable),
      translationCommentModelAssembler
    )
  }

  @GetMapping(value = ["{translationId}/comments/{commentId}"])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Operation(summary = "Returns single translation comment")
  fun get(@PathVariable translationId: Long, @PathVariable commentId: Long): TranslationCommentModel {
    val comment = translationCommentService.get(commentId)
    comment.checkFromProject()
    return translationCommentModelAssembler.toModel(comment)
  }

  @PutMapping(value = ["{translationId}/comments/{commentId}"])
  @AccessWithAnyProjectPermission
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @Operation(summary = "Updates single translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_EDIT)
  fun update(@PathVariable commentId: Long, @RequestBody @Valid dto: TranslationCommentDto): TranslationCommentModel {
    val comment = translationCommentService.get(commentId)
    if (comment.author.id != authenticationFacade.userAccount.id) {
      throw BadRequestException(io.tolgee.constants.Message.CAN_EDIT_ONLY_OWN_COMMENT)
    }
    translationCommentService.update(dto, comment)
    return translationCommentModelAssembler.toModel(comment)
  }

  @PutMapping(value = ["{translationId}/comments/{commentId}/set-state/{state}"])
  @Operation(summary = "Sets state of translation comment")
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_SET_STATE)
  fun setState(
    @PathVariable commentId: Long,
    @PathVariable state: TranslationCommentState
  ): TranslationCommentModel {
    val comment = translationCommentService.get(commentId)
    comment.checkFromProject()
    translationCommentService.setState(comment, state)
    return translationCommentModelAssembler.toModel(comment)
  }

  @DeleteMapping(value = ["{translationId}/comments/{commentId}"])
  @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @Operation(summary = "Deletes the translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_DELETE)
  fun delete(@PathVariable commentId: Long) {
    val comment = translationCommentService.get(commentId)
    comment.checkFromProject()
    if (comment.author.id != authenticationFacade.userAccount.id) {
      try {
        securityService.checkProjectPermission(
          projectHolder.project.id,
          Permission.ProjectPermissionType.MANAGE
        )
      } catch (e: PermissionException) {
        throw BadRequestException(io.tolgee.constants.Message.CAN_EDIT_ONLY_OWN_COMMENT)
      }
    }
    translationCommentService.delete(comment)
  }

  @PostMapping(value = ["/create-comment"])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Creates a translation comment. Empty translation is stored, when not exists.")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_ADD)
  fun create(
    @RequestBody @Valid dto: TranslationCommentWithLangKeyDto
  ): ResponseEntity<TranslationWithCommentModel> {
    val translation = translationService.getOrCreate(dto.keyId, dto.languageId)
    if (translation.key.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.KEY_NOT_FROM_PROJECT)
    }

    if (translation.language.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.LANGUAGE_NOT_FROM_PROJECT)
    }

    // Translation was just created
    if (translation.id == 0L) {
      translation.state = TranslationState.UNTRANSLATED
    }

    translationService.save(translation)

    val comment = translationCommentService.create(dto, translation, authenticationFacade.userAccountEntity)
    return ResponseEntity(
      TranslationWithCommentModel(
        comment = translationCommentModelAssembler.toModel(comment),
        translation = translationModelAssembler.toModel(translation)
      ),
      HttpStatus.CREATED
    )
  }

  @PostMapping(value = ["{translationId}/comments"])
  @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
  @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Creates a translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_ADD)
  fun create(
    @PathVariable translationId: Long,
    @RequestBody @Valid dto: TranslationCommentDto
  ): ResponseEntity<TranslationCommentModel> {
    val translation = translationService.find(translationId) ?: throw NotFoundException()
    translation.checkFromProject()
    val comment = translationCommentService.create(dto, translation, authenticationFacade.userAccountEntity)
    return ResponseEntity(translationCommentModelAssembler.toModel(comment), HttpStatus.CREATED)
  }

  private fun TranslationComment.checkFromProject() {
    if (this.translation.key.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.TRANSLATION_NOT_FROM_PROJECT)
    }
  }

  private fun Translation.checkFromProject() {
    if (this.key.project.id != projectHolder.project.id) {
      throw BadRequestException(io.tolgee.constants.Message.TRANSLATION_NOT_FROM_PROJECT)
    }
  }
}
