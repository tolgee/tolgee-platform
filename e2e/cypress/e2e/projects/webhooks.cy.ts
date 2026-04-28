import { assertMessage, confirmStandard, gcy } from '../../common/shared';
import { webhooksTestData } from '../../common/apiCalls/testData/testData';
import {
  deleteAllEmails,
  forceDate,
  getLatestEmail,
  login,
  releaseForcedDate,
  setWebhookControllerStatus,
  triggerWebhookAutoDisableCheck,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { API_URL } from '../../common/constants';
import { setFeature } from '../../common/features';
import { E2WebhooksView } from '../../compounds/webhooks/E2WebhooksView';

describe('Webhooks', () => {
  const testUrl = API_URL + '/internal/webhook-testing';
  const preCreatedUrl = 'https://this-will-hopefully-never-exist.com/wh';
  const testUserEmail = 'webhooks-test@test.com';
  const view = new E2WebhooksView();
  let projectId: number;

  beforeEach(() => {
    releaseForcedDate();
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
    releaseForcedDate();
    setFeature('WEBHOOKS', true);
    setWebhookControllerStatus(400);
  });

  it('creates webhook', () => {
    createWebhook();
  });

  it('updates webhook', () => {
    const editDialog = view.item(preCreatedUrl).openEdit();
    editDialog.setUrl('testurl');
    editDialog.save();
    waitForGlobalLoading();
    assertMessage('Webhook successfully updated!');
    view.item('testurl').shouldExist();
  });

  it('deletes webhook', () => {
    const editDialog = view.item(preCreatedUrl).openEdit();
    editDialog.delete();
    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Webhook successfully deleted!');
    view.item(preCreatedUrl).shouldNotExist();
  });

  it('tests valid webhook', () => {
    createWebhook();
    view.item(testUrl).test();
    waitForGlobalLoading();
    assertMessage('Test request sent to the webhook successfully');
  });

  it('tests invalid webhook', () => {
    view.item(preCreatedUrl).test();
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
    setFeature('WEBHOOKS', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );
    const editDialog = view.item(preCreatedUrl).openEdit();
    editDialog.delete();
    confirmStandard();
    waitForGlobalLoading();

    cy.contains("Your plan doesn't include the webhooks feature").should(
      'be.visible'
    );
    view.shouldShowAddButtonDisabled();
  });

  it('toggles webhook disabled and enabled', () => {
    const item = view.item(preCreatedUrl);

    item.disable();
    waitForGlobalLoading();
    item.shouldBeDisabled();

    item.enable();
    waitForGlobalLoading();
    item.shouldBeEnabled();
  });

  it('auto-disables a webhook after persistent failures and sends email', () => {
    deleteAllEmails();

    // Move time forward 4 days so the checker sees persistent failure
    forceDate(Date.now() + 4 * 24 * 60 * 60 * 1000);

    // Trigger the auto-disable check on all webhooks
    triggerWebhookAutoDisableCheck();

    // Reload and verify webhook is disabled and marked as auto-disabled
    cy.reload();
    waitForGlobalLoading();
    view.item(preCreatedUrl).shouldBeDisabled();
    gcy('webhook-auto-disabled-label').should('be.visible');

    // Verify notification email was sent to the org owner
    getLatestEmail().then((email) => {
      cy.wrap(email.To[0].Address).should('eq', testUserEmail);
    });
  });

  it('sends warning email after 6 hours of failure', () => {
    deleteAllEmails();

    // Move time forward 7 hours so the checker sees prolonged failure
    forceDate(Date.now() + 7 * 60 * 60 * 1000);

    // Trigger the auto-disable check
    triggerWebhookAutoDisableCheck();

    // Webhook should still be enabled (not yet 3 days)
    cy.reload();
    waitForGlobalLoading();
    view.item(preCreatedUrl).shouldBeEnabled();

    // But a warning email should have been sent
    getLatestEmail().then((email) => {
      cy.wrap(email.To[0].Address).should('eq', testUserEmail);
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
