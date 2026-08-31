package io.tolgee.security

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.ApiKey
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Test databases are migrated from scratch, so the backfill always runs over zero rows in CI. These cases execute the
 * same statements against real rows, which is the only place the upgrade path is exercised at all.
 */
class TasksAssignedAccessBackfillTest : AbstractSpringTest() {
  lateinit var testData: BaseTestData

  private var grantingPermissionId: Long = 0
  private var emptyPermissionId: Long = 0
  private var alreadyScopedPermissionId: Long = 0
  private var roleBasedPermissionId: Long = 0
  private lateinit var grantingKey: ApiKey
  private lateinit var scopelessKey: ApiKey
  private lateinit var alreadyScopedKey: ApiKey

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    testData = BaseTestData("assigned_access_backfill_user", "assigned_access_backfill_project")
    grantingKey = keyFor("backfill_granting_key", mutableSetOf(Scope.TRANSLATIONS_VIEW))
    scopelessKey = keyFor("backfill_scopeless_key", mutableSetOf())
    alreadyScopedKey = keyFor("backfill_already_scoped_key", mutableSetOf(Scope.TASKS_ASSIGNED_ACCESS))
    testDataService.saveTestData(testData.root)

    // Inserted rather than built: the builder persists `scopes` as NULL, and null-versus-empty is the whole
    // distinction here — a NULL row is skipped by the migration either way, so a built fixture would assert nothing.
    grantingPermissionId = insertGranularPermission(arrayOf(Scope.TRANSLATIONS_VIEW.name, Scope.KEYS_VIEW.name))
    emptyPermissionId = insertGranularPermission(arrayOf())
    alreadyScopedPermissionId =
      insertGranularPermission(arrayOf(Scope.TRANSLATIONS_VIEW.name, Scope.TASKS_ASSIGNED_ACCESS.name))
    roleBasedPermissionId = insertGranularPermission(null)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a granular permission that grants something keeps the assignee elevation`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(scopesOf(grantingPermissionId)).contains(Scope.TASKS_ASSIGNED_ACCESS.name)
  }

  @Test
  fun `a permission that deliberately grants nothing does not become project visibility`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(scopesOf(emptyPermissionId)).isEmpty()
  }

  @Test
  fun `a role-based permission is left alone`() {
    // A role permission stores no scope list at all — ProjectPermissionType expands at runtime, so writing one here
    // would freeze it and stop the role from picking up scopes added later.
    jdbcTemplate.execute(backfillSql())

    assertThat(scopesOf(roleBasedPermissionId)).isEmpty()
    jdbcTemplate
      .queryForObject(
        "select scopes is null from permission where id = ?",
        Boolean::class.java,
        roleBasedPermissionId,
      )!!
      .let { assertThat(it).isTrue() }
  }

  @Test
  fun `a permission that already has the scope is not given it twice`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(scopesOf(alreadyScopedPermissionId).filter { it == Scope.TASKS_ASSIGNED_ACCESS.name }).hasSize(1)
  }

  @Test
  fun `an API key that grants something keeps the elevation`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(apiKeyScopesOf(grantingKey)).contains(Scope.TASKS_ASSIGNED_ACCESS.name)
  }

  @Test
  fun `an API key with no scopes at all stays empty`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(apiKeyScopesOf(scopelessKey)).isEmpty()
  }

  @Test
  fun `an API key that already has the scope does not get a duplicate row`() {
    jdbcTemplate.execute(backfillSql())

    assertThat(apiKeyScopesOf(alreadyScopedKey).filter { it == Scope.TASKS_ASSIGNED_ACCESS.name }).hasSize(1)
  }

  @Test
  fun `rolling back takes the scope off a granular permission again`() {
    jdbcTemplate.execute(backfillSql())
    jdbcTemplate.execute(rollbackSql())

    assertThat(scopesOf(grantingPermissionId)).doesNotContain(Scope.TASKS_ASSIGNED_ACCESS.name)
    assertThat(apiKeyScopesOf(grantingKey)).doesNotContain(Scope.TASKS_ASSIGNED_ACCESS.name)
  }

  @Test
  fun `rolling back also takes the scope off rows the backfill never wrote`() {
    jdbcTemplate.execute(backfillSql())
    jdbcTemplate.execute(rollbackSql())

    assertThat(scopesOf(alreadyScopedPermissionId)).doesNotContain(Scope.TASKS_ASSIGNED_ACCESS.name)
    assertThat(apiKeyScopesOf(alreadyScopedKey)).doesNotContain(Scope.TASKS_ASSIGNED_ACCESS.name)
  }

  @Test
  fun `a permission whose only scope was the new one becomes role-based NONE, not an empty scope array`() {
    // An emptied granular row is un-persistable: PermissionListeners nulls the empty array and then requires
    // exactly one of scopes or type, so the next edit of that member's permission would throw.
    val onlyScopedId = insertGranularPermission(arrayOf(Scope.TASKS_ASSIGNED_ACCESS.name))

    jdbcTemplate.execute(backfillSql())
    jdbcTemplate.execute(rollbackSql())

    jdbcTemplate
      .queryForObject(
        "select type from permission where id = ? and scopes is null",
        String::class.java,
        onlyScopedId,
      )!!
      .let { assertThat(it).isEqualTo(ProjectPermissionType.NONE.name) }
  }

  @Test
  fun `rolling back leaves a role-based permission's null scopes alone`() {
    jdbcTemplate.execute(backfillSql())
    jdbcTemplate.execute(rollbackSql())

    jdbcTemplate
      .queryForObject(
        "select scopes is null from permission where id = ?",
        Boolean::class.java,
        roleBasedPermissionId,
      )!!
      .let { assertThat(it).isTrue() }
  }

  private fun insertGranularPermission(grantedScopes: Array<String>?): Long {
    val id = jdbcTemplate.queryForObject("select nextval('hibernate_sequence')", Long::class.java)!!
    jdbcTemplate.update(
      "insert into permission (id, project_id, user_id, scopes, created_at, updated_at) " +
        "values (?, ?, ?, ?, now(), now())",
      id,
      testData.project.id,
      testData.user.id,
      grantedScopes,
    )
    return id
  }

  private fun keyFor(
    keyValue: String,
    grantedScopes: MutableSet<Scope?>,
  ): ApiKey {
    lateinit var apiKey: ApiKey
    testData.projectBuilder.addApiKey {
      key = keyValue
      scopesEnum = grantedScopes
      userAccount = testData.user
      apiKey = this
    }
    return apiKey
  }

  private fun scopesOf(permissionId: Long): List<String> =
    jdbcTemplate
      .queryForList("select unnest(scopes) from permission where id = ?", String::class.java, permissionId)
      .filterNotNull()

  private fun apiKeyScopesOf(apiKey: ApiKey): List<String> =
    jdbcTemplate
      .queryForList(
        "select scopes_enum from api_key_scopes_enum where api_key_id = ?",
        String::class.java,
        apiKey.id,
      ).filterNotNull()

  private fun backfillSql(): String = changelogSql("tasksAssignedAccessBackfill.sql")

  private fun rollbackSql(): String = changelogSql("tasksAssignedAccessBackfillRollback.sql")

  private fun changelogSql(fileName: String): String =
    ClassPathResource("db/changelog/$fileName")
      .inputStream
      .reader()
      .readText()
}
