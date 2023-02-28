package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class PermissionsTestData {
  var projectBuilder: ProjectBuilder

  val root: TestDataBuilder = TestDataBuilder().apply {
    val admin = addUserAccount { username = "admin@admin.com" }
    val member = addUserAccount { username = "member@member.com" }

    projectBuilder = addProject { name = "Project" }.build {
      val en = addEnglish()
      val de = addGerman()
      val cs = addCzech()

      addPermission {
        this.user = admin.self
        this.type = ProjectPermissionType.MANAGE
      }

      addPermission {
        this.user = admin.self
        this.type = ProjectPermissionType.VIEW
      }

      for (i in 1..10) {
        addKey { name = "key-$i" }.build {
          listOf(en, de, cs).forEach {
            addTranslation {
              text = "${it.self.name} text $i"
              language = it.self
            }.build {
              addComment { text = "comment $i" }
            }
          }
        }
      }
    }
  }

  fun addUserWithPermissions(
    scopes: List<Scope>?,
    type: ProjectPermissionType?,
    viewLanguageTags: List<String>?,
    translateLanguageTags: List<String>?,
    stateChangeLanguageTags: List<String>?
  ) {
    val me = root.addUserAccount {
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
  }

  fun getLanguagesByTags(tags: List<String>?) = tags?.map { tag ->
    projectBuilder.data.languages.find { it.self.tag == tag }?.self ?: throw NotFoundException(
      Message.LANGUAGE_NOT_FOUND
    )
  }?.toMutableSet() ?: mutableSetOf()
}
