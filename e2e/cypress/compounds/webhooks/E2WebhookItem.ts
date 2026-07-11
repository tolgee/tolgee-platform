import { confirmStandard, gcyAdvanced } from '../../common/shared';
import { E2WebhookEditDialog } from './E2WebhookEditDialog';

export class E2WebhookItem {
  constructor(private url: string) {}

  get root() {
    return gcyAdvanced({ value: 'webhooks-list-item', url: this.url });
  }

  shouldExist() {
    this.root.should('be.visible');
    return this;
  }

  shouldNotExist() {
    this.root.should('not.exist');
    return this;
  }

  openEdit() {
    this.root.findDcy('webhooks-item-edit').click();
    return new E2WebhookEditDialog();
  }

  test() {
    this.root.findDcy('webhooks-item-test').click();
  }

  disable() {
    this.root.findDcy('webhook-item-toggle').find('input').click();
    confirmStandard();
  }

  enable() {
    this.root.findDcy('webhook-item-toggle').find('input').click();
  }

  shouldBeEnabled() {
    this.root.findDcy('webhook-item-toggle').find('input').should('be.checked');
    this.root.should('have.css', 'opacity', '1');
    return this;
  }

  shouldBeDisabled() {
    this.root
      .findDcy('webhook-item-toggle')
      .find('input')
      .should('not.be.checked');
    this.root.should('have.css', 'opacity', '0.6');
    return this;
  }
}
