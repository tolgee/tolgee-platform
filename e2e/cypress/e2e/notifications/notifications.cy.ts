import {
  deleteAllEmails,
  getAllEmails,
  getLastEmail,
  internalFetch,
  login,
} from '../../common/apiCalls/common';
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

function assertNewestEmail(
  expectedSubject: string,
  expectedTextFragment: string
) {
  getLastEmail().then(({ subject, html }) => {
    assert(subject === expectedSubject, 'mail subject');
    assert(html.includes(expectedTextFragment), 'mail text');
  });
}

function targetPageShouldHaveInUrl(expectedUrlFragment: string) {
  cy.visit(`${HOST}`);
  notifications.getNotifications().first().click();
  cy.location().its('href').should('contain', expectedUrlFragment);
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
    deleteAllEmails();
  });

  afterEach(() => {
    notificationTestData.clean();
    deleteAllEmails();
  });

  it('shows paged notifications', () => {
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
    getAllEmails().then((emails) => assert(emails.length === 25, 'mail count'));
  });

  it('notifications are clickable and correct mails are sent', () => {
    notifications.assertUnseenNotificationsCount(0);
    notifications.assertNotificationListIsEmpty();

    generateNotification(userId, 'TASK_ASSIGNED');
    assertNewestEmail(
      'Task has been assigned to you',
      "You've been assigned to a task"
    );
    targetPageShouldHaveInUrl('/translations?task=');

    generateNotification(userId, 'TASK_COMPLETED');
    assertNewestEmail(
      'Task has been completed',
      "you've created has been completed"
    );
    targetPageShouldHaveInUrl('/translations?task=');

    generateNotification(userId, 'TASK_CLOSED');
    assertNewestEmail('Task has been closed', "you've created has been closed");
    targetPageShouldHaveInUrl('/translations?task=');

    generateNotification(userId, 'MFA_ENABLED');
    assertNewestEmail(
      'Multi-factor authentication has been enabled for your account',
      'Multi-factor authentication has been enabled for your account'
    );
    targetPageShouldHaveInUrl('/account/security');

    generateNotification(userId, 'MFA_DISABLED');
    assertNewestEmail(
      'Multi-factor authentication has been disabled for your account',
      'Multi-factor authentication has been disabled for your account'
    );
    targetPageShouldHaveInUrl('/account/security');

    generateNotification(userId, 'PASSWORD_CHANGED');
    assertNewestEmail(
      'Password has been changed for your account',
      'Password has been changed for your account'
    );
    targetPageShouldHaveInUrl('/account/security');
  });

  it('disabled task notifications do not generate notifications', () => {
    cy.gcy('notifications-button').click();
    cy.gcy('notifications-settings-icon').click();
    cy.get('[data-cy="notifications-settings-TASKS-IN_APP"]').click();
    cy.get('[data-cy="notifications-settings-TASKS-EMAIL"]').click();

    generateNotification(userId, 'TASK_ASSIGNED');
    generateNotification(userId, 'PASSWORD_CHANGED');

    notifications.assertUnseenNotificationsCount(1);
    assertNewestEmail(
      'Password has been changed for your account',
      'Password has been changed for your account'
    );
  });
});
