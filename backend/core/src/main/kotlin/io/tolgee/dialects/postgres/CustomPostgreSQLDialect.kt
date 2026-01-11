package io.tolgee.dialects.postgres

import org.hibernate.dialect.DatabaseVersion
import org.hibernate.dialect.PostgreSQLDialect

/**
 * This class was historicaly used to provide additional functions to the HQL.
 * It seemed this class could be removed, but when running Tolgee with Postgres >= 15, it
 * started to fail due to:
 *
 * https://hibernate.atlassian.net/browse/HHH-17588
 *
 * When this bug is fixed in Hibernate, this class can be probably removed.
 *
 * This class only forces PostgreSQLDialect to use version 13.
 */
@Suppress("unused")
class CustomPostgreSQLDialect : PostgreSQLDialect(DatabaseVersion.make(13))
