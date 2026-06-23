package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.machine-translation.baidu",
  description =
    "See [Baidu's](https://fanyi-api.baidu.com/product/11) page (in Chinese) " +
      "for more information and applicable pricing.",
  displayName = "Baidu Translate",
)
open class BaiduMachineTranslationProperties(
  @DocProperty(description = "Whether Baidu-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true,
  @DocProperty(description = "Whether to use Baidu Translate as a primary translation engine.")
  override var defaultPrimary: Boolean = false,
  @DocProperty(description = "Baidu Translate App ID.")
  var appId: String? = null,
  @DocProperty(description = "Baidu Translate Secret key.")
  var appSecret: String? = null,
  @DocProperty(
    description =
      "Whether the resulting translation should be changed according to the user-defined dictionary.\n" +
        "The dictionary used can be modified at " +
        "[Manage Terms](https://fanyi-api.baidu.com/manage/term) (login required).",
  )
  var action: Boolean = false,
) : MachineTranslationServiceProperties
