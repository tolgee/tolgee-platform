package io.tolgee.component.email

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.Invitation
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

@Component
class InvitationEmailSender(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun sendInvitation(invitation: Invitation) {
    val email = invitation.email
    if (email.isNullOrBlank()) {
      return
    }
    val url = getInvitationAcceptUrl(invitation.code)
    val params =
      EmailParams(
        to = email,
        subject = "Invitation to Tolgee",
        text =
          """
          Good news. ${getInvitationSentence(invitation)}<br/><br/>
          
          To accept the invitation, <b>follow this link</b>:<br/>
          <a href="$url">$url</a><br/><br/>
          """.trimIndent(),
      )
    tolgeeEmailSender.sendEmail(params)
  }

  private fun getInvitationSentence(invitation: Invitation): Any {
    val projectNameOrNull = invitation.permission?.project?.name
    val organizationNameOrNull = invitation.organizationRole?.organization?.name
    val toWhat =
      when {
        projectNameOrNull != null -> "project"
        organizationNameOrNull != null -> "organization"
        else -> throw IllegalStateException("No organization or project!")
      }

    val name =
      projectNameOrNull ?: organizationNameOrNull
        ?: throw IllegalStateException("Both the organization and the project are null??")

    val escapedName = HtmlUtils.htmlEscape(name)
    return "You have been invited to $toWhat $escapedName in Tolgee."
  }

  fun getInvitationAcceptUrl(code: String): String {
    return "${frontendUrlProvider.url}/accept_invitation/$code"
  }
}
