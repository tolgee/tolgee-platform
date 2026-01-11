package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.Namespace

class NamespacesTestData : BaseTestData() {
  var keyWithoutNs: Key
  var keyInNs1: Key
  var singleKeyInNs2: Key
  lateinit var defaultUnusedProject: Project
  var namespaces = mutableMapOf<Pair<Project, String>, Namespace>()
  lateinit var dotProject: Project
  lateinit var translator: UserAccount

  init {
    root.apply {
      addUserAccount {
        username = "franta"
        translator = this
      }
    }

    projectBuilder.apply {
      keyWithoutNs = addKeyWithTranslation("key", null)
      keyInNs1 = addKeyWithTranslation("key", "ns-1")
      singleKeyInNs2 = addKeyWithTranslation("key", "ns-2")
      addKeyWithTranslation("key2", null)
      addKeyWithTranslation("key2", "ns-1")

      addPermission {
        user = translator
        type = ProjectPermissionType.TRANSLATE
      }
    }
    root.apply {
      addProject {
        name = "Project 2"
        useNamespaces = true
      }.build {
        addKeyWithTranslation("key", null)
        addKeyWithTranslation("key", "ns-1")
      }
    }
    root.apply {
      addProject {
        name = "Project 3"
        useNamespaces = true
        defaultUnusedProject = this
      }.build {
        addKeyWithTranslation("key", "ns-1")
      }
    }
    root.apply {
      addProject {
        name = "Project 4"
        useNamespaces = true
        dotProject = this
      }.build {
        addKeyWithTranslation("key", "ns.1")
      }
    }
  }

  private fun ProjectBuilder.addKeyWithTranslation(
    keyName: String,
    namespace: String?,
  ): Key {
    val keyBuilder =
      this
        .addKey {
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
