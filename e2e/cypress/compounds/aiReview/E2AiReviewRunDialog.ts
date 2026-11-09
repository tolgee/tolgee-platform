import { gcy, gcyAdvanced } from '../../common/shared';

export class E2AiReviewRunDialog {
  assertVariant(variant: 'review' | 'score' | 'retry') {
    gcyAdvanced({ value: 'ai-review-run-dialog', variant }).should(
      'be.visible'
    );
    return this;
  }

  start() {
    gcy('ai-review-run-start').click();
  }

  cancel() {
    gcy('ai-review-run-cancel').click();
  }
}
