/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

class NotificationsTestData {
  lateinit var admin: UserAccount

  lateinit var orgAdmin: UserAccount
  lateinit var projectManager: UserAccount
  lateinit var frenchTranslator: UserAccount
  lateinit var czechTranslator: UserAccount
  lateinit var germanTranslator: UserAccount
  lateinit var frenchCzechTranslator: UserAccount

  lateinit var bob: UserAccount
  lateinit var alice: UserAccount

  lateinit var acme: Organization

  lateinit var project1: Project
  lateinit var project2: Project

  lateinit var keyProject1: Key
  lateinit var key1EnTranslation: Translation
  lateinit var key1FrTranslation: Translation
  lateinit var key1CzTranslation: Translation

  lateinit var keyProject2: Key
  lateinit var key2EnTranslation: Translation

  val root: TestDataBuilder = TestDataBuilder()

  init {
    root.apply {
      addUserAccount {
        name = "Admin"
        username = "admin"
        isInitialUser = true
        role = UserAccount.Role.ADMIN

        admin = this
      }

      addUserAccountWithoutOrganization {
        name = "Acme 1 chief"
        username = "chief-acme-1"

        orgAdmin = this
      }

      addUserAccountWithoutOrganization {
        name = "Project manager"
        username = "project-manager"

        projectManager = this
      }

      addUserAccountWithoutOrganization {
        name = "Translator (french)"
        username = "french-translator"

        frenchTranslator = this
      }

      addUserAccountWithoutOrganization {
        name = "Translator (czech)"
        username = "czech-translator"

        czechTranslator = this
      }

      addUserAccountWithoutOrganization {
        name = "Translator (german)"
        username = "german-translator"

        germanTranslator = this
      }

      addUserAccountWithoutOrganization {
        name = "Translator (french and czech)"
        username = "french-czech-translator"

        frenchCzechTranslator = this
      }

      addUserAccount {
        name = "Bob"
        username = "bob"

        bob = this
      }

      addUserAccount {
        name = "Alice"
        username = "alice"

        alice = this
      }

      addOrganization {
        name = "ACME Corporation"
        slug = "acme"

        this@NotificationsTestData.acme = this@addOrganization
      }.build {
        addRole {
          user = orgAdmin
          type = OrganizationRoleType.OWNER
        }

        addRole {
          user = projectManager
          type = OrganizationRoleType.MEMBER
        }

        addRole {
          user = frenchTranslator
          type = OrganizationRoleType.MEMBER
        }

        addRole {
          user = czechTranslator
          type = OrganizationRoleType.MEMBER
        }

        addRole {
          user = germanTranslator
          type = OrganizationRoleType.MEMBER
        }

        addRole {
          user = frenchCzechTranslator
          type = OrganizationRoleType.MEMBER
        }

        addProject {
          name = "Explosive Type-Checker"
          slug = "explosive-type-checker"
          organizationOwner = acme

          project1 = this
        }.build {
          val en = addEnglish()
          val fr = addFrench()
          val cz = addCzech()
          val de = addGerman()

          val key = addKey {
            name = "some-key"

            this@NotificationsTestData.keyProject1 = this@addKey
          }

          addTranslation {
            this.key = key.self
            language = en.self
            text = "Some translation"
            state = TranslationState.REVIEWED

            this@NotificationsTestData.key1EnTranslation = this@addTranslation
          }

          addTranslation {
            this.key = key.self
            language = fr.self
            text = "Some french translation"
            state = TranslationState.REVIEWED

            this@NotificationsTestData.key1FrTranslation = this@addTranslation
          }

          addTranslation {
            this.key = key.self
            language = cz.self
            text = "Some czech translation"

            this@NotificationsTestData.key1CzTranslation = this@addTranslation
          }

          // --- --- ---
          addPermission {
            user = projectManager
            type = ProjectPermissionType.MANAGE
          }

          addPermission {
            user = frenchTranslator
            type = ProjectPermissionType.EDIT
            viewLanguages.add(en.self)
            viewLanguages.add(fr.self)
            translateLanguages.add(en.self)
            translateLanguages.add(fr.self)
            stateChangeLanguages.add(en.self)
            stateChangeLanguages.add(fr.self)
          }

          addPermission {
            user = czechTranslator
            type = ProjectPermissionType.EDIT
            viewLanguages.add(en.self)
            viewLanguages.add(cz.self)
            translateLanguages.add(en.self)
            translateLanguages.add(cz.self)
            stateChangeLanguages.add(en.self)
            stateChangeLanguages.add(cz.self)
          }

          addPermission {
            user = germanTranslator
            type = ProjectPermissionType.EDIT
            viewLanguages.add(en.self)
            viewLanguages.add(de.self)
            translateLanguages.add(en.self)
            translateLanguages.add(de.self)
            stateChangeLanguages.add(en.self)
            stateChangeLanguages.add(de.self)
          }

          addPermission {
            user = frenchCzechTranslator
            type = ProjectPermissionType.EDIT
            viewLanguages.add(en.self)
            viewLanguages.add(fr.self)
            viewLanguages.add(cz.self)
            translateLanguages.add(en.self)
            translateLanguages.add(fr.self)
            translateLanguages.add(cz.self)
            stateChangeLanguages.add(en.self)
            stateChangeLanguages.add(fr.self)
            stateChangeLanguages.add(cz.self)
          }

          addPermission {
            user = bob
            scopes = arrayOf(Scope.TRANSLATIONS_EDIT)
          }
        }

        addProject {
          name = "Rocket-Powered Office Chair Controller"
          slug = "rpocc"
          organizationOwner = acme

          project2 = this
        }.build {
          val en = addEnglish()
          val key = addKey {
            name = "some-key"

            this@NotificationsTestData.keyProject2 = this@addKey
          }

          addTranslation {
            this.key = key.self
            language = en.self
            text = "Some translation"

            this@NotificationsTestData.key2EnTranslation = this@addTranslation
          }

          addPermission {
            user = alice
            scopes = arrayOf(Scope.TRANSLATIONS_EDIT, Scope.SCREENSHOTS_UPLOAD)
          }
        }
      }
    }
  }
}
