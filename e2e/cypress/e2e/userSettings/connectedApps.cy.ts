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

  it('lists the authorized apps', () => {
    gcy('account-security-connected-apps').should('be.visible');
    gcy('connected-app-list-item').should('have.length', 2);
    gcyAdvanced({
      value: 'connected-app-list-item',
      'client-id': 'tolgee-cli',
    }).should('exist');
  });

  it('disconnects an app', () => {
    gcyAdvanced({
      value: 'connected-app-list-item',
      'client-id': 'tolgee-cli',
    })
      .findDcy('connected-app-revoke-button')
      .click();
    confirmStandard();

    gcy('connected-app-list-item').should('have.length', 1);
    gcyAdvanced({
      value: 'connected-app-list-item',
      'client-id': 'tolgee-cli',
    }).should('not.exist');
  });

  it('hides the section once nothing is connected', () => {
    gcy('connected-app-list-item').each(($row) => {
      cy.wrap($row).findDcy('connected-app-revoke-button').click();
      confirmStandard();
    });

    gcy('account-security-connected-apps').should('not.exist');
  });
});
