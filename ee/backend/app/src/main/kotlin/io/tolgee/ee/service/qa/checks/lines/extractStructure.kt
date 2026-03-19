package io.tolgee.ee.service.qa.checks.lines

/**
 * Extracts the structure from text based on empty lines.
 *
 * Classifies each line as empty (blank) or content.
 * Consecutive content lines collapse into one content block.
 * Each empty line is counted individually.
 *
 * Returns a list of gaps with empty line counts and text positions.
 */
fun extractStructure(text: String): Structure {
  val lines = splitLines(text)
  val separatorType = detectSeparator(lines)
  var currentGaps = 0
  var inContent = false
  val gaps = mutableListOf<Gap>()

  for ((line, offset, _) in lines) {
    if (line.isBlank()) {
      // Empty line
      inContent = false
      currentGaps++
      continue
    }

    if (inContent) {
      // Inner content line
      continue
    }

    // First content line
    gaps.add(Gap(lineCount = currentGaps, endIndex = offset))
    currentGaps = 0
    inContent = true
  }

  gaps.add(Gap(lineCount = currentGaps, endIndex = text.length))

  return Structure(gaps, separatorType)
}
