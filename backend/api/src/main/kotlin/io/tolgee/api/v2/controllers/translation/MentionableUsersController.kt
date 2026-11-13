package io.tolgee.api.v2.controllers.translation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.translations.comments.MentionedUserModel
import io.tolgee.hateoas.translations.comments.MentionedUserModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.CommentMentionService
import io.tolgee.service.translation.MentionableUser
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects/{projectId:[0-9]+}"])
@Tag(name = "Translation Comments", description = "Operations related to translation comments")
class MentionableUsersController(
  private val projectHolder: ProjectHolder,
  private val commentMentionService: CommentMentionService,
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val mentionedUserModelAssembler: MentionedUserModelAssembler,
  private val pagedResourcesAssembler: PagedResourcesAssembler<MentionableUser>,
) {
  @GetMapping("/mentionable-users")
  @Operation(
    summary = "Get users who can be mentioned in a comment",
    description =
      "Returns people who can view the given language, except the current user: earlier commenters on the " +
        "given key in that language first, then project members. " +
        "`page.totalElements` is exact on the last page; on earlier pages it is a lower bound.",
  )
  @RequiresProjectPermissions([Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_COMMENTS_ADD])
  @AllowApiAccess
  fun getMentionableUsers(
    @RequestParam
    @Parameter(description = "Language of the commented translation")
    languageId: Long,
    @RequestParam(required = false)
    @Parameter(description = "Key of the commented translation, its earlier commenters are listed first")
    keyId: Long?,
    @RequestParam(required = false)
    @Parameter(description = "Case-insensitive part of the user name")
    search: String?,
    @ParameterObject
    pageable: Pageable,
  ): PagedModel<MentionedUserModel> {
    val projectId = projectHolder.project.id
    securityService.checkLanguageViewPermission(projectId, listOf(languageId))
    val members =
      commentMentionService.findMentionableUsers(
        projectId = projectId,
        keyId = keyId,
        languageId = languageId,
        exceptUserId = authenticationFacade.authenticatedUser.id,
        search = search,
        pageable = pageable,
      )
    return pagedResourcesAssembler.toModel(members, mentionedUserModelAssembler)
  }
}
