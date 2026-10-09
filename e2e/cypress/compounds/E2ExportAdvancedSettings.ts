import { gcyAdvanced } from '../common/shared';

type SummaryItemState = 'default' | 'changed' | 'invalid';

export class E2ExportAdvancedSettings {
  getToggle() {
    return cy.gcy('export-advanced-settings-toggle');
  }

  getContent() {
    return cy.gcy('export-advanced-settings-content');
  }

  getSummary() {
    return cy.gcy('export-advanced-settings-summary');
  }

  getSummaryItems(state: SummaryItemState) {
    return gcyAdvanced({
      value: 'export-advanced-settings-summary-item',
      state,
    });
  }

  expand() {
    this.getToggle().then(($toggle) => {
      if ($toggle.attr('aria-expanded') !== 'true') {
        cy.wrap($toggle).click();
      }
    });
    this.getContent().should('be.visible');
  }

  collapse() {
    this.getToggle().should('have.attr', 'aria-expanded', 'true').click();
    this.getContent().should('not.exist');
  }
}

export const expandExportAdvancedSettings = () =>
  new E2ExportAdvancedSettings().expand();
