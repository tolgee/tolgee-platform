import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';

export class E2BranchesSection {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/branches`);
  }

  getAddButton() {
    return gcy('project-settings-branches-add');
  }

  getBranchItem(name: string) {
    return gcy('project-settings-branch-item-name')
      .contains(name)
      .scrollIntoView()
      .should('be.visible')
      .closestDcy('project-settings-branch-item');
  }

  openBranchActions(name: string) {
    this.getBranchItem(name)
      .findDcy('project-settings-branches-actions-menu')
      .click();
  }

  createBranch(name: string) {
    this.getAddButton().click();
    gcy('branch-name-input').find('input').type(name);
    gcy('global-form-save-button').click();
  }

  renameBranch(currentName: string, newName: string) {
    this.openBranchActions(currentName);
    gcy('project-settings-branches-rename-button').should('be.visible').click();
    gcy('branch-name-input').find('input').clear().type(newName);
    gcy('global-form-save-button').click();
  }

  deleteBranch(name: string) {
    this.openBranchActions(name);
    gcy('project-settings-branches-remove-button').click();
    gcy('global-confirmation-confirm').click();
  }

  protectBranch(name: string) {
    this.openBranchActions(name);
    gcy('project-settings-branches-protection-button')
      .should('have.attr', 'data-cy-protect', 'true')
      .click();
    gcy('global-confirmation-confirm').click();
  }

  unprotectBranch(name: string) {
    this.openBranchActions(name);
    gcy('project-settings-branches-protection-button')
      .should('have.attr', 'data-cy-protect', 'false')
      .click();
    gcy('global-confirmation-confirm').click();
  }

  assertBranchExists(name: string) {
    gcy('project-settings-branch-item-name')
      .contains(name)
      .should('be.visible');
  }

  assertBranchNotExists(name: string) {
    gcy('project-settings-branch-item-name').contains(name).should('not.exist');
  }

  assertBranchIsProtected(name: string) {
    this.getBranchItem(name).findDcy('branch-protected-icon').should('exist');
  }

  assertBranchIsNotProtected(name: string) {
    this.getBranchItem(name)
      .findDcy('branch-protected-icon')
      .should('not.exist');
  }

  assertBranchIsDefault(name: string) {
    this.getBranchItem(name).findDcy('branch-default-chip').should('exist');
  }

  assertDeleteButtonNotInMenu() {
    gcy('project-settings-branches-remove-button').should('not.exist');
  }

  assertCreateButtonNotVisible() {
    gcy('project-settings-branches-add').should('not.exist');
  }

  assertActionsMenuNotVisible(name: string) {
    this.getBranchItem(name)
      .findDcy('project-settings-branches-actions-menu')
      .should('not.exist');
  }
}
