package io.tolgee.dialects.postgres

import org.hibernate.dialect.DatabaseVersion
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.query.spi.QueryEngine
import org.hibernate.query.sqm.NullOrdering
import org.hibernate.query.sqm.function.SqmFunctionRegistry
import org.hibernate.type.BasicTypeRegistry
import org.hibernate.type.StandardBasicTypes

@Suppress("unused")
class CustomPostgreSQLDialect : PostgreSQLDialect(DatabaseVersion.make(10, 0)) {
  override fun getNullOrdering(): NullOrdering {
    return NullOrdering.FIRST
  }

  override fun initializeFunctionRegistry(queryEngine: QueryEngine) {
    val basicTypeRegistry: BasicTypeRegistry = queryEngine.typeConfiguration.getBasicTypeRegistry()
    val functionRegistry: SqmFunctionRegistry = queryEngine.sqmFunctionRegistry
    functionRegistry.registerPattern(
      "similarity",
      "similarity(?1, ?2)",
      basicTypeRegistry.resolve(StandardBasicTypes.FLOAT)
    )
  }
}
