package io.tolgee.service.dataImport

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Service

@Service
class ImportDeleteService(
  private val entityManager: EntityManager,
) {
  fun deleteImport(importId: Long) {
    entityManager.unwrap(Session::class.java).doWork { connection ->
      deleteImportTranslations(connection, importId)
      deleteImportLanguages(connection, importId)
      deleteImportKeyMetaTags(connection, importId)
      deleteImportKeyMetaComments(connection, importId)
      deleteImportKeyMetaCodeReferences(connection, importId)
      deleteImportKeyMeta(connection, importId)
      deleteImportKeys(connection, importId)
      deleteImportFileIssueParams(connection, importId)
      deleteImportFileIssues(connection, importId)
      deleteImportFiles(connection, importId)
      deleteTheImport(connection, importId)
    }
  }

  fun executeUpdate(
    connection: java.sql.Connection,
    query: String,
    importId: Long,
  ) {
    @Suppress("SqlSourceToSinkFlow")
    connection.prepareStatement(query).use { statement ->
      statement.setLong(1, importId)
      statement.executeUpdate()
    }
  }

  fun deleteImportTranslations(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_translation " +
        "where id in (" +
        "select it.id from import_translation it " +
        "join import_language il on il.id = it.language_id " +
        "join import_file if on il.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportLanguages(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_language " +
        "where id in (select il.id from import_language il " +
        "join import_file if on il.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeys(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_key " +
        "where id in (select ik.id from import_key ik " +
        "join import_file if on ik.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaTags(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_meta_tags " +
        "where key_metas_id in (select ikm.id from key_meta ikm " +
        "join import_key ik on ikm.import_key_id = ik.id " +
        "join import_file if on ik.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaComments(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_comment " +
        "where key_meta_id in (select ikm.id from key_meta ikm " +
        "join import_key ik on ikm.import_key_id = ik.id " +
        "join import_file if on ik.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaCodeReferences(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_code_reference " +
        "where key_meta_id in (select ikm.id from key_meta ikm " +
        "join import_key ik on ikm.import_key_id = ik.id " +
        "join import_file if on ik.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMeta(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_meta " +
        "where id in (select ikm.id from key_meta ikm " +
        "join import_key ik on ikm.import_key_id = ik.id " +
        "join import_file if on ik.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFileIssueParams(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file_issue_param " +
        "where import_file_issue_param.issue_id in (select ifi.id from import_file_issue ifi " +
        "join import_file if on ifi.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFileIssues(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file_issue " +
        "where id in (select ifi.id from import_file_issue ifi " +
        "join import_file if on ifi.file_id = if.id where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFiles(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file " +
        "where id in (select if.id from import_file if where if.import_id = ?)"
    executeUpdate(connection, query, importId)
  }

  fun deleteTheImport(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query = "delete from import where id = ?"
    executeUpdate(connection, query, importId)
  }
}
