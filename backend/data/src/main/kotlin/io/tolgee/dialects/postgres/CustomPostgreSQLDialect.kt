package io.tolgee.dialects.postgres

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.NullPrecedence
import org.hibernate.dialect.PostgreSQL10Dialect
import org.hibernate.dialect.function.SQLFunction
import org.hibernate.engine.spi.Mapping
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.type.FloatType
import org.hibernate.type.Type
import java.sql.Types

@Suppress("unused")
class CustomPostgreSQLDialect : PostgreSQL10Dialect() {

  override fun renderOrderByElement(
    expression: String?,
    collation: String?,
    order: String?,
    nulls: NullPrecedence?
  ): String {
    if (nulls == NullPrecedence.NONE) {
      if (order == "asc") {
        return super.renderOrderByElement(expression, collation, order, NullPrecedence.FIRST)
      }
      if (order == "desc") {
        return super.renderOrderByElement(expression, collation, order, NullPrecedence.LAST)
      }
    }
    return super.renderOrderByElement(expression, collation, order, nulls)
  }

  init {
    registerFunction(
      "similarity",
      object : SQLFunction {
        override fun hasArguments(): Boolean = true

        override fun hasParenthesesIfNoArguments() = false

        override fun getReturnType(firstArgumentType: Type?, mapping: Mapping?) = FloatType()

        override fun render(
          firstArgumentType: Type,
          arguments: MutableList<Any?>,
          factory: SessionFactoryImplementor
        ): String {
          return "similarity(${arguments[0]}, ${arguments[1]})"
        }
      }
    )
    registerHibernateType(Types.OTHER, JsonBinaryType::class.java.name)
  }
}
