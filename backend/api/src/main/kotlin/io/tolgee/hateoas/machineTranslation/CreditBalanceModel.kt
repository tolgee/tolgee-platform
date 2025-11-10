package io.tolgee.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

@Suppress("unused")
class CreditBalanceModel(
  val creditBalance: Long,
  val bucketSize: Long,
) : RepresentationModel<CreditBalanceModel>(),
  Serializable {
  @Schema(
    deprecated = true,
    description =
      "Customers were able to buy extra credits separately in the past.\n\n" +
        "This option is not available anymore and this field is kept only for " +
        "backward compatibility purposes and is always 0.",
  )
  val extraCreditBalance: Long = 0
}
