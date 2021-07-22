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
        noTagKey = self {
          name = "no tag key"
        }
      }
      addKey {
        self {
          name = "test key"
          existingTagKey = this
        }
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
        self {
          name = "existing tag key 2"
          existingTagKey2 = this
        }
        addMeta {
          self {
            tags.add(existingTag2)
          }
        }
      }
      (1..20).forEach { keyNum ->
        addKey {
          self {
            name = "test key $keyNum"
          }
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
      (1..2000).forEach { keyNum ->
        addKey {
          self {
            name = "test key from lot of $keyNum"
          }
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
