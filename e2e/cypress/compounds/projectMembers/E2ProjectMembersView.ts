import { HOST } from '../../common/constants';
import { E2ProjectMembersInvitationDialog } from './E2ProjectMembersInvitationDialog';

export class E2ProjectMembersView {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/permissions`);
  }

  openInvitationDialog() {
    cy.gcy('invite-generate-button').click();
    return new E2ProjectMembersInvitationDialog();
  }
}
