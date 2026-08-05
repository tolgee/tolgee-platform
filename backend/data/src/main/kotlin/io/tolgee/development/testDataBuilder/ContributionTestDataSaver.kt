package io.tolgee.development.testDataBuilder

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class ContributionTestDataSaver(
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
) : AdditionalTestDataSaver {
  override fun save(builder: TestDataBuilder) {}

  override fun clean(builder: TestDataBuilder) {}

  override fun afterSave(builder: TestDataBuilder) {
    builder.data.projects.forEach { projectBuilder ->
      projectBuilder.data.contributions.forEach { contribution ->
        ContributorActivityRecorder.record(
          entityManager,
          currentDateProvider,
          projectBuilder.self.id,
          contribution.author.id,
          contribution.at,
        )
      }
    }
  }
}
