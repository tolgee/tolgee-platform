package io.tolgee.formats.xliff.model

class XliffModel {
  val files = mutableListOf<XliffFile>()
  var version: String? = null
}
