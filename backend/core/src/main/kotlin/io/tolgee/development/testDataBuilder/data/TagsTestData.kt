package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag

class TagsTestData : BaseTestData("tagsTestUser", "tagsTestProject") {
  lateinit var noTagKey: Key
  lateinit var existingTag: Tag
  lateinit var existingTag2: Tag
  lateinit var existingTagKey: Key
  lateinit var existingTagKey2: Key

  init {
    projectBuilder.apply {
      addKey {
        name = "no tag key"
        noTagKey = this
      }
      addKey {
        name = "test key"
        existingTagKey = this
      }.build {
        addTag("test")
        existingTag = addTag("existing tag")
        existingTag2 = addTag("existing tag 2")
      }
      addKey {
        name = "existing tag key 2"
        existingTagKey2 = this
      }.build {
        addMeta {
          tags.add(existingTag2)
        }
      }
      (1..20).forEach { keyNum ->
        addKey {
          name = "test key $keyNum"
        }.build {
          (1..20).forEach { tagNum ->
            addTag("tag $keyNum $tagNum")
          }
        }
      }
    }
  }

  fun addNamespacedKey() {
    projectBuilder
      .addKey {
        name = "namespaced key"
      }.build {
        setNamespace("namespace")
        addTag("existing tag")
      }
  }

  fun generateVeryLotOfData() {
    projectBuilder.apply {
      (1..1000).forEach { keyNum ->
        addKey {
          name = "test key from lot of $keyNum"
        }.build {
          (1..2).forEach { tagNum ->
            addTag("tag from lot of $tagNum")
          }
        }
      }
    }
  }
}
