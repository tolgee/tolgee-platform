import { HOST } from '../common/constants';
import { waitForGlobalLoading } from '../common/loading';
import { confirmStandard } from '../common/shared';

export class E2TrashSection {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/translations/trash`);
    waitForGlobalLoading();
    return this;
  }

  getTrashRows() {
    return cy.gcy('trash-row');
  }

  assertTrashRowCount(count: number) {
    this.getTrashRows().should('have.length', count);
    return this;
  }

  assertTrashEmpty() {
    cy.gcy('trash-row').should('not.exist');
    return this;
  }

  restoreKey(keyName: string) {
    this.getTrashRows()
      .contains(keyName)
      .should('be.visible')
      .closestDcy('trash-row')
      .findDcy('trash-restore-button')
      .click();
    waitForGlobalLoading();
    return this;
  }

  permanentlyDeleteKey(keyName: string) {
    this.getTrashRows()
      .contains(keyName)
      .should('be.visible')
      .closestDcy('trash-row')
      .findDcy('trash-permanent-delete-button')
      .click();
    confirmStandard();
    waitForGlobalLoading();
    return this;
  }
}
