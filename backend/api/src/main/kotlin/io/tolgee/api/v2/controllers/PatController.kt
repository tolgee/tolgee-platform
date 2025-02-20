package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.dtos.request.pat.RegeneratePatDto
import io.tolgee.dtos.request.pat.UpdatePatDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.pat.PatModel
import io.tolgee.hateoas.pat.PatModelAssembler
import io.tolgee.hateoas.pat.PatWithUserModel
import io.tolgee.hateoas.pat.PatWithUserModelAssembler
import io.tolgee.hateoas.pat.RevealedPatModel
import io.tolgee.hateoas.pat.RevealedPatModelAssembler
import io.tolgee.model.Pat
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.security.PatService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/pats")
@Tag(name = "Personal Access Tokens")
class PatController(
  private val patService: PatService,
  private val patModelAssembler: PatModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val pagedResourcesAssembler: PagedResourcesAssembler<Pat>,
  private val authenticationFacade: AuthenticationFacade,
  private val revealedPatModelAssembler: RevealedPatModelAssembler,
  private val patWithUserModelAssembler: PatWithUserModelAssembler,
) : IController {
  @GetMapping(value = [""])
  @Operation(summary = "Get PAKs")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun getAll(
    @ParameterObject pageable: Pageable,
  ): PagedModel<PatModel> {
    return pagedResourcesAssembler.toModel(
      patService.findAll(authenticationFacade.authenticatedUser.id, pageable),
      patModelAssembler,
    )
  }

  @GetMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Get one PAK")
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun get(
    @PathVariable id: Long,
  ): PatModel {
    checkOwner(id)
    return patModelAssembler.toModel(patService.get(id))
  }

  @PostMapping(value = [""])
  @Operation(summary = "Create PAK")
  @ResponseStatus(HttpStatus.CREATED)
  @RequiresSuperAuthentication
  fun create(
    @RequestBody @Valid
    dto: CreatePatDto,
  ): RevealedPatModel {
    return revealedPatModelAssembler.toModel(patService.create(dto, authenticationFacade.authenticatedUserEntity))
  }

  @PutMapping(value = ["/{id:[0-9]+}/regenerate"])
  @Operation(
    summary = "Regenerate PAK",
    description =
      "Regenerates Personal Access Token. " +
        "It generates new token value and updates its time of expiration.",
  )
  @RequiresSuperAuthentication
  fun regenerate(
    @RequestBody @Valid
    dto: RegeneratePatDto,
    @PathVariable id: Long,
  ): RevealedPatModel {
    checkOwner(id)
    return revealedPatModelAssembler.toModel(patService.regenerate(id, dto.expiresAt))
  }

  @PutMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Update PAK", description = "Updates Personal Access Token")
  @RequiresSuperAuthentication
  fun update(
    @RequestBody @Valid
    dto: UpdatePatDto,
    @PathVariable id: Long,
  ): PatModel {
    checkOwner(id)
    return patModelAssembler.toModel(patService.update(id, dto))
  }

  @DeleteMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Delete PAK", description = "Deletes Personal Access Token")
  @RequiresSuperAuthentication
  @BypassEmailVerification
  @BypassForcedSsoAuthentication
  fun delete(
    @PathVariable id: Long,
  ) {
    val pat = checkOwner(id)
    return patService.delete(pat)
  }

  @GetMapping(path = ["/current"])
  @Operation(
    summary = "Return current PAK",
    description =
      "Returns current Personal Access Token. If the request is not authenticated with a Personal Access " +
        "Token, it will return 400 response status.",
  )
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  fun getCurrent(): PatWithUserModel {
    if (!authenticationFacade.isPersonalAccessTokenAuth) {
      throw BadRequestException(Message.INVALID_AUTHENTICATION_METHOD)
    }

    return patWithUserModelAssembler.toModel(authenticationFacade.personalAccessTokenEntity)
  }

  private fun checkOwner(id: Long): Pat {
    val pat = patService.get(id)
    if (pat.userAccount.id != authenticationFacade.authenticatedUser.id) {
      throw PermissionException()
    }
    return pat
  }
}
