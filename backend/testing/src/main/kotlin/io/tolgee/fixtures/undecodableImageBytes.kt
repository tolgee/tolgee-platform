package io.tolgee.fixtures

/**
 * Bytes with no recognized image magic header, so **no** registered `ImageIO` reader claims them and
 * `ImageIO.read` returns `null` (verified — a recognized-but-truncated header like BMP's `0x42 0x4D`
 * instead makes a reader throw, exercising a different branch). Shared between the `:data`
 * `ImageProcessor` unit test and the `:server-app` public-upload HTTP test so both exercise the same
 * proven null-returning payload.
 */
val undecodableImageBytes: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
