package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryType
import java.util.Date

/**
 * Test data for Translation Memory tests.
 *
 * Provides two projects. Both have an auto-created project-type TM (every project gets one in
 * the current model), so neither is "TM-less" any more — the difference is what's preloaded:
 *   - [projectWithTm]: project TM is pre-populated with one key and its TM entry, plus a number
 *     of shared TM assignments for the controller tests. Use for the write pipeline, update,
 *     delete cascade, project deletion cleanup, and any test that needs realistic shared-TM
 *     fixtures.
 *   - [projectWithoutTm]: the base project (inherited from [BaseTestData]) — has only the
 *     auto-created project TM, no fixture entries, no shared TM assignments. Use it whenever a
 *     test needs a "clean slate" project against which to assert default behaviour.
 */
class TranslationMemoryTestData : BaseTestData() {
  lateinit var projectWithTm: Project
  lateinit var projectTm: TranslationMemory
  lateinit var sharedTm: TranslationMemory

  /**
   * Shared TM with a non-zero `defaultPenalty` and a single entry `"Farewell" -> "Auf Wiedersehen" (de)`.
   * Preloaded so penalty tests can assert `similarity == rawSimilarity - defaultPenalty/100` without
   * mutating TM state in the test body.
   */
  lateinit var sharedTmWithPenalty: TranslationMemory

  /**
   * Shared TM whose assignment to [projectWithTm] carries a per-row penalty override that beats
   * the TM's [TranslationMemory.defaultPenalty]. Single entry `"Good luck" -> "Viel Glück" (de)`.
   */
  lateinit var sharedTmWithOverride: TranslationMemory

  /**
   * Shared TM with `writeOnlyReviewed = true`. Its only writable target is [projectWithTm].
   * Used by reviewed-only pipeline tests — saves of non-REVIEWED translations must skip (or
   * delete from) this TM while sibling permissive TMs take the write.
   */
  lateinit var sharedTmReviewedOnly: TranslationMemory

  lateinit var unassignedSharedTm: TranslationMemory

  /**
   * Project assigned only to its own PROJECT-type TM — no shared TM assignments. Use for
   * tests that need to change the project's base language without tripping the
   * shared-TM-base-mismatch validation.
   */
  lateinit var projectWithOnlyProjectTm: Project
  lateinit var onlyProjectTm: TranslationMemory

  /**
   * Second write-access project for [multiProjectSharedTm]. Has a single key whose English
   * source matches `existingKey` from [projectWithTm] but a different German translation,
   * so the same source text on this TM yields two distinct virtual rows from two projects.
   * Drives the multi-candidate-row UI test.
   */
  lateinit var conflictProject: Project

  /**
   * Shared TM with two write-access-assigned projects ([projectWithTm] and [conflictProject])
   * that both translate the same English source text differently. Used to exercise the
   * multi-candidate row expansion in the TM content browser.
   */
  lateinit var multiProjectSharedTm: TranslationMemory

  /**
   * Shared TM declared with a `fr` source — intentionally different from [projectWithTm]'s
   * English base. Drives the base-language-match validation: assigning this TM must be
   * rejected.
   */
  lateinit var mismatchedBaseSharedTm: TranslationMemory
  lateinit var germanLanguageNoTm: Language
  lateinit var germanLanguageWithTm: Language

  /**
   * Czech on [projectWithTm]. Paired with [helloKeyCzechTranslation] (empty) and the
   * `shared-greeting-cs` key on [conflictProject] (non-empty cs translation) to drive the
   * cross-project virtual-row regression test for auto-translate: [projectWithTm] can read
   * [multiProjectSharedTm] and pulls the cs target from [conflictProject]'s translation,
   * not from any stored entry.
   */
  lateinit var czechLanguageWithTm: Language

  /** Empty Czech row on [helloKey] — the seat the cross-project auto-translate fills. */
  lateinit var helloKeyCzechTranslation: Translation
  lateinit var existingKey: Key
  lateinit var existingTargetTranslation: Translation

  /**
   * Second key on [projectWithTm] whose German translation is already in `REVIEWED` state.
   * Combined with [existingKey] (state = TRANSLATED) this gives the backfill tests a mixed
   * state corpus: the reviewed-only TM should only pick up this key's translation.
   */
  lateinit var reviewedKey: Key
  lateinit var reviewedTargetTranslation: Translation

