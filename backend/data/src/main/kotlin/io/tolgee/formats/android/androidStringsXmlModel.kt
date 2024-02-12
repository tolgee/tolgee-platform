package io.tolgee.formats.android

class StringsModel {
  val strings = mutableListOf<StringUnit>()
  val stringArrays = mutableListOf<StringArrayUnit>()
  val plurals = mutableListOf<PluralsUnit>()
}

class StringUnit {
  var name: String? = null
  var value: String? = null
}

class StringArrayUnit {
  var name: String? = null
  val items = mutableListOf<String>()
}

class PluralsUnit {
  var name: String? = null
  val items = mutableMapOf<String, String>()
}
