export class E2ProjectMembersInvitationDialog {
  getEmailField() {
    return cy.gcy('invitation-dialog-input-field');
  }

  typeEmail(email: string) {
    this.getEmailField().type(email);
  }

  clickInvite() {
    cy.gcy('invitation-dialog-invite-button').click();
  }

  fillAndInvite(email: string) {
    this.typeEmail(email);
    this.clickInvite();
  }
}
