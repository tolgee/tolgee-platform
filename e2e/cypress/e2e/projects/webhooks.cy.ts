import { assertMessage, confirmStandard } from '../../common/shared';
import { contentDeliveryTestData } from '../../common/apiCalls/testData/testData';
import {
  deleteAllEmails,
  getLatestEmail,
  internalFetch,
  login,
  setWebhookControllerStatus,
  triggerWebhookAutoDisable,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { API_URL } from '../../common/constants';
import { setFeature } from '../../common/features';
import { E2WebhooksView } from '../../compounds/webhooks/E2WebhooksView';

describe('Content delivery', () => {
  const testUrl = API_URL + '/internal/webhook-testing';
  const view = new E2WebhooksView();

  beforeEach(() => {
    setWebhookControllerStatus(200);
    setFeature('WEBHOOKS', true);
    contentDeliveryTestData.clean();
    contentDeliveryTestData.generateStandard().then((response) => {
      login();
      view.visit(response.body.projects[0].id);
    });
  });

  afterEach(() => {
    setFeature('WEBHOOKS', true);
    setWebhookControllerStatus(400);
  });

  it('creates webhook', () => {
    createWebhook();
  });

  it('updates webhook', () => {
    const newUrl = 'testurl';
    createWebhook();
    const editDialog = view.item(testUrl).openEdit();
    editDialog.setUrl(newUrl);
    editDialog.save();
    waitForGlobalLoading();
    assertMessage('Webhook successfully updated!');
    view.item(newUrl).shouldExist();
  });

  it('deletes webhook', () => {
    createWebhook();
    const editDialog = view.item(testUrl).openEdit();
    editDialog.delete();
    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Webhook successfully deleted!');
    view.item(testUrl).shouldNotExist();
  });

  it('tests valid webhook', () => {
    createWebhook();
    view.item(testUrl).test();
    waitForGlobalLoading();
    assertMessage('Test request sent to the webhook successfully');
  });

  it('tests invalid webhook', () => {
    createWebhook('invalid');
    view.item('invalid').test();
    waitForGlobalLoading();
    assertMessage('Test failed!');
  });

  it('tests failing webhook', () => {
    setWebhookControllerStatus(400);
    createWebhook();
    view.item(testUrl).test();
    waitForGlobalLoading();
    assertMessage('Test failed!');
  });

  it('shows info if feature not enabled', () => {
    createWebhook();
    setFeature('WEBHOOKS', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );
    const editDialog = view.item(testUrl).openEdit();
    editDialog.delete();
    confirmStandard();
    waitForGlobalLoading();

    cy.contains("Your plan doesn't include the webhooks feature").should(
      'be.visible'
    );
    view.shouldShowAddButtonDisabled();
  });

  it('toggles webhook disabled and enabled', () => {
    createWebhook();
    const item = view.item(testUrl);

    item.disable();
    waitForGlobalLoading();
    item.shouldBeDisabled();

    item.enable();
    waitForGlobalLoading();
    item.shouldBeEnabled();
  });

  it('auto-disables a webhook that has been failing for more than 3 days', () => {
    deleteAllEmails();
    createWebhook();

    // Mark the webhook as having failed 4 days ago
    const fourDaysAgo = new Date(Date.now() - 4 * 24 * 60 * 60 * 1000);
    internalFetch('sql/execute', {
      method: 'POST',
      body: `UPDATE webhook_config SET first_failed = '${fourDaysAgo.toISOString()}' WHERE url = '${testUrl}'`,
    });

    // Trigger the auto-disable scheduler
    triggerWebhookAutoDisable();

    // Reload and verify webhook is disabled
    cy.reload();
    waitForGlobalLoading();
    view.item(testUrl).shouldBeDisabled();

    // Verify an email was sent to the org owner
    getLatestEmail().then((email) => {
      cy.wrap(email.Subject).should('contain', 'Webhook');
    });
  });

  function createWebhook(url: string = testUrl) {
    const dialog = view.openAddDialog();
    dialog.setUrl(url);
    dialog.save();

    waitForGlobalLoading();
    view.item(url).shouldExist();
    assertMessage('Webhook successfully created!');
  }
});
