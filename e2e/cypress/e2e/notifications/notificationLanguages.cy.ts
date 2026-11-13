import { login } from '../../common/apiCalls/common';
import { notificationsPocTestData } from '../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced } from '../../common/shared';
import { HOST } from '../../common/constants';

const chip = (tag: string) =>
  gcyAdvanced({
    value: 'notifications-settings-languages-chip',
    'language-tag': tag,
  });

const state = () => gcy('notifications-settings-languages-state');

describe('Notification languages per project', () => {
  beforeEach(() => {
    notificationsPocTestData.clean();
    notificationsPocTestData.generateStandard();
    login('andreas@example.com');
    cy.visit(`${HOST}/account/notifications`);
  });

  afterEach(() => {
    notificationsPocTestData.clean();
  });

  it('follows languages picked from recent work and lets the user change them', () => {
    gcy('notifications-settings-languages-project').should('have.length', 1);
    state().should('have.attr', 'data-cy-state', 'DEFAULT');
    chip('de').should('have.attr', 'data-cy-followed', 'true');
    chip('fr').should('have.attr', 'data-cy-followed', 'false');

    chip('fr').click();
    state().should('have.attr', 'data-cy-state', 'CHOSEN');
    chip('fr').should('have.attr', 'data-cy-followed', 'true');

    chip('de').click();
    chip('de').should('have.attr', 'data-cy-followed', 'false');
    chip('fr').click();
    state().should('have.attr', 'data-cy-state', 'MUTED');

    gcy('notifications-settings-languages-reset').click();
    state().should('have.attr', 'data-cy-state', 'DEFAULT');
    chip('de').should('have.attr', 'data-cy-followed', 'true');
    chip('fr').should('have.attr', 'data-cy-followed', 'false');
  });
});
