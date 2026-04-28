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

/**
 * Test data for Translation Memory tests.
 *
 * Provides two projects:
 *   - [projectWithTm]: has a project TM assigned, pre-populated with one key and its TM entry.
 *     Use for testing the write pipeline, update, delete cascade, and project deletion cleanup.
 *   - [projectWithoutTm]: the base project (inherited from [BaseTestData]) with no TM assigned.
 *     Use for testing that the write pipeline skips projects without TM.
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
   * Shared TM declared with a `fr` source — intentionally different from [projectWithTm]'s
   * English base. Drives the base-language-match validation: assigning this TM must be
   * rejected.
   */
  lateinit var mismatchedBaseSharedTm: TranslationMemory
  lateinit var snapshotSourceTm: TranslationMemory
  lateinit var germanLanguageNoTm: Language
  lateinit var germanLanguageWithTm: Language
  lateinit var existingKey: Key
  lateinit var existingTargetTranslation: Translation

  /**
   * Second key on [projectWithTm] whose German translation is already in `REVIEWED` state.
   * Combined with [existingKey] (state = TRANSLATED) this gives the backfill tests a mixed
   * state corpus: the reviewed-only TM should only pick up this key's translation.
   */
  lateinit var reviewedKey: Key
  lateinit var reviewedTargetTranslation: Translation

  /** Non-default branch on [projectWithTm] — exercises the "TM writes skip non-default branches" rule. */
  lateinit var featureBranch: Branch

  /** Key attached to [featureBranch]; has no TM entries and no base translation. */
  lateinit var keyOnFeatureBranch: Key

  lateinit var orgMember: UserAccount

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

      // Non-zero defaultPenalty is intentional: PROJECT-type TMs are hard-coded to
      // similarity=raw in the suggestion SQL, so tests using this TM exercise that invariant.
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = projectWithTm.name
          sourceLanguageTag = "en"
          type = TranslationMemoryType.PROJECT
          defaultPenalty = 50
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
          assignProject(projectWithTm) { priority = 1 }
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

      // A shared TM with one entry, not assigned to any project — used by keepData
      // snapshot tests that need to assign → disconnect → verify dedup
      userAccountBuilder.defaultOrganizationBuilder
        .addTranslationMemory {
          name = "Snapshot Source TM"
          sourceLanguageTag = "en"
          type = TranslationMemoryType.SHARED
        }.build {
          snapshotSourceTm = self
          addEntry {
            sourceText = "Repeat source"
            targetText = "Wiederhole Quelle"
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
    }
  }

  /** Alias for the TM-less project (inherited from [BaseTestData]). */
  val projectWithoutTm get() = project
}
