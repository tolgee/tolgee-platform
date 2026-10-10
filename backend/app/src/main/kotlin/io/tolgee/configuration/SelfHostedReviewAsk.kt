package io.tolgee.configuration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.InstanceIdService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.ansi.AnsiColor
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.PrintStream
import java.time.Duration

@Component
class SelfHostedReviewAsk(
  private val tolgeeProperties: TolgeeProperties,
  private val publicBillingConfProvider: PublicBillingConfProvider,
  private val environment: Environment,
  private val instanceIdService: InstanceIdService,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    try {
      printIfEnabled(System.out)
    } catch (e: Exception) {
      logger.debug("Could not print the review ask", e)
    }
  }

  fun printIfEnabled(out: PrintStream) {
    if (!tolgeeProperties.reviewAsk.enabled) return
    if (publicBillingConfProvider().enabled) return
    if (isBannerOff()) return
    if (isInstanceTooNew()) return
    out.println(render(glyphsFor(out)))
  }

  private fun isBannerOff(): Boolean {
    val bannerMode = environment.getProperty("spring.main.banner-mode")?.lowercase()
    return bannerMode == "off" || bannerMode == UNQUOTED_YAML_OFF
  }

  private fun isInstanceTooNew(): Boolean {
    val createdAt = instanceIdService.getInstanceCreatedAt() ?: return true
    val age = Duration.between(createdAt.toInstant(), currentDateProvider.date.toInstant())
    return age < MIN_INSTANCE_AGE
  }

  private fun glyphsFor(out: PrintStream): Glyphs {
    if (out.charset().newEncoder().canEncode(UNICODE_GLYPHS.all)) return UNICODE_GLYPHS
    return ASCII_GLYPHS
  }

  private fun render(glyphs: Glyphs): String {
    return buildString {
      appendLine()
      appendLine("  " + AnsiOutput.toString(AnsiColor.GREEN, "${glyphs.check} Tolgee is ready"))
      appendLine()
      besideMouse(note(glyphs), glyphs).forEach { appendLine(it) }
      appendLine()
      appendLine(
        "  " + AnsiOutput.toString(AnsiColor.BRIGHT_BLACK, "Hide this message: TOLGEE_REVIEW_ASK_ENABLED=false"),
      )
    }
  }

  private fun note(glyphs: Glyphs): List<Line> =
    listOf(
      Line("Hi, I'm Jan, the founder of Tolgee.${glyphs.wave}"),
      Line(""),
      Line("For more than 5 years we've been building Tolgee in the open and giving"),
      Line("it to teams like yours for free, so they can translate their software"),
      Line("the best way possible."),
      Line(""),
      Line("Could you do one thing for me? If Tolgee helps you, please leave it a"),
      Line("short review on G2. It takes about 6 minutes and it really helps us get"),
      Line("noticed, grow, and make Tolgee even more awesome."),
      Line(""),
      Line(G2_REVIEW_URL, AnsiColor.BLUE),
      Line(""),
      Line("Thanks a lot ${glyphs.heart}"),
      Line("Jan", AnsiColor.BRIGHT_BLACK),
    )

  private fun besideMouse(
    lines: List<Line>,
    glyphs: Glyphs,
  ): List<String> {
    val rule = AnsiOutput.toString(AnsiColor.MAGENTA, glyphs.rule)
    return lines.mapIndexed { index, line ->
      val mouseLine = AnsiOutput.toString(AnsiColor.MAGENTA, MOUSE.getOrElse(index) { "" }.padEnd(MOUSE_WIDTH))
      val text = line.color?.let { AnsiOutput.toString(it, line.text) } ?: line.text
      "  $mouseLine  $rule  $text".trimEnd()
    }
  }

  private data class Line(
    val text: String,
    val color: AnsiColor? = null,
  )

  private data class Glyphs(
    val rule: String,
    val check: String,
    val heart: String,
    val wave: String,
  ) {
    val all = rule + check + heart + wave
  }

  companion object {
    const val G2_REVIEW_URL = "https://www.g2.com/products/tolgee/reviews"

    private const val UNQUOTED_YAML_OFF = "false"
    private val MIN_INSTANCE_AGE: Duration = Duration.ofDays(5)

    private val UNICODE_GLYPHS = Glyphs(rule = "│", check = "✔", heart = "❤", wave = " 👋")
    private val ASCII_GLYPHS = Glyphs(rule = "|", check = "*", heart = "<3", wave = "")

    private val MOUSE =
      listOf(
        " ,-.   ,-. ",
        "(   \\_/   )",
        " \\  o o  / ",
        " =\\  Y  /= ",
        "   `---'   ",
      )
    private val MOUSE_WIDTH = MOUSE.maxOf { it.length }
  }
}
