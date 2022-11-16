package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace

class NamespacesTestData : BaseTestData() {
  var keyInNs1: Key
  var singleKeyInNs2: Key
  lateinit var defaultUnusedProject: Project
  var namespaces = mutableMapOf<Pair<Project, String>, Namespace>()
  lateinit var dotProject: Project
  lateinit var translator: UserAccount

  init {
    projectBuilder.testDataBuilder.apply {
      addUserAccount {
        username = "franta"
        translator = this
      }
    }
    projectBuilder.apply {
      addKey("key", null)
      keyInNs1 = addKey("key", "ns-1")
      singleKeyInNs2 = addKey("key", "ns-2")
      addKey("key2", null)
      addKey("key2", "ns-1")

      addPermission {
        user = translator
        type = Permission.ProjectPermissionType.TRANSLATE
      }
    }
    root.apply {
      addProject {
        name = "Project 2"
      }.build {
        addKey("key", null)
        addKey("key", "ns-1")
      }
    }
    root.apply {
      addProject {
        name = "Project 3"
        defaultUnusedProject = this
      }.build {
        addKey("key", "ns-1")
      }
    }
    root.apply {
      addProject {
        name = "Project 4"
        dotProject = this
      }.build {
        addKey("key", "ns.1")
      }
    }
  }

  private fun ProjectBuilder.addKey(keyName: String, namespace: String?): Key {
    val keyBuilder = this.addKey {
      name = keyName
    }.build {
      setNamespace(namespace)?.self?.let {
        namespaces[it.project to it.name] = it
      }
      addTranslation {
        language = englishLanguage
        text = "hello"
      }
    }
    return keyBuilder.self
  }
}
