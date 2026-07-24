package io.tolgee.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Enforces that only sanctioned response models declare a `username` property (a user's e-mail).
 * Any other `RepresentationModel` that declares `username` fails the scan.
 *
 * Scope limits a maintainer must know:
 * - only `RepresentationModel` subtypes are scanned — keep user-referencing responses on it;
 * - it keys on a property literally named `username`, so an e-mail re-exposed under another field
 *   name is out of scope here;
 * - it checks that a model *declares* username, not the value it emits — the empty-value contract
 *   for the stripped models is pinned separately by value assertions in the guard's tests.
 */
object UsernameDisclosureGuard {
  /** Models that intentionally expose a real username (the e-mail). */
  val allowlistedModelNames: Set<String> =
    setOf(
      "io.tolgee.hateoas.userAccount.UserAccountInProjectModel",
      "io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel",
      "io.tolgee.hateoas.userAccount.PrivateUserAccountModel",
      "io.tolgee.hateoas.userAccount.UserAccountModel",
    )

  /** Models allowed to declare a `username` property but which must keep it empty (see value tests). */
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

    // Trips loudly if a whole response-model module drops off the classpath (today ~180 core, more with EE).
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

  private fun declaresUsername(model: KClass<*>): Boolean =
    runCatching { model.memberProperties.any { it.name == "username" } }
      .getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }
}
