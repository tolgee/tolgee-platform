export const notifications = {
  assertUnseenNotificationsCount: function (expectedCount: number) {
    if (expectedCount === 0) {
      cy.gcy('notifications-count').should('not.be.visible');
    } else {
      cy.gcy('notifications-count').should('have.text', expectedCount);
    }
  },
  getNotifications: function () {
    return getNotificationsListWrappper().get(
      '[data-cy=notifications-list-item]'
    );
  },
  assertNotificationListIsEmpty: function () {
    getNotificationsListWrappper()
      .get('[data-cy=notifications-empty-message]')
      .should('have.length', 1)
      .first()
      .should('have.text', 'You have no notifications.');
    resetNotificationsDropdown();
  },
};

function resetNotificationsDropdown() {
  cy.get('body') // Targeting the body for a global event
    .trigger('keydown', {
      key: 'Escape',
      code: 'Escape',
    });
  cy.gcy('notifications-list').should('not.be.visible');
}

function getNotificationsListWrappper() {
  resetNotificationsDropdown();
  cy.gcy('notifications-button').click();
  notifications.assertUnseenNotificationsCount(0);
  cy.gcy('notifications-list').should('be.visible');
  return cy.gcy('notifications-list');
}
