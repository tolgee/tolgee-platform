import { login } from '../../../common/apiCalls/common';
import { HOST } from '../../../common/constants';
import { exportTagFilterTestData } from '../../../common/apiCalls/testData/testData';
import { getFileName, visitExport } from '../../../common/export';
import { dismissMenu, gcyAdvanced } from '../../../common/shared';
import { E2ExportTagFilters } from '../../../compounds/E2ExportTagFilters';
import { expandExportAdvancedSettings } from '../../../compounds/E2ExportAdvancedSettings';

const PROJECT_NAME = 'Export tag filter project';
const NAMESPACES = ['', 'ns-1', 'ns-2'];

describe('Export tag and namespace filtering', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  const tagFilters = new E2ExportTagFilters();

  beforeEach(() => {
    exportTagFilterTestData.clean();
    exportTagFilterTestData.generateStandard().then((response) => {
      login('export_tag_filter_user');
      visitExport(response.body.projects[0].id);
      expandExportAdvancedSettings();
    });
  });

  afterEach(() => {
    exportTagFilterTestData.clean();
  });

  it('exports only keys matching included and not excluded tags', () => {
    selectOnlyNamespace('');
    tagFilters.addIncludedTag('included');
    tagFilters.addExcludedTag('excluded');

    cy.gcy('export-submit-button').click();

    readExportedFile().should('deep.equal', {
      'default-included': 'default-included text',
    });
  });

  it('supports wildcard tags', () => {
    selectOnlyNamespace('');
    tagFilters.addIncludedTag('feature-*');

    cy.gcy('export-submit-button').click();

    readExportedFile().should('deep.equal', {
      'default-feature': 'default-feature text',
    });
  });

  it('combines tag and namespace filters', () => {
    selectOnlyNamespace('ns-2');
    tagFilters.addExcludedTag('excluded');

    cy.intercept('POST', '/v2/projects/*/export').as('export');
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should('deep.include', {
        filterNamespace: ['ns-2'],
        filterTagNotIn: ['excluded'],
      });

    readExportedFile().should('deep.equal', {
      'ns2-included': 'ns2-included text',
      'ns2-untagged': 'ns2-untagged text',
    });
  });

  it('applies tag filters from a link', () => {
    cy.location('pathname').then((pathname) => {
      cy.visit(`${HOST}${pathname}?tagsIn=included&tagsNotIn=excluded`);
    });
    expandExportAdvancedSettings();

    tagFilters.getIncludedTagRemoveButtons().should('have.length', 1);
    tagFilters.getExcludedTagRemoveButtons().should('have.length', 1);
    cy.intercept('POST', '/v2/projects/*/export').as('export');
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should('deep.include', {
        filterTagIn: ['included'],
        filterTagNotIn: ['excluded'],
      });
  });

  it('offers only existing tags and wildcard patterns', () => {
    tagFilters.typeIncludedTag('inc');
    cy.gcy('tag-autocomplete-option').should('have.length', 1);

    tagFilters.typeIncludedTag('no-such-tag');
    tagFilters.getIncludedTagInput().should('have.value', 'no-such-tag');
    cy.waitForDom();
    cy.gcy('tag-autocomplete-option').should('not.exist');

    tagFilters.typeIncludedTag('feat*');
    cy.gcy('tag-autocomplete-option').should('have.length', 1).click();
    tagFilters.getIncludedTagRemoveButtons().should('have.length', 1);
  });

  it('removes tag filter when tag chip is deleted', () => {
    tagFilters.addIncludedTag('included');
    tagFilters.removeOnlyIncludedTag();

    cy.intercept('POST', '/v2/projects/*/export').as('export');
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should('not.have.any.keys', 'filterTagIn', 'filterTagNotIn');
  });

  function readExportedFile() {
    return cy.readFile(
      downloadsFolder + '/' + getFileName(PROJECT_NAME, 'json', 'en')
    );
  }
});

function selectOnlyNamespace(namespace: string) {
  cy.gcy('export-namespace-selector').click();
  NAMESPACES.filter((ns) => ns !== namespace).forEach((ns) => {
    gcyAdvanced({ value: 'export-namespace-selector-item', namespace: ns })
      .should('be.visible')
      .click();
  });
  dismissMenu();
}
