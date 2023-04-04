package io.tolgee.util

fun getSafeNamespace(name: String?) = if (name.isNullOrBlank()) null else name
