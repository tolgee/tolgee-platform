package io.tolgee.development.testDataBuilder

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.service.notification.NotificationProjectPreferenceService
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class NotificationPreferenceTestDataSaver(
  private val projectPreferenceService: NotificationProjectPreferenceService,
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
) : AdditionalTestDataSaver {
  override fun save(builder: TestDataBuilder) {}

  override fun clean(builder: TestDataBuilder) {}

  override fun afterSave(builder: TestDataBuilder) {
    builder.data.projects.forEach { projectBuilder ->
      projectBuilder.data.translationReviews.forEach { recordReview(projectBuilder, it) }
      projectBuilder.data.notificationLanguageChoices.forEach { choice ->
        projectPreferenceService.set(choice.user.id, projectBuilder.self.id, choice.languages.map { it.id })
      }
    }
  }

  private fun recordReview(
    projectBuilder: ProjectBuilder,
    review: ProjectBuilder.TranslationReview,
  ) {
    val revisionId = jdbcTemplate.queryForObject("select nextval('activity_sequence')", Long::class.java)!!
    jdbcTemplate.update(
      """
      insert into activity_revision (id, project_id, author_id, type, "timestamp")
      values (?, ?, ?, 'SET_TRANSLATION_STATE', ?)
      """,
      revisionId,
      projectBuilder.self.id,
      review.author.id,
      Timestamp((review.at ?: currentDateProvider.date).time),
    )
    jdbcTemplate.update(
      """
      insert into activity_modified_entity
        (activity_revision_id, entity_class, entity_id, revision_type, modifications, describing_relations)
      values (?, 'Translation', ?, 1, '{"state": {"old": "TRANSLATED", "new": "REVIEWED"}}'::jsonb,
        jsonb_build_object(
          'language', jsonb_build_object('entityClass', 'Language', 'entityId', ?::bigint),
          'key', jsonb_build_object('entityClass', 'Key', 'entityId', ?::bigint)
        ))
      """,
      revisionId,
      review.translation.id,
      review.translation.language.id,
      review.translation.key.id,
    )
  }
}
