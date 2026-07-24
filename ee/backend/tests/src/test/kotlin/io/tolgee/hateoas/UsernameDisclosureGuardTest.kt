package io.tolgee.hateoas

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
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
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Guards the privacy invariant that a user's `username` (their e-mail) is disclosed only on an
 * allowlist of surfaces (see below); every other response model that references a user must strip
 * it. This is the enforcement for that rule — a new model that carries username without being
 * allowlisted fails here rather than silently shipping the e-mail, which is where the original
 * incident came from.
 *
 * The allowlist lives here, next to the check that enforces it. If you rename or add an
 * allowlisted model, update this set; if a new model must expose username, add it here on purpose.
 *
 * A model is flagged when its `username` is settable — via any constructor parameter, or via a
 * mutable (`var`) property an assembler could assign post-construction. The safe pattern is an
 * immutable `val username = ""` body property (see SimpleUserAccountModel). One residual blind spot
 * the guard cannot see reflectively: a `val` derived from a differently-named constructor field
 * (`val username = email`); that is not reachable by any assembler through the `username` name and
 * is not the pattern this codebase uses.
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

    // A single org-wide root recurses into every present and future hateoas package, so there is no
    // prefix list to keep in sync (a missed package would silently under-scan).
    val models =
      scanner
        .findCandidateComponents("io.tolgee")
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it).kotlin }

    // Guard against a vacuous pass: anchor on both the (api) allowlisted models and a known EE model,
    // so an empty scan of either side fails loudly instead of passing toothlessly.
    assertThat(models)
      .`as`("classpath scan did not discover the known response models — the guard would be vacuous")
      .containsAll(allowlistedModels + BranchModel::class)

    val offenders =
      models.filter { model ->
        if (model in allowlistedModels) return@filter false
        // Fail loud if a model cannot be inspected, rather than silently treating it as clean.
        val usernameProperty =
          runCatching { model.memberProperties.firstOrNull { it.name == "username" } }
            .getOrElse { throw AssertionError("Could not reflect over ${model.qualifiedName}", it) }
            ?: return@filter false
        val settableViaConstructor =
          model.constructors.any { constructor -> constructor.parameters.any { it.name == "username" } }
        val settableViaSetter = usernameProperty is KMutableProperty<*>
        settableViaConstructor || settableViaSetter
      }

    assertThat(offenders.map { it.qualifiedName })
      .`as`(
        "These response models expose a settable `username` but are not allowlisted. A user's " +
          "username is their e-mail: either strip it (make username an immutable empty body `val`, " +
          "like SimpleUserAccountModel) or, if disclosure is intended, add the model to allowlistedModels.",
      ).isEmpty()
  }
}
