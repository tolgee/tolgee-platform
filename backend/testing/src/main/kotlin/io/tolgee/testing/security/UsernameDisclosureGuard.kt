package io.tolgee.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

// Checks declaration only; the empty-value contract is pinned by UsernameDisclosureGuardValueTest.
object UsernameDisclosureGuard {
  // Models that intentionally expose a real username (the e-mail); serving one from a less-gated
  // endpoint than it has today would leak the e-mail.
  val allowlistedModelNames: Set<String> =
    setOf(
      "io.tolgee.hateoas.userAccount.UserAccountInProjectModel",
      "io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel",
      "io.tolgee.hateoas.userAccount.PrivateUserAccountModel",
      "io.tolgee.hateoas.userAccount.UserAccountModel",
    )

  // Addresses supplied by the inviter/admin themselves, not resolved from a user account. The
  // user-addressed contributor invitation stores no e-mail, so it serialises these as null.
  val sanctionedAddressProperties: Set<String> =
    setOf(
      "io.tolgee.hateoas.invitation.OrganizationInvitationModel.invitedUserEmail",
      "io.tolgee.hateoas.invitation.ProjectInvitationModel.invitedUserEmail",
      "io.tolgee.hateoas.invitation.PublicInvitationModel.inviteeEmail",
      "io.tolgee.hateoas.translationAgency.TranslationAgencyModel.email",
      "io.tolgee.hateoas.translationAgency.TranslationAgencyModel.emailBcc",
    )

  val strippedModelNames: Set<String> =
    setOf(
      "io.tolgee.hateoas.userAccount.SimpleUserAccountModel",
      "io.tolgee.hateoas.apiKey.ApiKeyModel",
      "io.tolgee.hateoas.apiKey.RevealedApiKeyModel",
      "io.tolgee.hateoas.apiKey.ApiKeyWithLanguagesModel",
      "io.tolgee.hateoas.activity.ProjectActivityAuthorModel",
    )

  fun assertNoLeak(additionalRequiredAnchors: Set<String> = emptySet()) {
    val representationModel = Class.forName("org.springframework.hateoas.RepresentationModel")
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(representationModel))

    val models =
      scanner
        .findCandidateComponents("io.tolgee")
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it).kotlin }

    assertThat(models)
      .`as`("suspiciously few response models scanned — a model-bearing module likely fell off the classpath")
      .hasSizeGreaterThan(50)

    val sanctioned = allowlistedModelNames + strippedModelNames
    assertThat(models.mapNotNull { it.qualifiedName })
      .`as`("classpath scan did not discover the expected models — the guard would be vacuous")
      .containsAll(sanctioned + additionalRequiredAnchors)

    val offenders =
      models
        .filter { it.qualifiedName !in sanctioned }
        .flatMap { model -> userAddressProperties(model).map { "${model.qualifiedName}.$it" } }
        .filter { it !in sanctionedAddressProperties }

    assertThat(offenders)
      .`as`(
        "These response model properties can carry a user's e-mail but are not sanctioned. Strip the " +
          "property and add the class to strippedModelNames with a value assertion; or add the model " +
          "to allowlistedModelNames; or, if only this property echoes an address the caller supplied, " +
          "add it to sanctionedAddressProperties.",
      ).isEmpty()
  }

  fun declaresUsername(model: KClass<*>): Boolean =
    runCatching { model.memberProperties.any { it.name == "username" } }
      .getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }

  fun userAddressProperties(model: KClass<*>): List<String> =
    runCatching {
      model.memberProperties
        .filter { USER_ADDRESS_PROPERTY.matches(it.name) }
        .filter { it.returnType.toString().contains("String") }
        .map { it.name }
    }.getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }

  // The username half is deliberately case-sensitive: `authorUsername` is an address, `invitedUserName`
  // is a display name, and lowercased they are the same string. Adding IGNORE_CASE here would drag
  // every *UserName display field into the guard and force them into the sanction list.
  private val USER_ADDRESS_PROPERTY = Regex("""(?:.*[a-zA-Z])?[Uu]sername|(?i:.*e-?mail.*)""")
}
