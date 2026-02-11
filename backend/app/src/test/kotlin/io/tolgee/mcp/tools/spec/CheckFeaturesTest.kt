package io.tolgee.mcp.tools.spec

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.OrganizationDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckFeaturesTest : McpSecurityTestBase() {
  @Test
  fun `no features required skips check`() {
    whenever(organizationHolder.organizationOrNull).thenReturn(null)

    sut.executeAs(spec(requiredFeatures = null, requiredOneOfFeatures = null)) {}

    verify(featureCheckService, never()).checkFeaturesEnabled(any(), any())
    verify(featureCheckService, never()).checkOneOfFeaturesEnabled(any(), any())
  }

  @Test
  fun `requiredFeatures set calls checkFeaturesEnabled`() {
    val orgDto = mock<OrganizationDto>()
    whenever(orgDto.id).thenReturn(42L)
    whenever(organizationHolder.organizationOrNull).thenReturn(orgDto)

    val features = arrayOf(Feature.GRANULAR_PERMISSIONS)

    sut.executeAs(spec(requiredFeatures = features)) {}

    verify(featureCheckService).checkFeaturesEnabled(eq(42L), eq(features))
  }

  @Test
  fun `requiredOneOfFeatures set calls checkOneOfFeaturesEnabled`() {
    val orgDto = mock<OrganizationDto>()
    whenever(orgDto.id).thenReturn(42L)
    whenever(organizationHolder.organizationOrNull).thenReturn(orgDto)

    val features = arrayOf(Feature.GRANULAR_PERMISSIONS)

    sut.executeAs(spec(requiredOneOfFeatures = features)) {}

    verify(featureCheckService).checkOneOfFeaturesEnabled(eq(42L), eq(features))
  }

  @Test
  fun `no org skips feature check even with features required`() {
    whenever(organizationHolder.organizationOrNull).thenReturn(null)

    sut.executeAs(spec(requiredFeatures = arrayOf(Feature.GRANULAR_PERMISSIONS))) {}

    verify(featureCheckService, never()).checkFeaturesEnabled(any(), any())
  }
}
