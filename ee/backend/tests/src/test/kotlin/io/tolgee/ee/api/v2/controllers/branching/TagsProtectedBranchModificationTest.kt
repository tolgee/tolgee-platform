package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.ProtectedBranchModificationTestBase
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class TagsProtectedBranchModificationTest : ProtectedBranchModificationTestBase() {
  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  override fun setup() {
    super.setup()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid tagging key on protected branch without protected scope`() {
    expectForbidden {
      tagKey(testData.protectedKey.id, "protected-tag")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow tagging key on protected branch with protected scope`() {
    expectOk {
      tagKey(testData.protectedKey.id, "protected-tag")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow tagging key on non-protected branch`() {
    expectOk {
      tagKey(testData.branchedKey.id, "main-tag")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid removing tag on protected branch without protected scope`() {
    expectForbidden {
      removeTag(testData.protectedKey.id, testData.firstTag.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow removing tag on protected branch with protected scope`() {
    expectOk {
      removeTag(testData.protectedKey.id, testData.firstTag.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow removing tag on non-protected branch`() {
    expectOk {
      removeTag(testData.branchedKey.id, testData.firstTag.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid complex tag operation on protected branch without protected scope`() {
    expectForbidden {
      executeComplexTagOperation(testData.protectedKey.id, protectedBranchName, "protected-complex-tag")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow complex tag operation on protected branch with protected scope`() {
    expectOk {
      executeComplexTagOperation(testData.protectedKey.id, protectedBranchName, "protected-complex-tag")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow complex tag operation on non-protected branch`() {
    expectOk {
      executeComplexTagOperation(testData.branchedKey.id, mainBranchName, "main-complex-tag")
    }
  }
}
