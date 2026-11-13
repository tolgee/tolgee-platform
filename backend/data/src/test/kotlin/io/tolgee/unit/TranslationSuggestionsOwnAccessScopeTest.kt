package io.tolgee.unit

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TranslationSuggestionsOwnAccessScopeTest {
  private val scope = Scope.TRANSLATION_SUGGESTIONS_OWN_ACCESS

  @Test
  fun `is not a read-only scope`() {
    assertThat(Scope.readOnlyScopes).doesNotContain(scope)
    assertThat(scope.isReadOnly()).isFalse()
  }

  @Test
  fun `implies translations view like every other suggestion scope`() {
    assertThat(scope.expand()).containsExactlyInAnyOrder(scope, Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW)
  }

  @Test
  fun `is implied by suggestions-manage and by nothing a suggester holds`() {
    assertThat(Scope.TRANSLATION_SUGGESTIONS_MANAGE.expand()).contains(scope)
    assertThat(Scope.TRANSLATIONS_SUGGEST.expand()).doesNotContain(scope)
  }

  @Test
  fun `is granted by every role preset except NONE`() {
    val roles = ProjectPermissionType.getRoles()
    assertThat(roles[ProjectPermissionType.VIEW.name]).contains(scope)
    assertThat(roles[ProjectPermissionType.TRANSLATE.name]).contains(scope)
    assertThat(roles[ProjectPermissionType.REVIEW.name]).contains(scope)
    assertThat(roles[ProjectPermissionType.EDIT.name]).contains(scope)
    assertThat(roles[ProjectPermissionType.MANAGE.name]).contains(scope)
    assertThat(roles[ProjectPermissionType.NONE.name]).doesNotContain(scope)
  }
}
