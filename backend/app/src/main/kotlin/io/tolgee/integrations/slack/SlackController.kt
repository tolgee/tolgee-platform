package io.tolgee.integrations.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.servlet.SlackAppServlet
import javax.servlet.annotation.WebServlet

@WebServlet("/slack/events")
class SlackController(app: App) : SlackAppServlet(app)
