package io.tolgee.model.views

import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.KeysDistance

interface BigMetaView {
  val keysDistance: KeysDistance
  val key: Key?
}
