import { HOST } from '../../common/constants';
import { gcy } from '../../common/shared';
import { E2WebhookEditDialog } from './E2WebhookEditDialog';
import { E2WebhookItem } from './E2WebhookItem';

export class E2WebhooksView {
  visit(projectId: number) {
    cy.visit(`${HOST}/projects/${projectId}/developer/webhooks`);
  }

  openAddDialog(): E2WebhookEditDialog {
    gcy('webhooks-add-item-button').click();
    return new E2WebhookEditDialog();
  }

  item(url: string): E2WebhookItem {
    return new E2WebhookItem(url);
  }

  shouldShowAddButtonDisabled() {
    gcy('webhooks-add-item-button').should('be.disabled');
  }
}
