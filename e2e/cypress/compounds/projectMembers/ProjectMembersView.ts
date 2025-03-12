import { HOST } from '../../common/constants';
import { ProjectMembersInvitationDialog } from './ProjectMembersInvitationDialog';

export class ProjectMembersView {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/permissions`);
  }

  openInvitationDialog() {
    cy.gcy('invite-generate-button').click();
    return new ProjectMembersInvitationDialog();
  }
}
