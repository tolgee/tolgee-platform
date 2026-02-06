package io.tolgee.config

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.ee.unit.batch.FakeOpenAiBatchApiService
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class BatchApiTestConfiguration {
  @Bean
  @Primary
  fun fakeOpenAiBatchApiService(currentDateProvider: CurrentDateProvider): FakeOpenAiBatchApiService {
    return FakeOpenAiBatchApiService(currentDateProvider)
  }

  @Bean
  @Primary
  fun machineTranslationProperties(): MachineTranslationProperties {
    return Mockito.spy(MachineTranslationProperties::class.java)
  }

  @Bean
  @Primary
  fun internalProperties(): InternalProperties {
    return Mockito.spy(InternalProperties::class.java)
  }
}
