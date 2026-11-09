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
}
