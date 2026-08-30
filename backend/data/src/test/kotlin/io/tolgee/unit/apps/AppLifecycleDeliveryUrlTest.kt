package io.tolgee.unit.apps

import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AppLifecycleDeliveryUrlTest {
  @Test
  fun `appends the lifecycle path to a bare origin`() {
    AppLifecycleDeliveryService
      .deliveryUrl("https://app.example.com")
      .assert
      .isEqualTo("https://app.example.com/tolgee/lifecycle")
  }

  @Test
  fun `collapses a trailing slash`() {
    AppLifecycleDeliveryService
      .deliveryUrl("https://app.example.com/")
      .assert
      .isEqualTo("https://app.example.com/tolgee/lifecycle")
  }

  @Test
  fun `keeps an existing base path`() {
    AppLifecycleDeliveryService
      .deliveryUrl("https://app.example.com/mounted")
      .assert
      .isEqualTo("https://app.example.com/mounted/tolgee/lifecycle")
  }

  @Test
  fun `does not splice the path into a query or fragment`() {
    AppLifecycleDeliveryService
      .deliveryUrl("https://app.example.com?tenant=1")
      .assert
      .isEqualTo("https://app.example.com/tolgee/lifecycle")
    AppLifecycleDeliveryService
      .deliveryUrl("https://app.example.com#section")
      .assert
      .isEqualTo("https://app.example.com/tolgee/lifecycle")
  }
}
