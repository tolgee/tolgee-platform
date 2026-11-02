package io.tolgee.unit.branching

import io.tolgee.constants.MtServiceType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class TranslationMergeFlagsTest {
  @Test
  fun `flags stay on the target when the text does not move`() {
    val target =
      Translation("same").apply {
        outdated = true
        auto = true
        mtProvider = MtServiceType.GOOGLE
      }
    val source = Translation("same")
    target.merge(source, null, BranchKeyMergeResolutionType.SOURCE)
    target.outdated.assert.isTrue()
    target.auto.assert.isTrue()
    target.mtProvider.assert.isEqualTo(MtServiceType.GOOGLE)
  }

  @Test
  fun `flags follow the source when its text moves in`() {
    val target = Translation("old").apply { outdated = true }
    val source =
      Translation("new").apply {
        auto = true
        mtProvider = MtServiceType.DEEPL
      }
    target.merge(source, null, BranchKeyMergeResolutionType.SOURCE)
    target.text.assert.isEqualTo("new")
    target.outdated.assert.isFalse()
    target.auto.assert.isTrue()
    target.mtProvider.assert.isEqualTo(MtServiceType.DEEPL)
  }
}
