package io.tolgee.component.machineTranslation.metadata

import io.tolgee.model.keyBigMeta.BigMetaType

data class BigMetaItem(
  var namespace: String? = null,

  var keyName: String,

  var location: String? = null,

  var contextData: Any? = null,

  var type: BigMetaType = BigMetaType.SCRAPE
)
