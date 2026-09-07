package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch

class OrganizationStatsWordCountTestData {
  val root = TestDataBuilder()

  lateinit var multiLangUser: UserAccount

  lateinit var multiLangOrg: Organization

  lateinit var branchDedupOrg: Organization

  lateinit var noBranchingOrg: Organization

  lateinit var emptyTranslationOrg: Organization

  lateinit var nullBranchDedupOrg: Organization

  init {
    root.apply {
      addMultiLangScenario()
      addBranchDedupScenario()
      addNoBranchingScenario()
      addEmptyTranslationScenario()
      addNullBranchDedupScenario()
    }
  }

  private fun TestDataBuilder.addMultiLangScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-multi-lang-user"
      }

    multiLangUser = userBuilder.self
    multiLangOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "Multi-Lang WC Project"
      organizationOwner = multiLangOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }
      addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
      }

      addKey { name = "ml-key1" }.build {
        addTranslation("en", "hello world")
        addTranslation("de", "foo bar baz")
      }
    }
  }

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
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }
      addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
      }

      addBranch {
        name = "main"
        project = self
        isDefault = true
      }.build { mainBranch = self }

      addBranch {
        name = "feature"
        project = self
        originBranch = mainBranch
      }.build { featureBranch = self }

      addKey {
        name = "bd-key1"
        branch = mainBranch
      }.build {
        addTranslation("en", "one")
        addTranslation("de", "eins zwei drei vier")
      }
      addKey {
        name = "bd-key1"
        branch = featureBranch
      }.build {
        addTranslation("en", "hello world")
        addTranslation("de", "eins zwei")
      }
    }
  }

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
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      val mainBranch =
        addBranch {
          name = "main"
          project = self
          isDefault = true
        }.build { self }.self

      val orphanBranch =
        addBranch {
          name = "orphan"
          project = self
          originBranch = mainBranch
        }.build { self }.self

      addKey { name = "nb-wc-key1" }.build {
        addTranslation("en", "hello world")
      }

      addKey {
        name = "nb-wc-key2"
        branch = orphanBranch
      }.build {
        addTranslation("en", "foo bar baz")
      }
    }
  }

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
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addKey { name = "et-key1" }.build {
        addTranslation("en", "")
      }
    }
  }

  /**
   * The same name on a branch and on no branch at all, in a branching project. Both are counted, so
   * they must still collapse to one — this is the case the unique indexes allow and the reason the
   * branch collapse keys on the name rather than on the branch.
   */
  private fun TestDataBuilder.addNullBranchDedupScenario() {
    val userBuilder =
      addUserAccount {
        username = "wc-null-branch-dedup-user"
      }

    nullBranchDedupOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "Null Branch Dedup WC Project"
      organizationOwner = nullBranchDedupOrg
      useBranching = true
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      val featureBranch =
        addBranch {
          name = "feature"
          project = self
        }.build { self }
          .self

      addKey { name = "nbd-key1" }.build {
        addTranslation("en", "one")
      }
      addKey {
        name = "nbd-key1"
        branch = featureBranch
      }.build {
        addTranslation("en", "hello world there")
      }
    }
  }
}
