package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class OrganizationTestData : BaseTestData() {
  lateinit var franta: UserAccount
  lateinit var pepa: UserAccount
  lateinit var jirina: UserAccount
  lateinit var kvetoslav: UserAccount
  lateinit var milan: UserAccount

  lateinit var pepaOrg: Organization
  lateinit var jirinaOrg: Organization

  init {
    root.apply {
      addUserAccountWithoutOrganization {
        name = "Franta Kocourek"
        username = "to.si@proladite.krkovicku"
        franta = this
      }
      projectBuilder.addPermission {
        user = franta
      }

      val pepaBuilder =
        addUserAccountWithoutOrganization {
          username = "pepa"
          name = "Josef Tyl"
          pepa = this
        }

      projectBuilder.addPermission {
        user = pepa
        project = projectBuilder.self
      }

      addOrganization {
        name = "Organizacion"
        pepaOrg = this
      }.build {
        addRole {
          user = pepa
          type = OrganizationRoleType.OWNER
        }
      }

      pepaBuilder.build {
        setUserPreferences {
          language = "de"
          preferredOrganization = pepaOrg
        }
      }

      val jirinaBuilder =
        addUserAccountWithoutOrganization {
          username = "jirina"
          name = "Jirina Svetla"
          jirina = this
        }

      val kvetoslavBuilder =
        addUserAccountWithoutOrganization {
          username = "kvetoslav"
          name = "Kvetoslav Barta"
          kvetoslav = this
        }

      projectBuilder.build {
        addPermission {
          user = kvetoslav
        }
      }

      kvetoslavBuilder.build {
        setUserPreferences {
          preferredOrganization = this@OrganizationTestData.userAccountBuilder.defaultOrganizationBuilder.self
        }
      }

      addOrganization {
        name = "Jirinina Org 2"
        jirinaOrg = this
      }.build {
        addRole {
          user = jirina
          type = OrganizationRoleType.OWNER
        }
        // to make it possible for jirina to leave
        addRole {
          user = kvetoslavBuilder.self
          type = OrganizationRoleType.OWNER
        }
      }

      jirinaBuilder.apply {
        setUserPreferences {
          language = "ft"
          preferredOrganization = jirinaOrg
        }
      }
      addUserAccountWithoutOrganization {
        name = "Milan"
        username = "milan"
        milan = this
      }
    }
  }
}