  /**
   * Key whose German translation starts as `TRANSLATED`. Drives the "promote → reviewed-only TM
   * picks it up" test: bumping the state to `REVIEWED` must surface the entry on the
   * reviewed-only TM without affecting the always-on TMs.
   */
  lateinit var promotedKey: Key
  lateinit var promotedTargetTranslation: Translation

  /**
   * Key whose German translation starts as `REVIEWED`. Drives the "demote → reviewed-only TM
   * drops it" test: bumping the state down to `TRANSLATED` must remove the entry from the
   * reviewed-only TM while leaving it on the always-on TMs.
   */
  lateinit var demotedKey: Key
  lateinit var demotedTargetTranslation: Translation

  /**
   * Key with an English source matching the "Hello world" entry on [sharedTm] and an empty
   * German target. Auto-translate tests use it to verify the source text gets filled in by
   * the shared-TM match.
   */
  lateinit var helloKey: Key
  lateinit var helloKeyGermanTranslation: Translation

  /**
   * Key on [projectWithTm] whose only matching shared-TM entry lives on [sharedTmWithPenalty]
   * (defaultPenalty = 25). Drives the penalty-gate regression test: auto-translate must
   * refuse to fill an empty target when the only match comes from a TM that the project
   * doesn't fully trust.
   */
  lateinit var farewellKey: Key
  lateinit var farewellKeyGermanTranslation: Translation

  /**
   * Pair driving the trashed-source-key regression test:
   *   - [trashedSourceKey] is on [projectWithTm], soft-deleted ("in trash"), with English
   *     "Trash source" + German "Müll-Übersetzung".
   *   - [trashedSourceLiveTwinKey] is a live sibling with the same English base and an
   *     empty German row.
   * Auto-translate of the live twin must not pull the deleted twin's German.
   */
  lateinit var trashedSourceKey: Key
  lateinit var trashedSourceLiveTwinKey: Key
  lateinit var trashedSourceLiveTwinGermanTranslation: Translation

  /** Non-default branch on [projectWithTm] — exercises the "TM writes skip non-default branches" rule. */
  lateinit var featureBranch: Branch

  /** Key attached to [featureBranch]; has no TM entries and no base translation. */
  lateinit var keyOnFeatureBranch: Key

  lateinit var orgMember: UserAccount

  /**
   * User with direct EDIT permission on [projectWithTm] but no role in the organization. Used
   * by org-level TM authorization tests to verify project-only access does NOT grant access
   * to org-scoped TM browsing.
   */
  lateinit var projectOnlyViewer: UserAccount

  init {
    root.apply {
      // Base project from BaseTestData has no TM — add German so tests can save translations
      projectBuilder.apply {
        germanLanguageNoTm =
          addLanguage {
            name = "German"
            tag = "de"
          }.self
      }

      // Second project with a TM assigned (simulates Business plan project)
      val projectWithTmBuilder =
        addProject {
          name = "Project With TM"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProject@{
          addPermission {
            project = this@buildProject.self
            user = this@TranslationMemoryTestData.user
            type = ProjectPermissionType.MANAGE
          }
          val english =
            addLanguage {
              name = "English"
              tag = "en"
              originalName = "English"
              this@buildProject.self.baseLanguage = this
            }.self
          germanLanguageWithTm =
            addLanguage {
              name = "German"
              tag = "de"
            }.self
          czechLanguageWithTm =
            addLanguage {
              name = "Czech"
              tag = "cs"
            }.self

          // Pre-existing key + translations
          addKey {
            name = "existing-key"
            existingKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Existing source"
            }
            existingTargetTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = "Bestehende Übersetzung"
              }.self
          }

          // Second key whose target is already REVIEWED. Drives the reviewed-only filter
          // tests (backfill picks this but not existingKey).
          addKey {
            name = "reviewed-key"
            reviewedKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Reviewed source"
            }
            reviewedTargetTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = "Überprüfte Übersetzung"
                state = TranslationState.REVIEWED
              }.self
          }

