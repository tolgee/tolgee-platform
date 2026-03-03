package io.tolgee.ee.api.v2.controllers.branching.modifications

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.BranchModificationTestBase
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class KeysBranchModificationTest : BranchModificationTestBase() {
  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  override fun setup() {
    super.setup()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE])
  @Test
  fun `forbid creating key on protected branch without protected scope`() {
    expectForbidden {
      createKey("protected-created-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow creating key on protected branch with protected scope`() {
    expectCreated {
      createKey("protected-created-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE])
  @Test
  fun `allow creating key on non-protected branch`() {
    expectCreated {
      createKey("main-created-key", mainBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid editing key on protected branch without protected scope`() {
    expectForbidden {
      editKey(testData.protectedKey.id, "protected-key-renamed")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow editing key on protected branch with protected scope`() {
    expectOk {
      editKey(testData.protectedKey.id, "protected-key-renamed")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow editing key on non-protected branch`() {
    expectOk {
      editKey(testData.branchedKey.id, "branched-key-renamed")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `forbid complex key update on protected branch without protected scope`() {
    expectForbidden {
      complexEditKey(testData.protectedKey.id, "protected-key-complex")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow complex key update on protected branch with protected scope`() {
    expectOk {
      complexEditKey(testData.protectedKey.id, "protected-key-complex")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `allow complex key update on non-protected branch`() {
    expectOk {
      complexEditKey(testData.branchedKey.id, "branched-key-complex")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  @Test
  fun `forbid deleting key on protected branch without protected scope`() {
    expectForbidden {
      deleteKey(testData.protectedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow deleting key on protected branch with protected scope`() {
    expectOk {
      deleteKey(testData.protectedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  @Test
  fun `allow deleting key on non-protected branch`() {
    expectOk {
      deleteKey(testData.branchedKey.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid setting disabled languages on protected branch without protected scope`() {
    expectForbidden {
      setDisabledLanguages(testData.protectedKey.id, listOf(testData.de.id))
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow setting disabled languages on protected branch with protected scope`() {
    expectOk {
      setDisabledLanguages(testData.protectedKey.id, listOf(testData.de.id))
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow setting disabled languages on non-protected branch`() {
    expectOk {
      setDisabledLanguages(testData.branchedKey.id, listOf(testData.de.id))
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `forbid editing key on non-default branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    editKey(testData.protectedKey.id, "protected-key-renamed").andIsBadRequest
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  @Test
  fun `allow editing key on default branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    expectOk {
      editKey(testData.branchedKey.id, "branched-key-renamed")
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  @Test
  fun `forbid deleting key on non-default non-protected branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    deleteKey(testData.unprotectedBranchedKey.id).andIsBadRequest
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_DELETE])
  @Test
  fun `forbid deleting key on non-default protected branch when branching feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    deleteKey(testData.protectedKey.id).andIsBadRequest
  }
}
