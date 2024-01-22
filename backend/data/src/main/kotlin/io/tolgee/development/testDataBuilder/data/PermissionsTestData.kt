package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.springframework.core.io.ClassPathResource

class PermissionsTestData {
  var projectBuilder: ProjectBuilder
  var organizationBuilder: OrganizationBuilder
  var admin: UserAccountBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      admin = addUserAccount { username = "admin@admin.com" }
      organizationBuilder = admin.defaultOrganizationBuilder

      val member = addUserAccount { username = "member@member.com" }
      addUserAccount { username = "no@no.no" }

      val orgOnly = addUserAccount { username = "org@org.org" }

      organizationBuilder.build {
        addRole {
          user = orgOnly.self
          type = OrganizationRoleType.MEMBER
        }
      }

      projectBuilder =
        addProject { name = "Project" }.build {
          val en = addEnglish()
          val de = addGerman()
          val cs = addCzech()

          addPermission {
            this.user = member.self
            this.type = ProjectPermissionType.VIEW
          }

          val keyBuilders =
            (1..10).map { i ->
              addKey { name = "key-$i" }.build {

                listOf(en, de, cs).forEach {
                  addTranslation {
                    text = "${it.self.name} text $i"
                    language = it.self
                  }.build {
                    addComment {
                      text = "comment $i"
                      author = admin.self
                    }
                  }
                }
              }
            }

          keyBuilders[0].apply {
            val screenshotResource =
              ClassPathResource("development/testScreenshot.png", this::class.java.getClassLoader())
            addScreenshot(screenshotResource) {}
          }
        }
    }

  fun addUserWithPermissions(
    scopes: List<Scope>? = null,
    type: ProjectPermissionType? = null,
    viewLanguageTags: List<String>? = null,
    translateLanguageTags: List<String>? = null,
    stateChangeLanguageTags: List<String>? = null,
    organizationBaseScopes: List<Scope>? = null,
  ): UserAccount {
    val me =
      root.addUserAccount {
        username = "me@me.me"
      }

    projectBuilder.build {
      addPermission {
        user = me.self
        this.type = type
        scopes?.toTypedArray()?.let { this.scopes = it }
        viewLanguages = getLanguagesByTags(viewLanguageTags)
        translateLanguages = getLanguagesByTags(translateLanguageTags)
        stateChangeLanguages = getLanguagesByTags(stateChangeLanguageTags)
      }
    }

    if (organizationBaseScopes != null) {
      organizationBuilder.self.basePermission.type = null
      organizationBuilder.self.basePermission.scopes = organizationBaseScopes.toTypedArray()
      organizationBuilder.build {
        addRole {
          user = me.self
          this.type = OrganizationRoleType.MEMBER
        }
      }
    }
    return me.self
  }

  fun addUnrelatedUsers() {
    val user =
      root.addUserAccount {
        username = "unrelated@ur.com"
      }

    root.addUserAccount { username = "another@an.com" }

    root.addProject {
      name = "unrelated"
      organizationOwner = user.defaultOrganizationBuilder.self
    }.build {
      addPermission {
        this.user = user.self
        type = ProjectPermissionType.VIEW
      }
    }
  }

  private fun getLanguagesByTags(tags: List<String>?) =
    tags?.map { tag ->
      projectBuilder.data.languages.find { it.self.tag == tag }?.self ?: throw NotFoundException(
        Message.LANGUAGE_NOT_FOUND,
      )
    }?.toMutableSet() ?: mutableSetOf()
}
