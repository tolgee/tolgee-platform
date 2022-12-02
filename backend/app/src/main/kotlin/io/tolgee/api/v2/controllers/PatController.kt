package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.pat.PatModel
import io.tolgee.api.v2.hateoas.pat.PatModelAssembler
import io.tolgee.api.v2.hateoas.pat.PatWithUserModel
import io.tolgee.api.v2.hateoas.pat.PatWithUserModelAssembler
import io.tolgee.api.v2.hateoas.pat.RevealedPatModel
import io.tolgee.api.v2.hateoas.pat.RevealedPatModelAssembler
import io.tolgee.constants.Message
import io.tolgee.controllers.IController
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.dtos.request.pat.RegeneratePatDto
import io.tolgee.dtos.request.pat.UpdatePatDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Pat
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.NeedsSuperJwtToken
import io.tolgee.security.patAuth.DenyPatAccess
import io.tolgee.service.security.PatService
import org.springdoc.api.annotations.ParameterObject
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
import javax.validation.Valid

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
  @Operation(summary = "Get all Personal Access Tokens")
  fun getAll(
    @ParameterObject pageable: Pageable
  ): PagedModel<PatModel> {
    return pagedResourcesAssembler.toModel(
      patService.findAll(authenticationFacade.userAccount.id, pageable),
      patModelAssembler
    )
  }

  @GetMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Get Personal Access Token")
  fun get(
    @PathVariable id: Long
  ): PatModel {
    checkOwner(id)
    return patModelAssembler.toModel(patService.get(id))
  }

  @PostMapping(value = [""])
  @Operation(summary = "Creates new Personal Access Token")
  @ResponseStatus(HttpStatus.CREATED)
  @DenyPatAccess
  @NeedsSuperJwtToken
  fun create(@RequestBody @Valid dto: CreatePatDto): RevealedPatModel {
    return revealedPatModelAssembler.toModel(patService.create(dto, authenticationFacade.userAccountEntity))
  }

  @PutMapping(value = ["/{id:[0-9]+}/regenerate"])
  @Operation(
    summary = "Regenerates Personal Access Token. " +
      "It generates new token value and updates its time of expiration."
  )
  @DenyPatAccess
  @NeedsSuperJwtToken
  fun regenerate(
    @RequestBody @Valid dto: RegeneratePatDto,
    @PathVariable id: Long
  ): RevealedPatModel {
    checkOwner(id)
    return revealedPatModelAssembler.toModel(patService.regenerate(id, dto.expiresAt))
  }

  @PutMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Updates Personal Access Token")
  @DenyPatAccess
  @NeedsSuperJwtToken
  fun update(
    @RequestBody @Valid dto: UpdatePatDto,
    @PathVariable id: Long
  ): PatModel {
    checkOwner(id)
    return patModelAssembler.toModel(patService.update(id, dto))
  }

  @DeleteMapping(value = ["/{id:[0-9]+}"])
  @Operation(summary = "Deletes Personal Access Token")
  @DenyPatAccess
  @NeedsSuperJwtToken
  fun delete(
    @PathVariable id: Long
  ) {
    checkOwner(id)
    return patService.delete(id)
  }

  @GetMapping(path = ["/current"])
  @Operation(summary = "Returns current Personal Access Token info")
  fun getCurrent(): PatWithUserModel {
    if (!authenticationFacade.isPatAuthentication) {
      throw BadRequestException(Message.INVALID_AUTHENTICATION_METHOD)
    }

    val pat = authenticationFacade.pat
    return patWithUserModelAssembler.toModel(pat)
  }

  private fun checkOwner(id: Long) {
    if (patService.get(id).userAccount.id != authenticationFacade.userAccount.id) {
      throw PermissionException()
    }
  }
}
