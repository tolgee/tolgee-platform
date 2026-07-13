package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm

class GlossaryGuestAccessTestData : BaseTestData() {
  lateinit var organization: Organization
  lateinit var publicProject: Project
  lateinit var privateProject: Project
  lateinit var publicGlossary: Glossary
  lateinit var publicGlossaryTerm: GlossaryTerm
  lateinit var privateGlossary: Glossary
  lateinit var privateGlossaryTerm: GlossaryTerm
  lateinit var mixedGlossary: Glossary
  lateinit var degenerateProject: Project
  lateinit var degenerateGlossary: Glossary
  lateinit var softDeletedProject: Project
  lateinit var softDeletedProjectGlossary: Glossary
  lateinit var softDeletedBaseLangProject: Project
  lateinit var softDeletedBaseLangGlossary: Glossary
  lateinit var storedGuest: UserAccount
  lateinit var virtualGuest: UserAccount
  lateinit var directPermissionUser: UserAccount
  lateinit var noneOnlyUser: UserAccount

  init {
    root.apply {
      storedGuest =
        addUserAccount {
          username = "glossary_stored_guest"
          name = "Glossary Stored Guest"
        }.self

      virtualGuest =
        addUserAccount {
          username = "glossary_virtual_guest"
          name = "Glossary Virtual Guest"
        }.self

      directPermissionUser =
        addUserAccount {
          username = "glossary_direct_perm_user"
          name = "Glossary Direct Perm User"
        }.self

      noneOnlyUser =
        addUserAccount {
          username = "glossary_none_only_user"
          name = "Glossary None Only User"
        }.self

      userAccountBuilder.defaultOrganizationBuilder.build {
        organization = self
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Guest visible public project"
        public = true
      }.build {
        publicProject = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "A members only project"
      }.build {
        privateProject = self
        addBaseLanguage()
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
        addPermission {
          user = this@GlossaryGuestAccessTestData.directPermissionUser
          type = ProjectPermissionType.TRANSLATE
        }
        addPermission {
          user = this@GlossaryGuestAccessTestData.storedGuest
          type = ProjectPermissionType.NONE
        }
        addPermission {
          user = this@GlossaryGuestAccessTestData.noneOnlyUser
          type = ProjectPermissionType.NONE
        }
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Degenerate public project"
        public = true
      }.build {
        degenerateProject = self
        addBaseLanguage()
        clearBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Soft-deleted public project"
        public = true
      }.build {
        softDeletedProject = self
        addBaseLanguage()
        setDeletedAt()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Soft-deleted base language public project"
        public = true
      }.build {
        softDeletedBaseLangProject = self
        addBaseLanguage().setDeletedAt()
      }

      userAccountBuilder.defaultOrganizationBuilder.build {
        publicGlossary =
          addGlossary {
            name = "Public project glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.publicProject)
            publicGlossaryTerm =
              addTerm {
                description = "Public term"
              }.build {
                addTranslation {
                  languageTag = "en"
                  text = "public term"
                }
              }.self
          }.self

        privateGlossary =
          addGlossary {
            name = "Private project glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.privateProject)
            privateGlossaryTerm =
              addTerm {
                description = "Members-only term"
              }.build {
                addTranslation {
                  languageTag = "en"
                  text = "members-only term"
                }
              }.self
          }.self

        mixedGlossary =
          addGlossary {
            name = "Mixed assignment glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.publicProject)
            assignProject(this@GlossaryGuestAccessTestData.privateProject)
          }.self

        degenerateGlossary =
          addGlossary {
            name = "Degenerate project glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.degenerateProject)
          }.self

        softDeletedProjectGlossary =
          addGlossary {
            name = "Soft-deleted project glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.softDeletedProject)
          }.self

        softDeletedBaseLangGlossary =
          addGlossary {
            name = "Soft-deleted base language glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(this@GlossaryGuestAccessTestData.softDeletedBaseLangProject)
          }.self
      }
    }
  }

  private fun ProjectBuilder.addBaseLanguage(): LanguageBuilder {
    return addLanguage {
      name = "English"
      tag = "en"
      originalName = "English"
      this@addBaseLanguage.self.baseLanguage = this
    }
  }
}
