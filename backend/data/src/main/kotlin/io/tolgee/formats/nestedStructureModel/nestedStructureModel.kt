package io.tolgee.formats.nestedStructureModel

interface Item

class ValueItem(val value: String?) : Item

class RootItem(var value: Item? = null) : Item

class Object : LinkedHashMap<String, Item>(), Item

class Array : LinkedHashMap<Int, Item>(), Item
