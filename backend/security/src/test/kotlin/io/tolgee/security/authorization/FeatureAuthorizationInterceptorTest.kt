/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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

package io.tolgee.security.authorization

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.security.OrganizationHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

class FeatureAuthorizationInterceptorTest {
  private val enabledFeaturesProvider = Mockito.mock(EnabledFeaturesProvider::class.java)
  private val organizationHolder = Mockito.mock(OrganizationHolder::class.java)
  private val organization = Mockito.mock(OrganizationDto::class.java)

  private val featureAuthorizationInterceptor =
    FeatureAuthorizationInterceptor(
      enabledFeaturesProvider,
      organizationHolder,
    )

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(featureAuthorizationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(organizationHolder.organization).thenReturn(organization)
    Mockito.`when`(organization.id).thenReturn(1337L)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(
      enabledFeaturesProvider,
      organizationHolder,
      organization,
    )
  }

  @Test
  fun `it has no effect on endpoints without feature requirements`() {
    mockMvc.perform(get("/v2/organizations/1337/no-features")).andIsOk
  }

  @Test
  fun `it does not allow both annotations to be present`() {
    assertThrows<Exception> { mockMvc.perform(get("/v2/organizations/1337/both-annotations")) }
  }

  @Test
  fun `it allows access when all required features are enabled`() {
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.GLOSSARY)).thenReturn(true)
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.TASKS)).thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/requires-features")).andIsOk
  }

  @Test
  fun `it denies access when any required feature is not enabled`() {
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.GLOSSARY)).thenReturn(true)
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.TASKS)).thenReturn(false)

    assertThrows<Exception> { mockMvc.perform(get("/v2/organizations/1337/requires-features")) }
  }

  @Test
  fun `it allows access when at least one of the required features is enabled`() {
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.GLOSSARY)).thenReturn(false)
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.TASKS)).thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/requires-one-of-features")).andIsOk
  }

  @Test
  fun `it denies access when none of the required features are enabled`() {
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.GLOSSARY)).thenReturn(false)
    whenever(enabledFeaturesProvider.isFeatureEnabled(1337L, Feature.TASKS)).thenReturn(false)

    assertThrows<Exception> { mockMvc.perform(get("/v2/organizations/1337/requires-one-of-features")) }
  }

  @RestController
  class TestController {
    @GetMapping("/v2/organizations/{id}/no-features")
    fun noFeatures(
      @PathVariable id: Long,
    ) = "No features required for org #$id!"

    @GetMapping("/v2/organizations/{id}/requires-features")
    @RequiresFeatures(features = [Feature.GLOSSARY, Feature.TASKS])
    fun requiresFeatures(
      @PathVariable id: Long,
    ) = "Requires GLOSSARY and TASKS features for org #$id!"

    @GetMapping("/v2/organizations/{id}/requires-one-of-features")
    @RequiresOneOfFeatures(features = [Feature.GLOSSARY, Feature.TASKS])
    fun requiresOneOfFeatures(
      @PathVariable id: Long,
    ) = "Requires either GLOSSARY or TASKS feature for org #$id!"

    @GetMapping("/v2/organizations/{id}/both-annotations")
    @RequiresFeatures(features = [Feature.GLOSSARY])
    @RequiresOneOfFeatures(features = [Feature.TASKS])
    fun bothAnnotations(
      @PathVariable id: Long,
    ) = "This should throw an exception!"
  }
}
