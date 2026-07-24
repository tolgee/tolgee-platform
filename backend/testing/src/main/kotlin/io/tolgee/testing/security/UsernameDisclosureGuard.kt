package io.tolgee.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

/**
 * Enforces the privacy invariant that a user's `username` (their e-mail) is disclosed only on an
 * allowlist of surfaces; every other response model that references a user must strip it. A new
 * model that carries username without being allowlisted fails here rather than silently shipping
 * the e-mail, which is where the original incident came from.
 *
 * This is the single source of truth (allowlist + scan). It is invoked from two thin entrypoints so
 * the net is never optional: a CORE entrypoint in `:server-app` (built and tested in every build,
 * including EE-less ones, so core response models stay guarded even if the EE module is dropped),
 * and an EE entrypoint in `:ee-test` that additionally anchors an EE model so the EE side is proven
 * non-empty too.
 *
 * A model is flagged when its `username` is settable — via any constructor parameter, or via a
 * mutable (`var`) property an assembler could assign post-construction. The safe pattern is an
 * immutable `val username = ""` body property (see SimpleUserAccountModel). Two blind spots:
 * (1) a `val` derived from a differently-named constructor field (`val username = email`) — not
 * reachable by any assembler through the `username` name and not a pattern this codebase uses; and
 * (2) only `RepresentationModel` response types are scanned — a plain data class used as a response
 * body would not be seen, so keep user-referencing responses on `RepresentationModel`.
 *
 * What this does NOT prove: that each allowlisted model is served only from a correctly gated
 * endpoint. That binding is documented on each allowlisted model class — honour it when wiring one
 * into a new endpoint.
 *
 * Model class names are strings so this module (`:testing`) needs no compile dependency on `:api`
 * or spring-hateoas; they resolve on the caller's runtime classpath.
 */
object UsernameDisclosureGuard {
  /** The response models that intentionally disclose username (the e-mail). Single source of truth. */
  val allowlistedModelNames: Set<String> =
    setOf(
      // project members list — GET /v2/projects/{id}/users, MEMBERS_VIEW + super-auth
      "io.tolgee.hateoas.userAccount.UserAccountInProjectModel",
      // org members list — GET /v2/organizations/{id}/users, org-role gated
      "io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel",
      // self ("me") — GET /v2/user, the caller's own e-mail
      "io.tolgee.hateoas.userAccount.PrivateUserAccountModel",
      // instance-admin user management — AdministrationController, super-auth
      "io.tolgee.hateoas.userAccount.UserAccountModel",
    )

  fun assertNoLeak(requiredAnchorClassNames: Set<String>) {
    val representationModel = Class.forName("org.springframework.hateoas.RepresentationModel")
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AssignableTypeFilter(representationModel))

    // A single org-wide root recurses into every present hateoas package, so there is no prefix
    // list to keep in sync. In an EE-less classpath it naturally sees only core models.
    val models =
      scanner
        .findCandidateComponents("io.tolgee")
        .mapNotNull { it.beanClassName }
        .distinct()
        .map { Class.forName(it).kotlin }

    // Guard against a vacuous pass: the scan must have discovered the caller's anchor models.
    assertThat(models.mapNotNull { it.qualifiedName })
      .`as`("classpath scan did not discover the expected models — the guard would be vacuous")
      .containsAll(requiredAnchorClassNames)

    val offenders =
      models.filter { model ->
        if (model.qualifiedName in allowlistedModelNames) return@filter false
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
          "like SimpleUserAccountModel) or, if disclosure is intended, add it to allowlistedModelNames.",
      ).isEmpty()
  }
}
