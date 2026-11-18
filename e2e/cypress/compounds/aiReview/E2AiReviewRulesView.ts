import { HOST } from '../../common/constants';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2AiReviewRuleDialog } from './E2AiReviewRuleDialog';

export class E2AiReviewRulesView {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/ai/review`);
  }

  openCreateDialog() {
    gcy('ai-review-add-rule-button').click();
    gcy('ai-review-rule-dialog').should('be.visible');
    return new E2AiReviewRuleDialog();
  }

  openEditDialog(name: string) {
    gcyAdvanced({ value: 'ai-review-rule-edit', name }).click();
    gcy('ai-review-rule-dialog').should('be.visible');
    return new E2AiReviewRuleDialog();
  }

  getRule(name: string) {
    return gcyAdvanced({ value: 'ai-review-rule-item', name });
  }

  getRuleMode(mode: string) {
    return gcyAdvanced({ value: 'ai-review-rule-mode', mode });
  }

  openRuleMenu(name: string) {
    gcyAdvanced({ value: 'ai-review-rule-menu', name }).click();
    return this;
  }

  deleteRule(name: string) {
    this.openRuleMenu(name);
    gcy('ai-review-rule-menu-delete').click();
    return this;
  }

  duplicateRule(name: string) {
    this.openRuleMenu(name);
    gcy('ai-review-rule-menu-duplicate').click();
    return this;
  }

  toggleRule(name: string) {
    this.openRuleMenu(name);
    gcyAdvanced({ value: 'ai-review-rule-switch', name }).click();
    return this;
  }

  getEnabledToggleInput() {
    return gcy('ai-review-enabled-toggle').find('input');
  }

  clickReviewNow(name: string) {
    this.getRule(name).findDcy('ai-review-rule-review-now').click();
    return this;
  }

  clickRetryFailed(name: string) {
    this.getRule(name).findDcy('ai-review-rule-retry-failed').click();
    return this;
  }
}
