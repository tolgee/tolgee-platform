package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate

class TranslationSuggestionsOwnAccessBackfillTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionsTestData

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @AfterEach
  fun clean() {
    if (this::testData.isInitialized) {
      testDataService.cleanTestData(testData.root)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `granular author can delete their suggestion only after the backfill`() {
    saveTestData()
    userAccount = testData.granularSuggester.self
    val ownSuggestionPath = createOwnSuggestionAndGetItsPath()

    performProjectAuthDelete(ownSuggestionPath).andIsForbidden
    runBackfill()
    performProjectAuthDelete(ownSuggestionPath).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `backfill appends the scope to every granular row and to no other, and only once`() {
    saveTestData()
    val granularMember = projectPermissionOf(testData.granularSuggester.self.id)
    val granularMemberImplyingTheScope = projectPermissionOf(testData.suggestionModerator.self.id)
    val roleBasedMember = projectPermissionOf(testData.projectReviewer.self.id)
    val granularOrganizationBase = makeOrganizationBasePermissionGranularWith("TRANSLATIONS_VIEW")
    val granularRowWithEmptyScopes = makeGranularWithEmptyScopes(projectPermissionOf(testData.viewOnlyUser.self.id))

    repeat(2) {
      runBackfill()

      storedScopes(granularMember).assert.containsExactly(
        "TRANSLATIONS_VIEW",
        "TRANSLATIONS_SUGGEST",
        "TRANSLATION_SUGGESTIONS_OWN_ACCESS",
      )
      storedScopes(granularMemberImplyingTheScope).assert.containsExactly(
        "TRANSLATIONS_VIEW",
        "TRANSLATION_SUGGESTIONS_MANAGE",
        "TRANSLATION_SUGGESTIONS_OWN_ACCESS",
      )
      storedScopes(granularOrganizationBase).assert.containsExactly(
        "TRANSLATIONS_VIEW",
        "TRANSLATION_SUGGESTIONS_OWN_ACCESS",
      )
      storedScopes(roleBasedMember).assert.isNull()
      storedScopes(granularRowWithEmptyScopes).assert.isEmpty()
    }
  }

  private fun saveTestData() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
  }

  private fun createOwnSuggestionAndGetItsPath(): String {
    val path = "languages/${testData.czechLanguage.id}/key/${testData.keys[1].self.id}/suggestion"
    val id =
      performProjectAuthPost(path, CreateTranslationSuggestionRequest(translation = "Granular suggestion"))
        .andIsOk
        .getIdFromResponse()
    return "$path/$id"
  }

  private fun runBackfill() {
    jdbcTemplate.execute(
      ClassPathResource("db/changelog/translationSuggestionsOwnAccessBackfill.sql")
        .inputStream
        .reader()
        .readText(),
    )
  }

  private fun projectPermissionOf(userId: Long): Long =
    jdbcTemplate.queryForObject(
      "select id from permission where user_id = ? and project_id = ?",
      Long::class.java,
      userId,
      testData.relatedProject.self.id,
    )!!

  private fun makeOrganizationBasePermissionGranularWith(scope: String): Long {
    val permissionId =
      jdbcTemplate.queryForObject(
        "select id from permission where organization_id = ?",
        Long::class.java,
        testData.project.organizationOwner.id,
      )!!
    jdbcTemplate.update(
      "update permission set type = null, scopes = array[?]::varchar[] where id = ?",
      scope,
      permissionId,
    )
    return permissionId
  }

  private fun makeGranularWithEmptyScopes(permissionId: Long): Long {
    jdbcTemplate.update("update permission set type = null, scopes = '{}' where id = ?", permissionId)
    return permissionId
  }

  @Suppress("UNCHECKED_CAST")
  private fun storedScopes(permissionId: Long): List<String>? =
    jdbcTemplate.queryForObject(
      "select scopes from permission where id = ?",
      { rs, _ -> (rs.getArray(1)?.array as Array<String>?)?.toList() },
      permissionId,
    )
}
