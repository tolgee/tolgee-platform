package io.tolgee.model.views

import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.BigMeta

interface BigMetaView {
  val bigMeta: BigMeta
  val key: Key?
}
