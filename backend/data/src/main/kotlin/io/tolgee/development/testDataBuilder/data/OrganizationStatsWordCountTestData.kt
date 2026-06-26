package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.branching.Branch

/**
 * Test data for OrganizationStatsService.getWordCount scenarios.
 *
 * Each scenario lives in its own organization so assertions stay isolated.
 *
 * Word counts used:
 *   "hello world" = 2 words
 *   "foo bar baz" = 3 words
 *   "one"         = 1 word
 */
class OrganizationStatsWordCountTestData {
  val root = TestDataBuilder()

  // Scenario 1: multi-language sum
  lateinit var multiLangOrg: Organization

  // Scenario 2: branch dedup (use_branching = true, MAX across branches)
  lateinit var branchDedupOrg: Organization

  // Scenario 3: non-branching excludes non-default-branch keys
  lateinit var noBranchingOrg: Organization

  // Scenario 4: empty translations contribute 0
  lateinit var emptyTranslationOrg: Organization

  init {
    root.apply {
      addMultiLangScenario()
      addBranchDedupScenario()
      addNoBranchingScenario()
      addEmptyTranslationScenario()
    }
  }

  /**
   * One key, two languages: EN "hello world" (2) + DE "foo bar baz" (3) = 5 words total.
   */
  private fun TestDataBuilder.addMultiLangScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-multi-lang-user"
      }

    multiLangOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "Multi-Lang WC Project"
      organizationOwner = multiLangOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"; tag = "en"; originalName = "English"
        this@build.self.baseLanguage = this
      }
      addLanguage {
        name = "German"; tag = "de"; originalName = "Deutsch"
      }

      addKey { name = "ml-key1" }.build {
        addTranslation("en", "hello world")
        addTranslation("de", "foo bar baz")
      }
    }
  }

  /**
   * Same key in two branches, one language:
   * main = "one" (1 word), feature = "hello world" (2 words).
   * MAX(1, 2) = 2 — should not sum to 3.
   */
  private fun TestDataBuilder.addBranchDedupScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-branch-dedup-user"
      }

    branchDedupOrg = userBuilder.defaultOrganizationBuilder.self

    lateinit var mainBranch: Branch
    lateinit var featureBranch: Branch

    addProject {
      name = "Branch Dedup WC Project"
      organizationOwner = branchDedupOrg
      useBranching = true
    }.build {
      addLanguage {
        name = "English"; tag = "en"; originalName = "English"
        this@build.self.baseLanguage = this
      }

      addBranch {
        name = "main"; project = self; isDefault = true
      }.build { mainBranch = self }

      addBranch {
        name = "feature"; project = self; originBranch = mainBranch
      }.build { featureBranch = self }

      addKey { name = "bd-key1"; branch = mainBranch }.build {
        addTranslation("en", "one")
      }
      addKey { name = "bd-key1"; branch = featureBranch }.build {
        addTranslation("en", "hello world")
      }
    }
  }

  /**
   * use_branching = false:
   * nb-wc-key1 on null branch (2 words) is counted;
   * nb-wc-key2 on orphan branch (3 words) is excluded.
   * Expected = 2.
   */
  private fun TestDataBuilder.addNoBranchingScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-no-branching-user"
      }

    noBranchingOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "No Branching WC Project"
      organizationOwner = noBranchingOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"; tag = "en"; originalName = "English"
        this@build.self.baseLanguage = this
      }

      val mainBranch =
        addBranch {
          name = "main"; project = self; isDefault = true
        }.build { self }.self

      val orphanBranch =
        addBranch {
          name = "orphan"; project = self; originBranch = mainBranch
        }.build { self }.self

      // On null branch (no branch_id) — counted
      addKey { name = "nb-wc-key1" }.build {
        addTranslation("en", "hello world")
      }

      // On orphan branch — excluded (branching disabled)
      addKey { name = "nb-wc-key2"; branch = orphanBranch }.build {
        addTranslation("en", "foo bar baz")
      }
    }
  }

  /**
   * Empty translation: text is "" which the StateListener converts to null on persist,
   * and the SQL filters out (text is not null and text <> '').
   * Expected word count = 0.
   */
  private fun TestDataBuilder.addEmptyTranslationScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-empty-translation-user"
      }

    emptyTranslationOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "Empty Translation WC Project"
      organizationOwner = emptyTranslationOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"; tag = "en"; originalName = "English"
        this@build.self.baseLanguage = this
      }

      addKey { name = "et-key1" }.build {
        addTranslation("en", "")
      }
    }
  }
}
