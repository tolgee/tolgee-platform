package io.tolgee.hateoas

import io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel
import io.tolgee.hateoas.userAccount.PrivateUserAccountModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModel
import io.tolgee.hateoas.userAccount.UserAccountModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.hateoas.RepresentationModel
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Guards the privacy invariant that a user's `username` (their e-mail) is disclosed only on an
 * allowlist of surfaces (see below); every other response model that references a user must strip
 * it. This is the enforcement for that rule — a new model that carries username without being
 * allowlisted fails here rather than silently shipping the e-mail, which is where the original
 * incident came from.
 *
 * The allowlist lives here, next to the check that enforces it. If you rename or add an
 * allowlisted model, update this set; if a new model must expose username, add it here on purpose.
 */
class UsernameDisclosureGuardTest {
  private val allowlistedModels: Set<KClass<*>> =
    setOf(
      // project members list
      UserAccountInProjectModel::class,
      // org members list
      UserAccountWithOrganizationRoleModel::class,
      // self ("me")
      PrivateUserAccountModel::class,
      // instance-admin user management
      UserAccountModel::class,
    )

  @Test
  fun `no non-allowlisted response model can carry a username`() {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(RepresentationModel::class.java))

    val models =
      listOf("io.tolgee.hateoas", "io.tolgee.ee.api.v2.hateoas")
        .flatMap { scanner.findCandidateComponents(it) }
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it).kotlin }

    // Guard against a vacuous pass: if the scan stops finding the known models the rule is toothless.
    assertThat(models)
      .`as`("classpath scan did not discover the allowlisted user models — the guard would be vacuous")
      .containsAll(allowlistedModels)

    val offenders =
      models.filter { model ->
        if (model in allowlistedModels) return@filter false
        val exposesUsername =
          runCatching { model.memberProperties.any { it.name == "username" } }.getOrDefault(false)
        if (!exposesUsername) return@filter false
        // A non-allowlisted model that references a user must strip username structurally: it must
        // not accept it as a constructor argument (an assembler could otherwise pass the e-mail).
        model.primaryConstructor
          ?.parameters
          .orEmpty()
          .any { it.name == "username" }
      }

    assertThat(offenders.map { it.qualifiedName })
      .`as`(
        "These response models expose a settable `username` but are not allowlisted. A user's " +
          "username is their e-mail: either strip it (make username a fixed empty body `val`, like " +
          "SimpleUserAccountModel) or, if disclosure is intended, add the model to allowlistedModels.",
      ).isEmpty()
  }
}
