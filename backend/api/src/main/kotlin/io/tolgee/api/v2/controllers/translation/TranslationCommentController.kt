/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.translation.comment.TranslationCommentDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.hateoas.translations.comments.TranslationCommentModel
import io.tolgee.hateoas.translations.comments.TranslationCommentModelAssembler
import io.tolgee.hateoas.translations.comments.TranslationWithCommentModel
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.TranslationComment
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationCommentService
import io.tolgee.service.translation.TranslationService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
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

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/projects/{projectId:[0-9]+}/translations/",
    "/v2/projects/translations/",
  ],
)
@Tags(
  value = [
    Tag(name = "Translation Comments", description = "Operations related to translation comments"),
  ],
)
class TranslationCommentController(
  private val projectHolder: ProjectHolder,
  private val translationService: TranslationService,
  private val translationCommentService: TranslationCommentService,
  private val pagedResourcesAssembler: PagedResourcesAssembler<TranslationComment>,
  private val translationCommentModelAssembler: TranslationCommentModelAssembler,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val translationModelAssembler: TranslationModelAssembler,
) {
  @GetMapping(value = ["{translationId}/comments"])
  @Operation(
    summary = "Get translation comments",
    description = "Returns translation comments of translation",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAll(
    @PathVariable translationId: Long,
    @ParameterObject pageable: Pageable,
  ): PagedModel<TranslationCommentModel> {
    val translation = translationService.get(projectHolder.project.id, translationId)
    return pagedResourcesAssembler.toModel(
      translationCommentService.getPaged(translation, pageable),
      translationCommentModelAssembler,
    )
  }

  @GetMapping(value = ["{translationId}/comments/{commentId}"])
  @Operation(
    summary = "Get one translation comment",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  fun get(
    @PathVariable translationId: Long,
    @PathVariable commentId: Long,
  ): TranslationCommentModel {
    val comment = translationCommentService.getWithAuthorFetched(projectHolder.project.id, translationId, commentId)
    return translationCommentModelAssembler.toModel(comment)
  }

  @PutMapping(value = ["{translationId}/comments/{commentId}"])
  @Operation(summary = "Update translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_EDIT)
  @UseDefaultPermissions // Security: Permission check done inside; users should be able to edit their comments
  @AllowApiAccess
  fun update(
    @PathVariable
    commentId: Long,
    @PathVariable
    translationId: Long,
    @RequestBody @Valid
    dto: TranslationCommentDto,
  ): TranslationCommentModel {
    val comment = translationCommentService.getWithAuthorFetched(projectHolder.project.id, translationId, commentId)
    if (comment.author.id != authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(io.tolgee.constants.Message.CAN_EDIT_ONLY_OWN_COMMENT)
    }
    translationCommentService.update(dto, comment)
    return translationCommentModelAssembler.toModel(comment)
  }

  @PutMapping(value = ["{translationId}/comments/{commentId}/set-state/{state}"])
  @Operation(summary = "Set state of translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_SET_STATE)
  @UseDefaultPermissions
  @AllowApiAccess
  fun setState(
    @PathVariable translationId: Long,
    @PathVariable commentId: Long,
    @PathVariable state: TranslationCommentState,
  ): TranslationCommentModel {
    val translation = translationService.get(translationId)
    securityService.checkScopeOrAssignedToTask(
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
      translation.language.id,
      translation.key.id,
    )
    val comment = translationCommentService.getWithAuthorFetched(projectHolder.project.id, translationId, commentId)
    translationCommentService.setState(comment, state)
    return translationCommentModelAssembler.toModel(comment)
  }

  @DeleteMapping(value = ["{translationId}/comments/{commentId}"])
  // the permissions are checked in the body! We need to enable authors to delete their comments
  @Operation(summary = "Delete translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_DELETE)
  @UseDefaultPermissions // Security: Permission check done inside; users should be able to delete their comments
  @AllowApiAccess
  fun delete(
    @PathVariable translationId: Long,
    @PathVariable commentId: Long,
  ) {
    val comment = translationCommentService.get(projectHolder.project.id, translationId, commentId)
    if (comment.author.id != authenticationFacade.authenticatedUser.id) {
      try {
        checkEditPermission()
      } catch (e: PermissionException) {
        throw BadRequestException(io.tolgee.constants.Message.CAN_EDIT_ONLY_OWN_COMMENT)
      }
    }
    translationCommentService.delete(comment)
  }

  private fun checkEditPermission() {
    securityService.checkProjectPermission(
      projectHolder.project.id,
      Scope.TRANSLATIONS_COMMENTS_EDIT,
    )
  }

  @PostMapping(value = ["/create-comment"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create translation comment",
    description = "Creates a translation comment. Empty translation is stored, when not exists.",
  )
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_ADD)
  @UseDefaultPermissions
  @AllowApiAccess
  fun create(
    @RequestBody @Valid
    dto: TranslationCommentWithLangKeyDto,
  ): ResponseEntity<TranslationWithCommentModel> {
    securityService.checkScopeOrAssignedToTask(
      Scope.TRANSLATIONS_COMMENTS_ADD,
      dto.languageId,
      dto.keyId,
    )
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

    val comment = translationCommentService.create(dto, translation, authenticationFacade.authenticatedUserEntity)
    return ResponseEntity(
      TranslationWithCommentModel(
        comment = translationCommentModelAssembler.toModel(comment),
        translation = translationModelAssembler.toModel(translation),
      ),
      HttpStatus.CREATED,
    )
  }

  @PostMapping(value = ["{translationId}/comments"])
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create translation comment")
  @RequestActivity(ActivityType.TRANSLATION_COMMENT_ADD)
  @RequiresProjectPermissions([Scope.TRANSLATIONS_COMMENTS_ADD])
  @AllowApiAccess
  fun create(
    @PathVariable translationId: Long,
    @RequestBody @Valid
    dto: TranslationCommentDto,
  ): ResponseEntity<TranslationCommentModel> {
    val translation =
      translationService.get(projectHolder.project.id, translationId)
    val comment = translationCommentService.create(dto, translation, authenticationFacade.authenticatedUserEntity)
    return ResponseEntity(translationCommentModelAssembler.toModel(comment), HttpStatus.CREATED)
  }
}
