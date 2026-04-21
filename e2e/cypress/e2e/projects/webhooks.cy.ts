import { assertMessage, confirmStandard, gcy } from '../../common/shared';
import { webhooksTestData } from '../../common/apiCalls/testData/testData';
import {
  deleteAllEmails,
  getLatestEmail,
  internalFetch,
  login,
  setWebhookControllerStatus,
  triggerWebhookAutoDisableCheck,
  v2apiFetch,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { API_URL } from '../../common/constants';
import { setFeature } from '../../common/features';
import { E2WebhooksView } from '../../compounds/webhooks/E2WebhooksView';

describe('Webhooks', () => {
  const testUrl = API_URL + '/internal/webhook-testing';
  const testUserEmail = 'webhooks-test@test.com';
  const view = new E2WebhooksView();
  let projectId: number;

  beforeEach(() => {
    setWebhookControllerStatus(200);
    setFeature('WEBHOOKS', true);
    webhooksTestData.clean();
    webhooksTestData.generateStandard().then((response) => {
      projectId = response.body.projects[0].id;
      login(testUserEmail);
      view.visit(projectId);
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

    // Get the webhook ID via API
    v2apiFetch(`projects/${projectId}/webhook-configs`).then((response) => {
      const webhook = response.body._embedded.webhookConfigs.find(
        (w) => w.url === testUrl
      );
      const webhookId = webhook.id;

      // Mark the webhook as having failed 4 days ago
      const fourDaysAgo = new Date(Date.now() - 4 * 24 * 60 * 60 * 1000);
      internalFetch('sql/execute', {
        method: 'POST',
        body: `UPDATE webhook_config SET first_failed = '${fourDaysAgo.toISOString()}' WHERE id = ${webhookId}`,
      });

      // Trigger the auto-disable check for this webhook
      triggerWebhookAutoDisableCheck(webhookId);

      // Reload and verify webhook is disabled and marked as auto-disabled
      cy.reload();
      waitForGlobalLoading();
      view.item(testUrl).shouldBeDisabled();
      gcy('webhook-auto-disabled-label').should('be.visible');

      // Verify notification email was sent to the org owner
      getLatestEmail().then((email) => {
        cy.wrap(email.To[0].Address).should('eq', testUserEmail);
      });
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
