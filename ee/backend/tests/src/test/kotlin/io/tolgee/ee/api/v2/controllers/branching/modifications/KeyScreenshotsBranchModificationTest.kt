package io.tolgee.ee.api.v2.controllers.branching.modifications

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.BranchModificationTestBase
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class KeyScreenshotsBranchModificationTest : BranchModificationTestBase() {
  lateinit var initialScreenshotUrl: String

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  override fun setup() {
    super.setup()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
  }

  @BeforeAll
  fun before() {
    initialScreenshotUrl = tolgeeProperties.fileStorageUrl
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialScreenshotUrl
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_UPLOAD])
  @Test
  fun `forbid uploading screenshot on protected branch without protected scope`() {
    expectForbidden {
      uploadScreenshot(testData.protectedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_UPLOAD, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow uploading screenshot on protected branch with protected scope`() {
    expectCreated {
      uploadScreenshot(testData.protectedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_UPLOAD])
  @Test
  fun `allow uploading screenshot on non-protected branch`() {
    expectCreated {
      uploadScreenshot(testData.branchedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_DELETE])
  @Test
  fun `forbid deleting screenshot on protected branch without protected scope`() {
    expectForbidden {
      deleteScreenshots(testData.protectedKey.id, testData.protectedScreenshotReference.screenshot.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_DELETE, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow deleting screenshot on protected branch with protected scope`() {
    expectOk {
      deleteScreenshots(testData.protectedKey.id, testData.protectedScreenshotReference.screenshot.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_DELETE])
  @Test
  fun `allow deleting screenshot on non-protected branch`() {
    expectOk {
      deleteScreenshots(testData.branchedKey.id, testData.branchedScreenshotReference.screenshot.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_UPLOAD])
  @Test
  fun `forbid uploading screenshot on non-default branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    uploadScreenshot(testData.protectedKey.id).andIsBadRequest
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.SCREENSHOTS_UPLOAD])
  @Test
  fun `allow uploading screenshot on default branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    expectCreated {
      uploadScreenshot(testData.branchedKey.id)
    }
  }
}
