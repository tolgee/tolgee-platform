/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import io.tolgee.api.v2.hateoas.translations.comments.TranslationCommentModel
import io.tolgee.api.v2.hateoas.translations.comments.TranslationCommentModelAssembler
import io.tolgee.constants.ApiScope
import io.tolgee.constants.Message
import io.tolgee.dtos.request.TranslationCommentDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.enums.TranslationCommentState
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
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@Tag(name = "Import")
@RequestMapping(value = [
    "/v2/projects/{projectId:[0-9]+}/translations/{translationId}/comments",
    "/v2/projects/translations/{translationId}/comments"
])
@Tags(value = [
    Tag(name = "Translations", description = "Operations related to translations in project"),
])
class TranslationCommentController(
        private val projectHolder: ProjectHolder,
        private val translationService: TranslationService,
        private val translationCommentService: TranslationCommentService,
        private val pagedResourcesAssembler: PagedResourcesAssembler<TranslationComment>,
        private val translationCommentModelAssembler: TranslationCommentModelAssembler,
        private val authenticationFacade: AuthenticationFacade,
        private val securityService: SecurityService
) {

    @GetMapping(value = [""])
    @AccessWithAnyProjectPermission
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
    fun getAll(translationId: Long, pageable: Pageable): PagedModel<TranslationCommentModel> {
        val translation = translationService.find(translationId) ?: throw NotFoundException()
        translation.checkFromProject()
        return pagedResourcesAssembler.toModel(
                translationCommentService.getPaged(translation, pageable),
                translationCommentModelAssembler
        )
    }

    @GetMapping(value = ["/{commentId}"])
    @AccessWithAnyProjectPermission
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_VIEW])
    fun get(translationId: Long, id: Long): TranslationCommentModel {
        val comment = translationCommentService.get(id)
        comment.checkFromProject()
        return translationCommentModelAssembler.toModel(comment)
    }


    @PutMapping(value = ["/{commentId}"])
    @AccessWithAnyProjectPermission
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    fun update(id: Long, dto: TranslationCommentDto): TranslationCommentModel {
        val comment = translationCommentService.get(id)
        if (comment.author.id == authenticationFacade.userAccount.id) {
            throw BadRequestException(Message.CAN_EDIT_ONLY_OWN_COMMENT)
        }
        translationCommentService.update(dto, comment)
        return translationCommentModelAssembler.toModel(comment)
    }

    @PutMapping(value = ["/{commentId}/set-state/{state}"])
    @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    fun setState(id: Long, commentId: Long, state: TranslationCommentState): TranslationCommentModel {
        val comment = translationCommentService.get(id)
        comment.checkFromProject()
        translationCommentService.setState(comment, state)
        return translationCommentModelAssembler.toModel(comment)
    }

    @DeleteMapping(value = ["/{commentId}"])
    @AccessWithProjectPermission(permission = Permission.ProjectPermissionType.TRANSLATE)
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    fun delete(id: Long) {
        val comment = translationCommentService.get(id)
        comment.checkFromProject()
        if (comment.author.id != authenticationFacade.userAccount.id) {
            try {
                securityService.checkProjectPermission(
                        projectHolder.project.id,
                        Permission.ProjectPermissionType.MANAGE
                )
            } catch (e: PermissionException) {
                throw BadRequestException(Message.CAN_EDIT_ONLY_OWN_COMMENT)
            }
        }
        translationCommentService.delete(comment)
    }

    @PostMapping(value = [""])
    @AccessWithProjectPermission(Permission.ProjectPermissionType.TRANSLATE)
    @AccessWithApiKey(scopes = [ApiScope.TRANSLATIONS_EDIT])
    fun create(translationId: Long, dto: TranslationCommentDto) {
        val translation = translationService.find(translationId) ?: throw NotFoundException()
        translation.checkFromProject()
        translationCommentService.create(dto, translation)
    }

    private fun TranslationComment.checkFromProject() {
        if (this.translation.key?.project?.id != projectHolder.project.id) {
            throw BadRequestException(Message.TRANSLATION_NOT_FROM_PROJECT)
        }
    }

    private fun Translation.checkFromProject() {
        if (this.key?.project?.id != projectHolder.project.id) {
            throw BadRequestException(Message.TRANSLATION_NOT_FROM_PROJECT)
        }
    }
}
