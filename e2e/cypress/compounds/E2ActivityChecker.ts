import { gcy, selectInProjectMenu } from '../common/shared';

export class E2ActivityChecker {
  checkActivity(activityText: string) {
    selectInProjectMenu('Project Dashboard');
    cy.waitForDom();

    gcy('activity-compact').contains(activityText).should('be.visible');

    gcy('activity-compact')
      .contains(activityText)
      .closestDcy('activity-compact')
      .findDcy('activity-compact-detail-button')
      .click({ force: true });

    return this;
  }

  assertActivityDetails(expectedTexts: string[]) {
    gcy('activity-detail-dialog').within(() => {
      expectedTexts.forEach((text) => {
        cy.contains(text).should('be.visible');
      });
    });
    return this;
  }
}
