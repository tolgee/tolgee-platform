package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType

class AvatarsTestData : BaseTestData("franta", "Project with jpg avatar") {
  lateinit var organization: Organization

  init {
    userAccountBuilder.setAvatar("e2eTestResources/avatars/png_avatar.png")

    projectBuilder.build {
      setAvatar("e2eTestResources/avatars/jpg_avatar.jpg")
    }

    root.apply {
      addOrganization {
        name = "Yeey Organization"
        this@AvatarsTestData.organization = this
      }.build {
        addRole {
          user = this@AvatarsTestData.user
          type = OrganizationRoleType.OWNER
        }
        setAvatar("e2eTestResources/avatars/jpg_avatar.jpg")
      }
    }
  }
}
