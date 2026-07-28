package io.tolgee.unit

import io.tolgee.constants.Feature
import io.tolgee.dtos.queryResults.organization.PrivateOrganizationView
import io.tolgee.hateoas.organization.OrganizationModel
import io.tolgee.hateoas.organization.OrganizationModelAssembler
import io.tolgee.hateoas.organization.PrivateOrganizationModelAssembler
import io.tolgee.hateoas.permission.PermissionModel
import io.tolgee.hateoas.quickStart.QuickStartModelAssembler
import io.tolgee.publicBilling.CloudSubscriptionModelProvider
import io.tolgee.publicBilling.PublicCloudSubscriptionModel
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PrivateOrganizationModelAssemblerTest {
  private val organizationModelAssembler = Mockito.mock(OrganizationModelAssembler::class.java)
  private val quickStartModelAssembler = Mockito.mock(QuickStartModelAssembler::class.java)
  private val cloudSubscriptionModelProvider = Mockito.mock(CloudSubscriptionModelProvider::class.java)

  private val underTest =
    PrivateOrganizationModelAssembler(
      organizationModelAssembler,
      quickStartModelAssembler,
      cloudSubscriptionModelProvider,
    )

  private val view = Mockito.mock(PrivateOrganizationView::class.java)

  private val organizationModel =
    OrganizationModel(
      id = 42L,
      name = "The org",
      slug = "the-org",
      description = null,
      basePermissions = Mockito.mock(PermissionModel::class.java),
      currentUserRole = null,
      avatar = null,
    )

  private fun setup() {
    val organizationView =
      Mockito.mock(io.tolgee.dtos.queryResults.organization.OrganizationView::class.java)
    whenever(organizationView.id).thenReturn(42L)
    whenever(view.organization).thenReturn(organizationView)
    whenever(view.quickStart).thenReturn(null)
    whenever(organizationModelAssembler.toModel(any())).thenReturn(organizationModel)
  }

  @Test
  fun `a below-member reader gets no subscription and maps the passed limitedView`() {
    setup()

    val model =
      underTest.toModel(
        view,
        arrayOf(Feature.GLOSSARY, Feature.PREMIUM_SUPPORT),
        isAtLeastMember = false,
        limitedView = true,
      )

    model.enabledFeatures
      .toSet()
      .assert
      .isEqualTo(setOf(Feature.GLOSSARY, Feature.PREMIUM_SUPPORT))
    model.currentUserRole.assert.isNull()
    model.limitedView.assert.isEqualTo(true)
    model.activeCloudSubscription.assert.isNull()
    verify(cloudSubscriptionModelProvider, never()).provide(any())
  }

  @Test
  fun `a below-member with standing gets no subscription but a non-limited view`() {
    setup()

    val model =
      underTest.toModel(
        view,
        arrayOf(Feature.GLOSSARY),
        isAtLeastMember = false,
        limitedView = false,
      )

    model.limitedView.assert.isEqualTo(false)
    model.activeCloudSubscription.assert.isNull()
    verify(cloudSubscriptionModelProvider, never()).provide(any())
  }

  @Test
  fun `a member gets features, the cloud subscription and a non-limited view`() {
    setup()
    val subscription = Mockito.mock(PublicCloudSubscriptionModel::class.java)
    whenever(cloudSubscriptionModelProvider.provide(42L)).thenReturn(subscription)

    val model =
      underTest.toModel(
        view,
        arrayOf(Feature.PREMIUM_SUPPORT, Feature.GLOSSARY),
        isAtLeastMember = true,
        limitedView = false,
      )

    model.enabledFeatures
      .toSet()
      .assert
      .isEqualTo(setOf(Feature.PREMIUM_SUPPORT, Feature.GLOSSARY))
    model.limitedView.assert.isEqualTo(false)
    model.activeCloudSubscription.assert.isEqualTo(subscription)
  }
}
