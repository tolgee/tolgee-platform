import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';

describe('QA settings', () => {
  let projectId: number;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    qaTestData.clean();
  });

  function visitQaSettings() {
    cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
    waitForGlobalLoading();
    gcy('project-settings-menu-qa').click();
    waitForGlobalLoading();
  }

  it('navigates to QA settings page', () => {
    visitQaSettings();

    gcy('qa-enabled-toggle').should('be.visible');
    gcy('qa-settings-row').should('have.length.gte', 1);
  });

  it('toggles QA checks on/off for project', () => {
    visitQaSettings();

    // Toggle off
    gcy('qa-enabled-toggle').click();
    waitForGlobalLoading();

    // Settings rows should be disabled (grayed out) when QA is off
    gcy('qa-settings-row')
      .first()
      .find('[role="combobox"]')
      .should('have.attr', 'aria-disabled', 'true');

    // Toggle back on
    gcy('qa-enabled-toggle').click();
    waitForGlobalLoading();

    // Settings rows should be enabled again
    gcy('qa-settings-row').should('have.length.gte', 1);
  });

  it('changes check type severity', () => {
    visitQaSettings();

    // Find first settings row and change its severity
    gcy('qa-settings-row').first().findDcy('qa-settings-select').click();

    // Select "Off" option
    cy.get('[role="listbox"]').contains('Off').click();
    waitForGlobalLoading();

    // Reload and verify the setting persisted
    visitQaSettings();
    gcy('qa-settings-row')
      .first()
      .findDcy('qa-settings-select')
      .should('contain.text', 'Off');
  });

  it('opens per-language settings dialog', () => {
    visitQaSettings();

    // Click the language settings button for French
    gcy('qa-language-settings-button').first().click();

    // Dialog should open with inherited banner
    gcy('qa-language-dialog-inherited-banner').should('exist');

    // Close the dialog (no changes were made, so Save is disabled)
    cy.get('body').type('{esc}');
  });

  it('resets language settings to global', () => {
    visitQaSettings();

    // Open language settings and make a change
    gcy('qa-language-settings-button').first().click();

    // Change a setting to create an override (scope within dialog)
    cy.get('[role="dialog"]').within(() => {
      gcy('qa-settings-row').first().findDcy('qa-settings-select').click();
    });
    cy.get('[role="listbox"]').contains('Off').click();

    gcy('qa-language-dialog-save').click();
    waitForGlobalLoading();

    // Reopen and reset to global
    gcy('qa-language-settings-button').first().click();
    gcy('qa-language-dialog-reset-to-global').click();
    waitForGlobalLoading();

    // Reopen again and verify inherited banner is back
    gcy('qa-language-settings-button').first().click();
    gcy('qa-language-dialog-inherited-banner').should('exist');
  });
});
