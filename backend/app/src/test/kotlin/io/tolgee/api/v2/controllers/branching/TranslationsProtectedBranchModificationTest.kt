package io.tolgee.api.v2.controllers.branching

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.ProtectedBranchModificationTestBase
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsProtectedBranchModificationTest : ProtectedBranchModificationTestBase() {
  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  override fun setup() {
    super.setup()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_EDIT])
  @Test
  fun `forbid updating translation on protected branch without protected scope`() {
    expectForbidden {
      translateKey("protected-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow updating translation on protected branch with protected scope`() {
    expectOk {
      translateKey("protected-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_EDIT])
  @Test
  fun `allow updating translation on non-protected branch`() {
    expectOk {
      translateKey("branched key 1", mainBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `forbid creating translation on protected branch without protected scope`() {
    expectForbidden {
      createOrUpdateTranslation("protected-new-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow creating translation on protected branch with protected scope`() {
    expectOk {
      createOrUpdateTranslation("protected-new-key", protectedBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `allow creating translation on non-protected branch`() {
    expectOk {
      createOrUpdateTranslation("main-new-key", mainBranchName)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `forbid setting translation state on protected branch without protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectForbidden {
      setTranslationStateToTranslated(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow setting translation state on protected branch with protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectOk {
      setTranslationStateToTranslated(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `allow setting translation state on non-protected branch`() {
    val translation = getTranslation(testData.branchedKey)
    expectOk {
      setTranslationStateToTranslated(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `forbid dismissing auto-translated state on protected branch without protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectForbidden {
      dismissAutoTranslatedState(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow dismissing auto-translated state on protected branch with protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectOk {
      dismissAutoTranslatedState(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `allow dismissing auto-translated state on non-protected branch`() {
    val translation = getTranslation(testData.branchedKey)
    expectOk {
      dismissAutoTranslatedState(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `forbid setting outdated flag on protected branch without protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectForbidden {
      setOutdatedFlag(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow setting outdated flag on protected branch with protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectOk {
      setOutdatedFlag(translation.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_STATE_EDIT])
  @Test
  fun `allow setting outdated flag on non-protected branch`() {
    val translation = getTranslation(testData.branchedKey)
    expectOk {
      setOutdatedFlag(translation.id)
    }
  }
}
