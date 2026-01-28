package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.machine-translation.aws",
  description =
    "See [AWS's Amazon Translate](https://aws.amazon.com/translate) page " +
      "for more information and applicable pricing.",
  displayName = "AWS Amazon Translate",
)
open class AwsMachineTranslationProperties(
  @DocProperty(description = "Whether AWS-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true,
  @DocProperty(description = "Whether to use AWS Amazon Translate as a primary translation engine.")
  override var defaultPrimary: Boolean = false,
  @DocProperty(
    description =
      "If you are authenticating using a different method than " +
        "explicit access key and secret key, which implicitly enable AWS Translate, " +
        "you should enable AWS Translate using this option.",
  )
  var enabled: Boolean? = null,
  @DocProperty(
    description =
      "AWS access key. (optional if you are authenticating " +
        "with a different method, like STS Web Identity)",
  )
  var accessKey: String? = null,
  @DocProperty(
    description =
      "AWS secret key. (optional if you are authenticating " +
        "with a different method, like STS Web Identity)",
  )
  var secretKey: String? = null,
  @DocProperty(description = "AWS region.")
  var region: String? = "eu-central-1",
) : MachineTranslationServiceProperties
