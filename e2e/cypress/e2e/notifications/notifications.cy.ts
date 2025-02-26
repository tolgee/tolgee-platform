import { internalFetch, login } from '../../common/apiCalls/common';
import { notificationTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { notifications } from '../../common/notifications';
import { HOST } from '../../common/constants';
import { setFeature } from '../../common/features';

function generateNotification(userId: number, type: string) {
  internalFetch(`e2e-data/notification/generate-notification`, {
    method: 'POST',
    body: { type: type, userId: userId },
  });
}

describe('notifications', () => {
  let userId: number;

  beforeEach(() => {
    setFeature('TASKS', true);
    notificationTestData.clean();
    notificationTestData
      .generateStandard()
      .then((r) => r.body)
      .then(({ users }) => {
        userId = users[0].id;
        return login(users[0].username);
      })
      .then(() => cy.visit(`${HOST}`));
    waitForGlobalLoading();
  });

  afterEach(() => {
    notificationTestData.clean();
  });

  it('shows notifications in batches', () => {
    for (let i = 0; i < 25; i++) {
      generateNotification(userId, 'PASSWORD_CHANGED');
    }

    notifications.assertUnseenNotificationsCount(25);
    notifications.getNotifications().as('notificationList');
    notifications.assertUnseenNotificationsCount(15);
    cy.get('@notificationList')
      .should('have.length', 10)
      .last()
      .scrollIntoView();
    notifications.assertUnseenNotificationsCount(5);
    cy.get('@notificationList')
      .should('have.length', 20)
      .last()
      .scrollIntoView();
    notifications.assertUnseenNotificationsCount(0);
    cy.get('@notificationList').should('have.length', 25);
  });
});
