package io.tolgee.util

import java.net.URI

fun urlHostOnly(url: Any?): String? = (url as? String)?.let { runCatching { URI(it).host }.getOrNull() }
