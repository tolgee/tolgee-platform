package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.batch.BatchJob
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

/**
 * A comment and a batch job both authored by the default user, so the self-access paths — which let an author reach
 * their own row without the scope the route otherwise demands — have something to reach.
 */
class ProjectApiKeySelfAccessTestData : BaseTestData() {
  lateinit var ownComment: TranslationComment

  lateinit var ownCommentTranslation: Translation

  lateinit var ownBatchJob: BatchJob

  init {
    projectBuilder
      .addKey { name = "pak-own-comment-key" }
      .build {
        addTranslation {
          language = projectBuilder.self.baseLanguage!!
          text = "value"
          ownCommentTranslation = this
        }.build {
          ownComment =
            addComment {
              text = "comment by the key owner"
              author = this@ProjectApiKeySelfAccessTestData.user
            }.self
        }
      }

    ownBatchJob =
      projectBuilder
        .addBatchJob {
          author = this@ProjectApiKeySelfAccessTestData.user
          totalItems = 1
        }.self
  }
}
