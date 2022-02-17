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
        addMeta {
          self {
            tags.add(
              Tag().apply {
                project = projectBuilder.self
                name = "test"
              }
            )
            tags.add(
              Tag().apply {
                project = projectBuilder.self
                name = "existing tag"
                existingTag = this
              }
            )
            tags.add(
              Tag().apply {
                project = projectBuilder.self
                name = "existing tag 2"
                existingTag2 = this
              }
            )
          }
        }
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
          addMeta {
            self {
              (1..20).forEach { tagNum ->
                tags.add(
                  Tag().apply {
                    project = projectBuilder.self
                    name = "tag $keyNum $tagNum"
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  fun generateVeryLotOfData() {
    projectBuilder.apply {
      (1..1000).forEach { keyNum ->
        addKey {
          name = "test key from lot of $keyNum"
        }.build {
          addMeta {
            self {
              (1..2).forEach { tagNum ->
                tags.add(
                  Tag().apply {
                    project = projectBuilder.self
                    name = "tag from lot of $tagNum"
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}
