package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TmSuggestionsE2eTestData
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/tm-suggestions"])
class TmSuggestionsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var em: EntityManager

  override val testData: TestDataBuilder
    get() = TmSuggestionsE2eTestData().root

  // Backdate the timestamp on the "X days ago" entry so the panel renders the relative-time
  // segment deterministically. Spring's `@LastModifiedDate` listener overwrites updatedAt on
  // persist — the only way to inject a known past timestamp is a native UPDATE after save.
  override fun afterTestDataStored(data: TestDataBuilder) {
    em.createNativeQuery(
      "UPDATE translation_memory_entry SET updated_at = NOW() - INTERVAL '3 days' WHERE target_text = :t",
    ).setParameter("t", TmSuggestionsE2eTestData.BACKDATED_TARGET_TEXT).executeUpdate()
  }
}
