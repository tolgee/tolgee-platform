package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.AdditionalTestDataSaver
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.ee.repository.EeSubscriptionRepository
import org.springframework.stereotype.Component

@Component
class EeSaver(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
) : AdditionalTestDataSaver {
  override fun save(builder: TestDataBuilder) {
  }

  override fun clean(builder: TestDataBuilder) {
    eeSubscriptionRepository.deleteAll()
  }
}
