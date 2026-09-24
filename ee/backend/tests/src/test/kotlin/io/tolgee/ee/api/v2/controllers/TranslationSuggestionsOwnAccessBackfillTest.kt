package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
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

  /** The key the rollback empties out: it is minted holding the new scope and nothing else. */
  private var inertKeyId: Long = 0

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
    val granularOrganizationBase = organizationBasePermissionId()
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `the rollback takes the scope back out of permissions and API keys alike`() {
    saveTestData()
    runBackfill()
    val granularMember = projectPermissionOf(testData.granularSuggester.self.id)
    val roleBasedMember = projectPermissionOf(testData.projectReviewer.self.id)
    storedScopes(granularMember).assert.contains("TRANSLATION_SUGGESTIONS_OWN_ACCESS")
    apiKeyScopeRows("TRANSLATION_SUGGESTIONS_OWN_ACCESS").assert.isEqualTo(2)

    runRollbackFromSchemaXml()

    storedScopes(granularMember).assert.containsExactly("TRANSLATIONS_VIEW", "TRANSLATIONS_SUGGEST")
    storedScopes(roleBasedMember).assert.isNull()
    apiKeyScopeRows("TRANSLATION_SUGGESTIONS_OWN_ACCESS").assert.isEqualTo(0)
    apiKeyScopeRows("TRANSLATIONS_VIEW").assert.isEqualTo(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a key left with no scopes by the rollback survives and is still readable`() {
    saveTestData()
    runBackfill()
    scopeNamesOf(inertKeyId).assert.containsExactly("TRANSLATION_SUGGESTIONS_OWN_ACCESS")

    runRollbackFromSchemaXml()

    // The rollback removes scope rows, it does not repair the key, so this one comes back with none. That is
    // deliberate: deleting a user's key on a rollback is worse. Nothing restores it on a roll-forward either —
    // the backfill only touches `permission`.
    apiKeyRowExists(inertKeyId).assert.isTrue()
    scopeNamesOf(inertKeyId).assert.isEmpty()

    // A scopeless key must fail closed, not break the endpoints that list it: those read `scopesEnum` leniently,
    // unlike the enum array a permission row stores.
    userAccount = testData.projectReviewer.self
    performAuthGet("/v2/api-keys?filterProjectId=${testData.relatedProject.self.id}")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.apiKeys").isArray.isNotEmpty
      }
  }

  private fun saveTestData() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    testData.relatedProject.addApiKey {
      key = "own-access-api-key"
      scopesEnum = mutableSetOf(Scope.TRANSLATIONS_VIEW, Scope.TRANSLATION_SUGGESTIONS_OWN_ACCESS)
      userAccount = testData.projectReviewer.self
    }
    val inertKey =
      testData.relatedProject.addApiKey {
        key = "own-access-only-api-key"
        scopesEnum = mutableSetOf(Scope.TRANSLATION_SUGGESTIONS_OWN_ACCESS)
        userAccount = testData.projectReviewer.self
      }
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
    inertKeyId = inertKey.self.id
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

  private fun organizationBasePermissionId(): Long =
    jdbcTemplate.queryForObject(
      "select id from permission where organization_id = ?",
      Long::class.java,
      testData.project.organizationOwner.id,
    )!!

  private fun apiKeyRowExists(apiKeyId: Long): Boolean =
    jdbcTemplate.queryForObject("select exists(select 1 from api_key where id = ?)", Boolean::class.java, apiKeyId)!!

  private fun scopeNamesOf(apiKeyId: Long): List<String?> =
    jdbcTemplate.queryForList(
      "select scopes_enum from api_key_scopes_enum where api_key_id = ?",
      String::class.java,
      apiKeyId,
    )

  private fun makeGranularWithEmptyScopes(permissionId: Long): Long {
    jdbcTemplate.update("update permission set type = null, scopes = '{}' where id = ?", permissionId)
    return permissionId
  }

  private fun runRollbackFromSchemaXml() {
    val xml =
      ClassPathResource("db/changelog/schema.xml")
        .inputStream
        .reader()
        .readText()
    val rollback =
      xml
        .substringAfter("""id="$BACKFILL_CHANGESET_ID"""")
        .substringBefore("</changeSet>")
        .substringAfter("<rollback>")
        .substringBefore("</rollback>")
    val statements =
      Regex("<sql>(.*?)</sql>", RegexOption.DOT_MATCHES_ALL)
        .findAll(rollback)
        .map { it.groupValues[1].trim() }
        .toList()

    statements.assert.hasSize(2)
    statements.forEach { jdbcTemplate.execute(it) }
  }

  private fun apiKeyScopeRows(scopeName: String): Int =
    jdbcTemplate.queryForObject(
      "select count(*) from api_key_scopes_enum k " +
        "join api_key a on a.id = k.api_key_id where a.project_id = ? and k.scopes_enum = ?",
      Int::class.java,
      testData.relatedProject.self.id,
      scopeName,
    )!!

  @Suppress("UNCHECKED_CAST")
  private fun storedScopes(permissionId: Long): List<String>? =
    jdbcTemplate.queryForObject(
      "select scopes from permission where id = ?",
      { rs, _ -> (rs.getArray(1)?.array as Array<String>?)?.toList() },
      permissionId,
    )

  companion object {
    private const val BACKFILL_CHANGESET_ID = "1789914853000-1"
  }
}
