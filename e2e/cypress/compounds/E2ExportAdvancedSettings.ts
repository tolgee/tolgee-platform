const TOGGLE: DataCy.Value = 'export-advanced-settings-toggle';

export class E2ExportAdvancedSettings {
  getToggle() {
    return cy.gcy(TOGGLE);
  }

  getContent() {
    return cy.gcy('export-advanced-settings-content');
  }

  expand() {
    cy.gcy('export-advanced-settings').then(($section) => {
      const toggle = $section.find(`[data-cy="${TOGGLE}"]`);
      if (toggle.length && toggle.attr('aria-expanded') !== 'true') {
        cy.wrap(toggle).click();
      }
    });
    this.getContent().should('be.visible');
  }

  collapse() {
    this.getToggle().should('have.attr', 'aria-expanded', 'true').click();
    this.getContent().should('not.exist');
  }

  shouldBeLockedOpen() {
    this.getContent().should('be.visible');
    this.getToggle().should('not.exist');
  }
}

export const expandExportAdvancedSettings = () =>
  new E2ExportAdvancedSettings().expand();
