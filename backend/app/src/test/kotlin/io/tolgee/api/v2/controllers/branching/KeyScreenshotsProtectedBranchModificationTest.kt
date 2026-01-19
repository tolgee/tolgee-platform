package io.tolgee.api.v2.controllers.branching

import io.tolgee.fixtures.ProtectedBranchModificationTestBase
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.util.InMemoryFileStorage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class KeyScreenshotsProtectedBranchModificationTest : ProtectedBranchModificationTestBase() {
  lateinit var initialScreenshotUrl: String

  @BeforeAll
  fun before() {
    initialScreenshotUrl = tolgeeProperties.fileStorageUrl
  }

  @AfterAll
  fun after() {
    tolgeeProperties.fileStorageUrl = initialScreenshotUrl
    (fileStorage as InMemoryFileStorage).clear()
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
}
