package io.tolgee.ee.api.v2.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.model.PlanPricesModel
import io.tolgee.ee.api.v2.hateoas.model.SelfHostedEePlanModel
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SelfHostedEePlanModelTest {
  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `parses plan response with unknown features`() {
    val parsed =
      objectMapper.readValue<SelfHostedEePlanModel>(
        """
        {
        "enabledFeatures": ["I've made this up", "ASSISTED_UPDATES"],
        "prices": $pricesJson,
        "free": false,
        "nonCommercial": false
        }
        """.trimIndent(),
      )

    parsed.enabledFeatures.toList().assert.containsExactly(Feature.ASSISTED_UPDATES)
  }

  val pricesJson: String
    get() {
      return objectMapper.writeValueAsString(PlanPricesModel())
    }
}
