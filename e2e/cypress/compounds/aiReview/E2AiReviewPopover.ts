import { gcy } from '../../common/shared';
import { components } from '../../../../webapp/src/service/apiSchema.generated';

type AiReviewOutcome =
  components['schemas']['TranslationAiReviewModel']['outcome'];

export class E2AiReviewPopover {
  assertOutcome(outcome: AiReviewOutcome) {
    gcy('ai-review-popover-status').should(
      'have.attr',
      'data-cy-outcome',
      outcome
    );
  }

  assertScore(score: number, threshold?: number) {
    const scoreLine = gcy('ai-review-popover-score').should(
      'have.attr',
      'data-cy-score',
      String(score)
    );
    if (threshold !== undefined) {
      scoreLine.should('have.attr', 'data-cy-threshold', String(threshold));
    }
  }
}
