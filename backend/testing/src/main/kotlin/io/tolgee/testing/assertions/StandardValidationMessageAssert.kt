package io.tolgee.testing.assertions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.IntegerAssert
import org.assertj.core.api.StringAssert

class StandardValidationMessageAssert(
  val data: Map<String, String>,
) : AbstractAssert<StandardValidationMessageAssert?, Map<String, String>>(
    data,
    StandardValidationMessageAssert::class.java,
  ) {
  fun onField(field: String?): StringAssert {
    if (!actual!!.containsKey(field)) {
      jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data).let {
        failWithMessage(
          """
                    |Data:
                    |
                    |$it
                    |
                    |Error is not on field %s.
          """.trimMargin(),
          field,
        )
      }
    }
    return StringAssert(actual[field]).describedAs("Message assertion on field %s", field)
  }

  fun errorCount(): IntegerAssert {
    return IntegerAssert(actual!!.size)
  }
}