          // Key whose German target is TRANSLATED — the promote test bumps it to REVIEWED.
          addKey {
            name = "promoted-key"
            promotedKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Promoted source"
            }
            promotedTargetTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = "Hochgestufter Text"
              }.self
          }

          // Key whose German target is REVIEWED — the demote test bumps it down to TRANSLATED.
          addKey {
            name = "demotion-key"
            demotedKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Demotion source"
            }
            demotedTargetTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = "Herabzustufender Text"
                state = TranslationState.REVIEWED
              }.self
          }

          // Key with an empty German row — auto-translate tests fill it with the "Hello world"
          // match from sharedTm. Empty target text means it's filtered out of virtual-row counts
          // (the virtual-row predicate requires non-empty target text), so other tests aren't
          // perturbed by its presence.
          addKey {
            name = "hello-key"
            helloKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Hello world"
            }
            helloKeyGermanTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = ""
              }.self
            helloKeyCzechTranslation =
              addTranslation {
                language = czechLanguageWithTm
                key = this@keyBuilder.self
                text = ""
              }.self
          }

          // Source matches sharedTmWithPenalty (defaultPenalty = 25). No other TM carries
          // a "Farewell" entry, so any auto-fill here would have to come through the
          // penalised TM — penalty gate should block it.
          addKey {
            name = "farewell-key"
            farewellKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Farewell"
            }
            farewellKeyGermanTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = ""
              }.self
          }

          addKey {
            name = "trashed-source-key"
            deletedAt = Date()
            trashedSourceKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Trash source"
            }
            addTranslation {
              language = germanLanguageWithTm
              key = this@keyBuilder.self
              text = "Müll-Übersetzung"
            }
          }
          addKey {
            name = "trashed-source-live-twin"
            trashedSourceLiveTwinKey = this
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Trash source"
            }
            trashedSourceLiveTwinGermanTranslation =
              addTranslation {
                language = germanLanguageWithTm
                key = this@keyBuilder.self
                text = ""
              }.self
          }

          // Non-default branch + a key pinned to it. Used by tests that exercise
          // the "TM writes skip non-default branches" invariant.
          featureBranch =
            addBranch {
              name = "feature"
              isDefault = false
            }.self
          addKey {
            name = "feature-branch-key"
            this.branch = featureBranch
            keyOnFeatureBranch = this
          }
        }
      projectWithTm = projectWithTmBuilder.self

      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = projectWithTm.name
          sourceLanguageTag = "en"
          type = TranslationMemoryType.PROJECT
        }.build {
          projectTm = self
          // Priority 0 = top of the list by default; shared TMs in this fixture stack under it
          // at priorities 1..4. Project TMs store no entries — their content is computed
          // virtually from the project's translations at read time.
          assignProject(projectWithTm) { priority = 0 }
        }

      // Shared TMs are assigned with staggered priorities (1..4) for deterministic tie-break ordering.
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Shared Marketing TM"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
        }.build {
          sharedTm = self
          assignProject(projectWithTm) {
            priority = 1
            writeAccess = false
          }
          addEntry {
            sourceText = "Hello world"
            targetText = "Hallo Welt"
            targetLanguageTag = "de"
          }
          addEntry {
            sourceText = "Hello world"
            targetText = "Bonjour le monde"
            targetLanguageTag = "fr"
          }
          addEntry {
            sourceText = "Thank you"
            targetText = "Danke"
            targetLanguageTag = "de"
          }
        }

      // Shared TM with defaultPenalty preset — for tests that assert the penalty is
      // subtracted from similarity at suggestion time.
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Shared TM with default penalty"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
          defaultPenalty = 25
        }.build {
          sharedTmWithPenalty = self
          assignProject(projectWithTm) { priority = 2 }
          addEntry {
            sourceText = "Farewell"
            targetText = "Auf Wiedersehen"
            targetLanguageTag = "de"
          }
        }

      // Shared TM with a per-assignment penalty override — the assignment's penalty
      // (40) wins over the TM's defaultPenalty (10).
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Shared TM with per-assignment penalty override"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
          defaultPenalty = 10
        }.build {
          sharedTmWithOverride = self
          assignProject(projectWithTm) {
            priority = 3
            penalty = 40
          }
          addEntry {
            sourceText = "Good luck"
            targetText = "Viel Glück"
            targetLanguageTag = "de"
          }
        }

      // Reviewed-only shared TM. No pre-seeded entries so the reviewed-only filter tests
      // can observe which entries the pipeline adds through `onTranslationSaved` and
      // `backfillProjectTm` — no initial state to subtract from.
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Reviewed-only shared TM"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
          writeOnlyReviewed = true
        }.build {
          sharedTmReviewedOnly = self
          assignProject(projectWithTm) { priority = 4 }
        }

      // Shared TM declared with a different base language (fr) than projectWithTm (en).
      // Unassigned so assignment-time base-mismatch validation can be exercised.
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Mismatched base shared TM"
          sourceLanguageTag = "fr"
          type = TranslationMemoryType.SHARED
        }.build {
          mismatchedBaseSharedTm = self
        }

      // Create an unassigned SHARED TM with no entries — used by TMX import/export tests
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Unassigned Shared TM"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
        }.build {
          unassignedSharedTm = self
        }

      // Project + project-only TM, no shared assignments. The base-language change test
      // edits this project so the shared-TM-base-mismatch validation has nothing to fire on.
      val onlyProjectTmProjectBuilder =
        addProject {
          name = "Project With Only Project TM"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProject@{
          addPermission {
            project = this@buildProject.self
            user = this@TranslationMemoryTestData.user
            type = ProjectPermissionType.MANAGE
          }
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@buildProject.self.baseLanguage = this
          }
          addLanguage {
            name = "German"
            tag = "de"
          }
        }
      projectWithOnlyProjectTm = onlyProjectTmProjectBuilder.self

      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = projectWithOnlyProjectTm.name
          sourceLanguageTag = "en"
          type = TranslationMemoryType.PROJECT
        }.build {
          onlyProjectTm = self
          assignProject(projectWithOnlyProjectTm) { priority = 0 }
        }

      // Second project carrying a key whose English source matches projectWithTm's
      // existingKey ("Existing source") but with a different German translation. Combined
      // with multiProjectSharedTm below, this drives the multi-candidate-row UI test:
      // the same source text yields two distinct virtual rows on a single TM, one per
      // project, each with its own keyName + translation.
      val conflictProjectBuilder =
        addProject {
          name = "Conflict source project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProject@{
          addPermission {
            project = this@buildProject.self
            user = this@TranslationMemoryTestData.user
            type = ProjectPermissionType.MANAGE
          }
          val english =
            addLanguage {
              name = "English"
              tag = "en"
              originalName = "English"
              this@buildProject.self.baseLanguage = this
            }.self
          val german =
            addLanguage {
              name = "German"
              tag = "de"
            }.self
          val czech =
            addLanguage {
              name = "Czech"
              tag = "cs"
            }.self
          addKey {
            name = "shared-greeting"
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Existing source"
            }
            addTranslation {
              language = german
              key = this@keyBuilder.self
              text = "Bestehende Übersetzung aus Konfliktprojekt"
            }
          }
          // Key whose English source intentionally matches projectWithTm.helloKey. The cs
          // translation here is the only non-empty cs target for "Hello world" anywhere —
          // no stored TM entry covers it — so auto-translate has to find it via the
          // virtual-rows half of TmAutoTranslateProviderEeImpl on multiProjectSharedTm.
          addKey {
            name = "shared-greeting-cs"
          }.build keyBuilder@{
            addTranslation {
              language = english
              key = this@keyBuilder.self
              text = "Hello world"
            }
            addTranslation {
              language = czech
              key = this@keyBuilder.self
              text = "Ahoj světe"
            }
          }
        }
      conflictProject = conflictProjectBuilder.self

      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Multi-project shared TM"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
        }.build {
          multiProjectSharedTm = self
          assignProject(projectWithTm) { priority = 5 }
          assignProject(conflictProject) { priority = 6 }
          // Manual override on a source that also has virtual entries. After the recent
          // candidate split this entry must render on its own row (no key reference,
          // editable) rather than blending into a virtual row's German cell.
          addEntry {
            sourceText = "Existing source"
            targetText = "Manual override"
            targetLanguageTag = "de"
          }
        }

      // An org MEMBER user — used by shared TM permission tests
      userAccountBuilder.defaultOrganizationBuilder.build {
        addRole {
          user =
            addUserAccount {
              username = "tm_org_member"
            }.build {
              orgMember = self
            }.self
          type = OrganizationRoleType.MEMBER
        }
      }

      // Project-only viewer: direct permission on projectWithTm, no org role. Used by org-level
      // TM authorization tests to verify project-only access doesn't open the org-scoped TM
      // browse endpoints.
      projectOnlyViewer =
        addUserAccountWithoutOrganization {
          username = "tm_project_only_viewer"
        }.self
      projectWithTmBuilder.apply {
        addPermission {
          project = projectWithTmBuilder.self
          user = projectOnlyViewer
          type = ProjectPermissionType.EDIT
        }
      }
    }
  }

  /** Alias for the BaseTestData project. Still gets the auto-created project TM, just nothing
   *  pre-populated and no shared-TM assignments. */
  val projectWithoutTm get() = project
}
