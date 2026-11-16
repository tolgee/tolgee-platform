import { login } from '../../common/apiCalls/common';
import { notificationsPocTestData } from '../../common/apiCalls/testData/testData';
import { gcy, gcyAdvanced } from '../../common/shared';
import { HOST } from '../../common/constants';

let mobileAppId: number;

const mobileAppRow = () =>
  gcyAdvanced({
    value: 'notifications-settings-languages-project',
    'project-id': String(mobileAppId),
  });

const chip = (tag: string) =>
  mobileAppRow().findDcyAdvanced({
    value: 'notifications-settings-languages-chip',
    'language-tag': tag,
  });

const state = () =>
  mobileAppRow().findDcy('notifications-settings-languages-state');

describe('Notification languages per project', () => {
  beforeEach(() => {
    notificationsPocTestData.clean();
    notificationsPocTestData.generateStandard().then((r) => {
      mobileAppId = r.body.projects.find((p) => p.name === 'Mobile App')!.id;
    });
    login('andreas@example.com');
    cy.visit(`${HOST}/account/notifications`);
  });

  afterEach(() => {
    notificationsPocTestData.clean();
  });

  it('follows languages picked from recent work and lets the user change them', () => {
    gcy('notifications-settings-languages-project').should('have.length', 2);
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

    mobileAppRow().findDcy('notifications-settings-languages-reset').click();
    state().should('have.attr', 'data-cy-state', 'DEFAULT');
    chip('de').should('have.attr', 'data-cy-followed', 'true');
    chip('fr').should('have.attr', 'data-cy-followed', 'false');
  });
});
