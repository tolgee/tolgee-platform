package io.tolgee.dialects.postgres

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.dialect.DatabaseVersion
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.type.StandardBasicTypes

@Suppress("unused")
class CustomPostgreSQLDialect : PostgreSQLDialect(DatabaseVersion.make(13)) {

  override fun contributeFunctions(functionContributions: FunctionContributions) {
    super.contributeFunctions(functionContributions)
    functionContributions.functionRegistry.registerPattern(
      "similarity",
      "similarity(?1, ?2)",
      functionContributions.typeConfiguration.basicTypeRegistry.resolve(StandardBasicTypes.FLOAT)
    )
  }
}
