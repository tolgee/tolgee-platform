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

  getContributorFirstContributions() {
    return cy.gcy('project-contributor-item-first-contribution');
  }

  getContributorLastContributions() {
    return cy.gcy('project-contributor-item-last-contribution');
  }

  getContributorInviteButtons() {
    return cy.gcy('project-contributor-invite-button');
  }

  getContributorInviteButton(name: string) {
    return this.getContributor(name).findDcy(
      'project-contributor-invite-button'
    );
  }

  getContributorInvitationPendings() {
    return cy.gcy('project-contributor-invitation-pending');
  }

  getContributorInvitationPending(name: string) {
    return this.getContributor(name).findDcy(
      'project-contributor-invitation-pending'
    );
  }

  inviteContributor(name: string) {
    this.getContributorInviteButton(name).click();
    return new E2ProjectMembersInvitationDialog();
  }

  getInvitations() {
    return cy.gcy('project-members-invitation-item');
  }

  cancelInvitation() {
    this.getInvitations()
      .findDcy('project-members-invitation-cancel-button')
      .click();
  }
}
