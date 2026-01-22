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
class TranslationLabelsProtectedBranchModificationTest : ProtectedBranchModificationTestBase() {
  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  override fun setup() {
    super.setup()
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_LABELS, Feature.BRANCHING)
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `forbid assigning label by key and language on protected branch without protected scope`() {
    expectForbidden {
      assignLabelByKeyAndLanguage(testData.protectedKey.id, testData.en.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow assigning label by key and language on protected branch with protected scope`() {
    expectOk {
      assignLabelByKeyAndLanguage(testData.protectedKey.id, testData.en.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `allow assigning label by key and language on non-protected branch`() {
    expectOk {
      assignLabelByKeyAndLanguage(testData.branchedKey.id, testData.en.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `forbid assigning label by translation id on protected branch without protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectForbidden {
      assignLabelByTranslationId(translation.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow assigning label by translation id on protected branch with protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectOk {
      assignLabelByTranslationId(translation.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `allow assigning label by translation id on non-protected branch`() {
    val translation = getTranslation(testData.branchedKey)
    expectOk {
      assignLabelByTranslationId(translation.id, testData.thirdLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `forbid unassigning label on protected branch without protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectForbidden {
      unassignLabel(translation.id, testData.firstLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN, Scope.BRANCH_PROTECTED_MODIFY])
  @Test
  fun `allow unassigning label on protected branch with protected scope`() {
    val translation = getTranslation(testData.protectedKey)
    expectOk {
      unassignLabel(translation.id, testData.firstLabel.id)
    }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATION_LABEL_ASSIGN])
  @Test
  fun `allow unassigning label on non-protected branch`() {
    val translation = getTranslation(testData.branchedKey)
    expectOk {
      unassignLabel(translation.id, testData.firstLabel.id)
    }
  }

  private fun assignLabelByKeyAndLanguage(
    keyId: Long,
    languageId: Long,
    labelId: Long,
  ) = performProjectAuthPut(
    "translations/label",
    mapOf(
      "keyId" to keyId,
      "languageId" to languageId,
      "labelId" to labelId,
    ),
  )

  private fun assignLabelByTranslationId(
    translationId: Long,
    labelId: Long,
  ) = performProjectAuthPut(
    "translations/$translationId/label/$labelId",
  )

  private fun unassignLabel(
    translationId: Long,
    labelId: Long,
  ) = performProjectAuthDelete(
    "translations/$translationId/label/$labelId",
  )
}
