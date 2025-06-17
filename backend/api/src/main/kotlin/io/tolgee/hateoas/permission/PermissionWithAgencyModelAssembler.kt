package io.tolgee.hateoas.permission

import io.tolgee.hateoas.TranslationAgencySimpleModelAssembler
import io.tolgee.model.Permission
import io.tolgee.model.enums.Scope
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class PermissionWithAgencyModelAssembler(
  private val translationAgencySimpleModelAssembler: TranslationAgencySimpleModelAssembler,
) : RepresentationModelAssembler<Permission, PermissionWithAgencyModel> {
  override fun toModel(entity: Permission): PermissionWithAgencyModel {
    return PermissionWithAgencyModel(
      scopes = Scope.expand(entity.scopes),
      permittedLanguageIds = entity.translateLanguageIds,
      translateLanguageIds = entity.translateLanguageIds,
      stateChangeLanguageIds = entity.stateChangeLanguageIds,
      viewLanguageIds = entity.viewLanguageIds,
      suggestLanguageIds = entity.suggestLanguageIds,
      type = entity.type,
      agency = entity.agency?.let { translationAgencySimpleModelAssembler.toModel(it) },
    )
  }
}
