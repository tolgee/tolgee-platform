import { HOST } from '../../common/constants';
import { gcyAdvanced } from '../../common/shared';
import { E2ProjectMembersInvitationDialog } from './E2ProjectMembersInvitationDialog';

export class E2ProjectMembersView {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/manage/permissions`);
  }

  openInvitationDialog() {
    cy.gcy('invite-generate-button').click();
    return new E2ProjectMembersInvitationDialog();
  }

  openTeamTab() {
    cy.gcy('project-members-tab-team').click();
  }

  openCommunityTab() {
    cy.gcy('project-members-tab-community').click();
  }

  getTeamTab() {
    return cy.gcy('project-members-tab-team');
  }

  getCommunityTab() {
    return cy.gcy('project-members-tab-community');
  }

  getContributor(name: string) {
    return gcyAdvanced({ value: 'project-contributor-item', name });
  }

  getContributors() {
    return cy.gcy('project-contributor-item');
  }

  getMembers() {
    return cy.gcy('project-member-item');
  }
}
