package io.tolgee.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

// Checks declaration only; the empty-value contract is pinned by UsernameDisclosureGuardValueTest.
object UsernameDisclosureGuard {
  // Models that intentionally expose a real username (the e-mail). Serve each only from the gated
  // endpoint noted — returning one from a less-gated endpoint would leak the e-mail.
  val allowlistedModelNames: Set<String> =
    setOf(
      // GET /v2/projects/{id}/users — MEMBERS_VIEW + super-auth
      "io.tolgee.hateoas.userAccount.UserAccountInProjectModel",
      // GET /v2/organizations/{id}/users — org-member role
      "io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel",
      // GET /v2/user — self only
      "io.tolgee.hateoas.userAccount.PrivateUserAccountModel",
      // GET /v2/administration/users — super-auth
      "io.tolgee.hateoas.userAccount.UserAccountModel",
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

    val offenders = models.filter { it.qualifiedName !in sanctioned && declaresUsername(it) }
    assertThat(offenders.map { it.qualifiedName })
      .`as`(
        "These response models declare a `username` (a user's e-mail) but are not sanctioned. Strip " +
          "it (immutable empty body `val`, like SimpleUserAccountModel) and add the class to " +
          "strippedModelNames with a value assertion; or, if the e-mail is intentionally disclosed " +
          "there, add it to allowlistedModelNames.",
      ).isEmpty()
  }

  fun declaresUsername(model: KClass<*>): Boolean =
    runCatching { model.memberProperties.any { it.name == "username" } }
      .getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }
}
