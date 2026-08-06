package io.tolgee.service.apps

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.hibernate.Session
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Replays the app-layer backfill against a database that already holds installs, their secrets,
 * their organization availability and their project enablements — the case a fresh install never
 * exercises and the one that would silently break every app already registered.
 *
 * The tables are cloned from the live ones so their shape cannot drift from production's, and the
 * backfill statements are read out of `schema.xml` itself rather than restated here, so a change to
 * the changeset is a change to what this test runs.
 */
class AppLayerBackfillMigrationTest : AbstractSpringTest() {
  @AfterEach
  fun cleanup() {
    execute("DROP SCHEMA IF EXISTS $SCRATCH_SCHEMA CASCADE")
  }

  @Test
  fun `backfills one app per app id and leaves everything hanging off the installs intact`() {
    givenPreAppLayerDatabase()

    applyBackfill()

    val apps = query("select app_id, organization_id from app order by app_id")
    apps.assert.hasSize(2)
    apps[0].assert.isEqualTo(listOf("native-app", null))
    apps[1].assert.isEqualTo(listOf("shared-app", ORG_ONE))

    installOwnership().assert.isEqualTo(
      listOf(
        listOf(INSTALL_ONE, "shared-app"),
        listOf(INSTALL_TWO, "shared-app"),
        listOf(INSTALL_NATIVE, "native-app"),
      ),
    )

    single("select count(*) from app_install_secret").assert.isEqualTo(2L)
    single("select count(*) from app_available_for_organization").assert.isEqualTo(1L)
    single("select count(*) from app_enabled_for_project").assert.isEqualTo(1L)
    single("select secret_hash from app_install_secret order by id limit 1").assert.isEqualTo("hash-one")
  }

  @Test
  fun `re-running the backfill changes nothing`() {
    givenPreAppLayerDatabase()
    applyBackfill()
    val appsAfterFirstRun = query("select id, app_id, organization_id from app order by app_id")

    applyBackfill()

    query("select id, app_id, organization_id from app order by app_id").assert.isEqualTo(appsAfterFirstRun)
  }

  /**
   * The organization of the *oldest* install owns an app id two organizations installed
   * independently: it is unique server-wide from now on, so only one of them can hold it.
   */
  private fun installOwnership(): List<List<Any?>> {
    return query(
      """
      select i.id, a.app_id
      from app_install i join app a on a.id = i.registered_app_id
      order by i.id
      """,
    )
  }

  private fun givenPreAppLayerDatabase() {
    execute("DROP SCHEMA IF EXISTS $SCRATCH_SCHEMA CASCADE")
    execute("CREATE SCHEMA $SCRATCH_SCHEMA")
    listOf("app", "app_install", "app_install_secret", "app_available_for_organization", "app_enabled_for_project")
      .forEach {
        execute("CREATE TABLE $SCRATCH_SCHEMA.$it (LIKE public.$it INCLUDING ALL)")
      }
    execute("ALTER TABLE $SCRATCH_SCHEMA.app_install DROP COLUMN registered_app_id")

    insertInstall(INSTALL_ONE, ORG_ONE, "shared-app", "2020-01-01")
    insertInstall(INSTALL_TWO, ORG_TWO, "shared-app", "2021-01-01")
    insertInstall(INSTALL_NATIVE, null, "native-app", "2022-01-01")

    insertSecret(1001, INSTALL_ONE, "hash-one")
    insertSecret(1002, INSTALL_TWO, "hash-two")
    execute(
      "insert into $SCRATCH_SCHEMA.app_available_for_organization " +
        "(id, created_at, updated_at, app_install_id, organization_id, author_id) " +
        "values (2001, now(), now(), $INSTALL_NATIVE, $ORG_ONE, $AUTHOR)",
    )
    execute(
      "insert into $SCRATCH_SCHEMA.app_enabled_for_project " +
        "(id, created_at, updated_at, app_install_id, project_id, author_id) " +
        "values (3001, now(), now(), $INSTALL_ONE, 42, $AUTHOR)",
    )
  }

  private fun insertInstall(
    id: Long,
    organizationId: Long?,
    appId: String,
    createdAt: String,
  ) {
    execute(
      "insert into $SCRATCH_SCHEMA.app_install " +
        "(id, created_at, updated_at, organization_id, author_id, manifest_url, app_id, name, version, " +
        "base_url, manifest_json, available_to_all_organizations) " +
        "values ($id, '$createdAt', '$createdAt', ${organizationId ?: "null"}, $AUTHOR, " +
        "'https://example.com/$appId.json', '$appId', 'App', '1.0.0', 'https://example.com', '{}', false)",
    )
  }

  private fun insertSecret(
    id: Long,
    installId: Long,
    hash: String,
  ) {
    execute(
      "insert into $SCRATCH_SCHEMA.app_install_secret " +
        "(id, created_at, updated_at, app_install_id, secret_hash, secret_prefix) " +
        "values ($id, now(), now(), $installId, '$hash', 'tgapps_xxx')",
    )
  }

  private fun applyBackfill() {
    execute("ALTER TABLE $SCRATCH_SCHEMA.app_install ADD COLUMN IF NOT EXISTS registered_app_id BIGINT")
    BACKFILL_CHANGESET_IDS.forEach { execute(changesetSql(it)) }
  }

  private fun changesetSql(changesetId: String): String {
    val document =
      DocumentBuilderFactory
        .newInstance()
        .also { it.isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(javaClass.classLoader.getResourceAsStream(CHANGELOG_RESOURCE))
    val changeSets = document.getElementsByTagNameNS("*", "changeSet")
    for (index in 0 until changeSets.length) {
      val changeSet = changeSets.item(index) as Element
      if (changeSet.getAttribute("id") != changesetId) continue
      return (changeSet.getElementsByTagNameNS("*", "sql").item(0) as Element).textContent
    }
    throw IllegalStateException("changeset $changesetId not found in $CHANGELOG_RESOURCE")
  }

  private fun execute(sql: String) {
    executeInNewTransaction(platformTransactionManager) {
      entityManager.unwrap(Session::class.java).doWork { connection ->
        connection.createStatement().use { statement ->
          statement.execute("SET LOCAL search_path TO $SCRATCH_SCHEMA, public")
          statement.execute(sql)
        }
      }
    }
  }

  private fun query(sql: String): List<List<Any?>> {
    return executeInNewTransaction(platformTransactionManager) {
      val rows = mutableListOf<List<Any?>>()
      entityManager.unwrap(Session::class.java).doWork { connection ->
        connection.createStatement().use { statement ->
          statement.execute("SET LOCAL search_path TO $SCRATCH_SCHEMA, public")
          statement.executeQuery(sql).use { resultSet ->
            val columnCount = resultSet.metaData.columnCount
            while (resultSet.next()) {
              rows.add((1..columnCount).map { resultSet.getObject(it) })
            }
          }
        }
      }
      rows
    }
  }

  private fun single(sql: String): Any? {
    return query(sql).single().single()
  }

  companion object {
    private const val SCRATCH_SCHEMA = "app_layer_backfill_test"
    private const val CHANGELOG_RESOURCE = "db/changelog/schema.xml"
    private val BACKFILL_CHANGESET_IDS = listOf("1783771000000-44", "1783771000000-45")

    private const val AUTHOR = 7L
    private const val ORG_ONE = 11L
    private const val ORG_TWO = 12L
    private const val INSTALL_ONE = 101L
    private const val INSTALL_TWO = 102L
    private const val INSTALL_NATIVE = 103L
  }
}
