package io.tolgee.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Only `RepresentationModel` subtypes are scanned; a plain data class used as a response body is not
 * covered — keep user-referencing responses on `RepresentationModel`.
 */
object UsernameDisclosureGuard {
  val allowlistedModelNames: Set<String> =
    setOf(
      "io.tolgee.hateoas.userAccount.UserAccountInProjectModel", // project members — MEMBERS_VIEW + super-auth
      "io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel", // org members — org-role
      "io.tolgee.hateoas.userAccount.PrivateUserAccountModel", // self — GET /v2/user
      "io.tolgee.hateoas.userAccount.UserAccountModel", // instance-admin — super-auth
    )

  val corePolicedModelNames: Set<String> =
    setOf(
      "io.tolgee.hateoas.userAccount.SimpleUserAccountModel",
      "io.tolgee.hateoas.apiKey.ApiKeyModel",
      "io.tolgee.hateoas.activity.ProjectActivityAuthorModel",
    )

  fun hasSettableUsername(model: KClass<*>): Boolean {
    val usernameProperty =
      runCatching { model.memberProperties.firstOrNull { it.name == "username" } }
        .getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }
        ?: return false
    if (model.constructors.any { constructor -> constructor.parameters.any { it.name == "username" } }) return true
    return usernameProperty is KMutableProperty<*>
  }

  fun assertNoLeak(requiredAnchorClassNames: Set<String>) {
    val representationModel = Class.forName("org.springframework.hateoas.RepresentationModel")
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(representationModel))

    val models =
      scanner
        .findCandidateComponents("io.tolgee")
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it).kotlin }

    assertThat(models.mapNotNull { it.qualifiedName })
      .`as`("classpath scan did not discover the expected models — the guard would be vacuous")
      .containsAll(requiredAnchorClassNames)

    val offenders =
      models.filter { it.qualifiedName !in allowlistedModelNames && hasSettableUsername(it) }

    assertThat(offenders.map { it.qualifiedName })
      .`as`(
        "These response models expose a settable `username` but are not allowlisted. A user's " +
          "username is their e-mail: either strip it (make username an immutable empty body `val`, " +
          "like SimpleUserAccountModel) or, if disclosure is intended, add it to allowlistedModelNames.",
      ).isEmpty()
  }
}
