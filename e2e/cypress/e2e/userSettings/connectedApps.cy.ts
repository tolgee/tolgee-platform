import { HOST } from '../../common/constants';
import { connectedAppsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { confirmStandard, gcy, gcyAdvanced } from '../../common/shared';

describe('Connected apps', () => {
  beforeEach(() => {
    connectedAppsTestData.clean();
    connectedAppsTestData.generateStandard();
    login('connected-apps@tolgee.io', 'admin');
    cy.visit(`${HOST}/account/security`);
  });

  afterEach(() => {
    connectedAppsTestData.clean();
  });

  const cliRow = () =>
    gcyAdvanced({
      value: 'connected-app-list-item',
      'client-id': 'tolgee-cli',
    });

  it('lists the authorized apps', () => {
    gcy('account-security-connected-apps').should('be.visible');
    gcy('connected-app-list-item').should('have.length', 2);
    cliRow().should('exist');
    // the CLI grant is bound to the fixture's own project, not left resolving to a literal "0"
    cliRow().findDcy('connected-app-projects').should('contain', 'test_project');
  });

  it('disconnects an app', () => {
    cliRow().findDcy('connected-app-revoke-button').click();
    confirmStandard();

    gcy('connected-app-list-item').should('have.length', 1);
    cliRow().should('not.exist');
  });

  it('hides the section once nothing is connected', () => {
    gcy('connected-app-list-item').each(($row) => {
      cy.wrap($row).findDcy('connected-app-revoke-button').click();
      confirmStandard();
    });

    gcy('account-security-connected-apps').should('not.exist');
  });
});
