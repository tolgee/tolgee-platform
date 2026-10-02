import { login } from '../../common/apiCalls/common';
import { notificationDigestTestData } from '../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced } from '../../common/shared';
import { HOST } from '../../common/constants';

describe('Localization notifications', () => {
  beforeEach(() => {
    notificationDigestTestData.clean();
    notificationDigestTestData.generateStandard();
    login('notification_digest_user');
  });

  afterEach(() => {
    notificationDigestTestData.clean();
  });

  it('shows grouped localization notifications', () => {
    cy.visit(HOST);
    gcy('notifications-button').click();
    gcy('notifications-localization-item').should('have.length', 2);
    gcyAdvanced({
      value: 'notifications-localization-flag',
      tag: 'fr',
    }).should('be.visible');
  });

  it('saves a localization setting and the digest frequency', () => {
    cy.visit(`${HOST}/account/notifications`);
    const toggle = () =>
      gcyAdvanced({
        value: 'notifications-settings-localization-toggle',
        type: 'KEYS_ADDED',
        channel: 'EMAIL',
      }).find('input');
    toggle().should('be.checked');
    toggle().click();
    toggle().should('not.be.checked');
    gcy('notifications-settings-digest-frequency').click();
    cy.get('[data-value="OFF"]').click();
    gcy('notifications-settings-digest-frequency')
      .find('input')
      .should('have.value', 'OFF');
  });
});
