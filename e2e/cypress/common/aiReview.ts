import { HOST } from './constants';
import { setFeature } from './features';
import { gcy, gcyAdvanced } from './shared';

export function visitAiReviewRules(projectId: number) {
  return cy.visit(`${HOST}/projects/${projectId}/ai/review`);
}

export function assertRunDialogVariant(variant: 'review' | 'score' | 'retry') {
  return gcyAdvanced({ value: 'ai-review-run-dialog', variant }).should(
    'be.visible'
  );
}

export function startRun() {
  gcy('ai-review-run-start').click();
}

// Pair with restoreAiReviewFeature in the spec's afterEach.
export function withoutAiReviewFeature() {
  setFeature('AI_REVIEW', false);
}

export function restoreAiReviewFeature() {
  setFeature('AI_REVIEW', true);
}
