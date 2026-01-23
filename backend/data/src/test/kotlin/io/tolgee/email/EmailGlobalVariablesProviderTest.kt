/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.email

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.email.EmailGlobalVariablesProvider.Companion.SELF_HOSTED_DEFAULT_QUALIFIER
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(EmailGlobalVariablesProvider::class)
class EmailGlobalVariablesProviderTest {
  @MockBean
  private lateinit var publicBillingConfProvider: PublicBillingConfProvider

  @MockBean
  private lateinit var tolgeeProperties: TolgeeProperties

  @MockBean
  private lateinit var frontendUrlProvider: FrontendUrlProvider

  @Autowired
  private lateinit var emailGlobalVariablesProvider: EmailGlobalVariablesProvider

  @Test
  fun `it returns the correct properties based on config in cloud`() {
    whenever(publicBillingConfProvider.invoke()).thenReturn(PublicBillingConfigurationDTO(true))
    whenever(tolgeeProperties.appName).thenReturn("Tolgee Test Edition")
    whenever(tolgeeProperties.backEndUrl).thenReturn("https://tolgee.test")

    emailGlobalVariablesProvider()
      .assert
      .containsEntry("isCloud", true)
      .containsEntry("instanceQualifier", "Tolgee Test Edition")
      .containsEntry("backendUrl", "https://tolgee.test")
      .hasSize(3)
  }

  @Test
  fun `it returns the correct properties based on config in self-hosted`() {
    whenever(publicBillingConfProvider.invoke()).thenReturn(PublicBillingConfigurationDTO(false))
    whenever(tolgeeProperties.appName).thenReturn("Tolgee Test Edition")
    whenever(tolgeeProperties.backEndUrl).thenReturn("https://tolgee.test")

    emailGlobalVariablesProvider()
      .assert
      .containsEntry("isCloud", false)
      .containsEntry("instanceQualifier", "tolgee.test")
      .containsEntry("backendUrl", "https://tolgee.test")
      .hasSize(3)
  }

  @Test
  fun `it gracefully handles bad frontend url configuration`() {
    whenever(publicBillingConfProvider.invoke()).thenReturn(PublicBillingConfigurationDTO(false))
    whenever(tolgeeProperties.appName).thenReturn("Tolgee Test Edition")
    whenever(tolgeeProperties.backEndUrl).thenReturn("https:/tolgee.test")

    emailGlobalVariablesProvider()
      .assert
      .containsEntry("isCloud", false)
      .containsEntry("instanceQualifier", SELF_HOSTED_DEFAULT_QUALIFIER)
      .containsEntry("backendUrl", "https:/tolgee.test")
      .hasSize(3)
  }

  @Test
  fun `it gracefully handles missing frontend url configuration`() {
    // That's a pathological situation to be in... but it can happen... :shrug:
    whenever(publicBillingConfProvider.invoke()).thenReturn(PublicBillingConfigurationDTO(false))
    whenever(tolgeeProperties.appName).thenReturn("Tolgee Test Edition")
    whenever(tolgeeProperties.frontEndUrl).thenReturn(null)

    emailGlobalVariablesProvider()
      .assert
      .containsEntry("isCloud", false)
      .containsEntry("instanceQualifier", SELF_HOSTED_DEFAULT_QUALIFIER)
      .containsEntry("backendUrl", null)
      .hasSize(3)
  }
}
