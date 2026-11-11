import { setFeature } from './features';

// Pair with restoreAiReviewFeature in the spec's afterEach.
export function withoutAiReviewFeature() {
  setFeature('AI_REVIEW', false);
}

export function restoreAiReviewFeature() {
  setFeature('AI_REVIEW', true);
}
