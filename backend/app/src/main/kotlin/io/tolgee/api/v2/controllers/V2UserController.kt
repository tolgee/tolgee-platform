package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModelAssembler
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.UserAccountService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid

@RestController
@RequestMapping("/v2/user")
@Tag(name = "User", description = "Manipulates currentntly authorized user")
class V2UserController(
  private val authenticationFacade: AuthenticationFacade,
  private val userAccountService: UserAccountService,
  private val userAccountModelAssembler: UserAccountModelAssembler,
  private val imageUploadService: ImageUploadService
) {
  @Operation(summary = "Returns current user's data")
  @GetMapping("")
  fun getInfo(): UserAccountModel {
    val userAccount = authenticationFacade.userAccountEntity
    return userAccountModelAssembler.toModel(userAccount)
  }

  @PostMapping("")
  @Operation(summary = "Updates current user's data")
  fun updateUser(@RequestBody @Valid dto: UserUpdateRequestDto?): UserAccountModel {
    val userAccount = userAccountService.update(authenticationFacade.userAccountEntity, dto!!)
    return userAccountModelAssembler.toModel(userAccount)
  }

  @PutMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads user's avatar")
  @ResponseStatus(HttpStatus.OK)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
  ): UserAccountModel {
    imageUploadService.validateIsImage(avatar)
    val entity = authenticationFacade.userAccountEntity
    userAccountService.setAvatar(authenticationFacade.userAccountEntity, avatar.inputStream)
    return userAccountModelAssembler.toModel(entity)
  }

  @DeleteMapping("/avatar")
  @Operation(summary = "Deletes user's avatar")
  @ResponseStatus(HttpStatus.OK)
  fun removeAvatar(): UserAccountModel {
    val entity = authenticationFacade.userAccountEntity
    userAccountService.removeAvatar(authenticationFacade.userAccountEntity)
    return userAccountModelAssembler.toModel(entity)
  }
}
