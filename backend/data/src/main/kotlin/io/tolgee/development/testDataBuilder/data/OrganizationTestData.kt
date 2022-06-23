package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class OrganizationTestData : BaseTestData() {
  lateinit var franta: UserAccount
  lateinit var pepa: UserAccount
  lateinit var jirina: UserAccount

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

      addUserAccountWithoutOrganization {
        username = "pepa"
        name = "Josef Tyl"
        pepa = this
      }.build {
        setUserPreferences {
          language = "de"
        }
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

      val jirinaBuilder = addUserAccountWithoutOrganization {
        username = "jirina"
        name = "Jirina Svetla"
        jirina = this
      }

      addOrganization {
        name = "Jirinina Org 2"
        jirinaOrg = this
      }.build {
        addRole {
          user = jirina
          type = OrganizationRoleType.OWNER
        }
      }

      jirinaBuilder.apply {
        setUserPreferences {
          language = "ft"
          preferredOrganization = jirinaOrg
        }
      }
    }
  }
}
