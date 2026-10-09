import { login, v2apiFetch } from '../../../common/apiCalls/common';
import { exportSettingsTestData } from '../../../common/apiCalls/testData/testData';
import { exportSelectFormat, visitExport } from '../../../common/export';
import { dismissMenu, gcyAdvanced } from '../../../common/shared';
import { HOST } from '../../../common/constants';
import { E2ExportAdvancedSettings } from '../../../compounds/E2ExportAdvancedSettings';
import { E2ExportTagFilters } from '../../../compounds/E2ExportTagFilters';

const ALL_STATES = ['UNTRANSLATED', 'TRANSLATED', 'REVIEWED'];

describe('Export settings', () => {
  const advanced = new E2ExportAdvancedSettings();
  const tagFilters = new E2ExportTagFilters();
  let projectId: number;

  beforeEach(() => {
    exportSettingsTestData.clean();
    exportSettingsTestData.generateStandard().then((response) => {
      projectId = response.body.projects[0].id;
      login('export_settings_user');
      visitExport(projectId);
    });
  });

  afterEach(() => {
    exportSettingsTestData.clean();
  });

  it('shows languages and format with the rest collapsed', () => {
    cy.gcy('export-language-selector').should('be.visible');
    cy.gcy('export-format-selector').should('be.visible');
    advanced.getContent().should('not.exist');
    advanced.getSummaryItems('default').should('have.length', 3);
    advanced.getSummaryItems('changed').should('not.exist');
    cy.gcy('export-reset-to-defaults-button').should('be.disabled');

    advanced.expand();
    advanced.getSummary().should('not.exist');
    cy.gcy('export-state-selector').should('be.visible');
    cy.gcy('export-tags-in-selector').should('be.visible');

    advanced.collapse();
    advanced.getSummary().should('be.visible');
  });

  it('exports all states by default', () => {
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body.filterState')
      .should('have.members', ALL_STATES);
  });

  it('remembers the last export setup', () => {
    exportSelectFormat('XLIFF');
    toggleLanguage('de');
    interceptExpandedSave();
    advanced.expand();
    cy.wait('@saveExpanded');
    deselectStates(['UNTRANSLATED']);
    tagFilters.addIncludedTag('release');
    interceptSettingsSave();
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export');
    cy.wait('@saveSettings');

    cy.location('search').should('equal', '');
    cy.reload();
    advanced.collapse();
    advanced.getSummaryItems('changed').should('have.length', 2);
    cy.gcy('export-reset-to-defaults-button').should('be.enabled');
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should((body) => {
        expect(body.format).to.equal('XLIFF');
        expect(body.languages).to.have.members(['en', 'cs']);
        expect(body.filterState).to.have.members(['TRANSLATED', 'REVIEWED']);
        expect(body.filterTagIn).to.deep.equal(['release']);
      });
  });

  it('remembers whether advanced settings are open', () => {
    interceptExpandedSave();
    advanced.expand();
    cy.wait('@saveExpanded');
    visitExport(projectId);
    advanced.getContent().should('be.visible');

    interceptExpandedSave();
    advanced.collapse();
    cy.wait('@saveExpanded');
    visitExport(projectId);
    advanced.getContent().should('not.exist');
  });

  it('lets settings from a link override the matching remembered ones', () => {
    storeSettings({ format: 'generic_xliff', tagsIn: ['release'] });
    cy.visit(`${HOST}/projects/${projectId}/export?languages=cs&tagsNotIn=wip`);
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should((body) => {
        expect(body.format).to.equal('XLIFF');
        expect(body.languages).to.deep.equal(['cs']);
        expect(body.filterTagIn).to.deep.equal(['release']);
        expect(body.filterTagNotIn).to.deep.equal(['wip']);
      });
  });

  it('does not remember settings of a failed export', () => {
    advanced.expand();
    tagFilters.addIncludedTag('nothing-*');
    interceptSettingsSave();
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export').its('response.statusCode').should('equal', 400);

    tagFilters.removeOnlyIncludedTag();
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export').its('response.statusCode').should('equal', 200);
    cy.wait('@saveSettings');
    cy.get('@saveSettings.all')
      .should('have.length', 1)
      .then((saves: any) => {
        expect(saves[0].request.body.tagsIn).to.deep.equal([]);
      });
  });

  it('shows remembered format options in the collapsed summary', () => {
    storeSettings({ format: 'generic_xliff', escapeHtml: true });
    visitExport(projectId);
    advanced.getSummaryItems('changed').should('have.length', 1);
  });

  it('shows a non-default message format in the collapsed summary', () => {
    storeSettings({ format: 'generic_xliff', messageFormat: 'PHP_SPRINTF' });
    visitExport(projectId);
    advanced.getSummaryItems('changed').should('have.length', 1);
  });

  it('ignores a message format the selected format does not use', () => {
    storeSettings({ format: 'native_json', messageFormat: 'PHP_SPRINTF' });
    visitExport(projectId);
    advanced.getSummaryItems('default').should('have.length', 3);
    advanced.getSummaryItems('changed').should('not.exist');
    cy.gcy('export-reset-to-defaults-button').should('be.disabled');
  });

  it('resets to defaults and forgets the remembered setup', () => {
    storeSettings({
      format: 'generic_xliff',
      states: ['REVIEWED'],
      tagsIn: ['release'],
    });
    visitExport(projectId);
    advanced.getSummaryItems('changed').should('have.length', 2);

    interceptSettingsSave();
    cy.gcy('export-reset-to-defaults-button').click();
    cy.wait('@saveSettings').then((interception) => {
      expect(interception.request.body).to.equal(null);
    });
    advanced.getSummaryItems('changed').should('not.exist');
    cy.gcy('export-reset-to-defaults-button').should('be.disabled');

    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body')
      .should((body) => {
        expect(body.format).to.equal('JSON');
        expect(body.filterState).to.have.members(ALL_STATES);
        expect(body).not.to.have.any.keys('filterTagIn');
      });
  });

  it('falls back to all languages when remembered ones were removed', () => {
    storeSettings({ languages: ['fr'] });
    visitExport(projectId);
    interceptExport();
    cy.gcy('export-submit-button').click();
    cy.wait('@export')
      .its('request.body.languages')
      .should('have.members', ['en', 'cs', 'de']);
  });

  it('keeps advanced settings open while no state is selected', () => {
    interceptExpandedSave();
    advanced.expand();
    deselectStates(ALL_STATES);
    advanced.getToggle().click();
    advanced.getContent().should('be.visible');
    cy.gcy('export-submit-button').should('be.disabled');
    cy.get('@saveExpanded.all')
      .should('have.length', 2)
      .then((saves: any) => {
        expect(saves[1].request.body).to.equal(false);
      });
  });

  function storeSettings(settings: Record<string, unknown>) {
    v2apiFetch(`user-preferences/project-storage/${projectId}/exportSettings`, {
      method: 'PUT',
      body: settings,
    });
  }
});

function toggleLanguage(tag: string) {
  cy.gcy('export-language-selector').click();
  gcyAdvanced({
    value: 'export-language-selector-item',
    language: tag,
  }).click();
  dismissMenu();
}

function deselectStates(states: string[]) {
  cy.gcy('export-state-selector').click();
  states.forEach((state) => {
    gcyAdvanced({ value: 'export-state-selector-item', state }).click();
  });
  dismissMenu();
}

function interceptExport() {
  cy.intercept('POST', '/v2/projects/*/export').as('export');
}

function interceptSettingsSave() {
  cy.intercept(
    'PUT',
    '/v2/user-preferences/project-storage/*/exportSettings'
  ).as('saveSettings');
}

function interceptExpandedSave() {
  cy.intercept(
    'PUT',
    '/v2/user-preferences/project-storage/*/exportAdvancedExpanded'
  ).as('saveExpanded');
}
