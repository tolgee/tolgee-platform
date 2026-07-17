import { components } from 'tg.service/apiSchema.generated';

type TranslationViewModel = components['schemas']['TranslationViewModel'];

type AcceptedSuggestionUpdate = Pick<TranslationViewModel, 'text'> &
  Partial<Pick<TranslationViewModel, 'suggestions' | 'activeSuggestionCount'>>;

/**
 * `translation.suggestions` is only a capped display preview (up to
 * MAX_DISPLAYED_SUGGESTIONS); the real total lives in `activeSuggestionCount`.
 * When siblings remain, decrement the count
 * alone — do NOT re-sync the preview array to it (the surviving sibling can't be
 * derived here; the refetch reconciles it). Clear the preview only when none remain.
 */
export function applyAcceptedSuggestion(
  translation: { activeSuggestionCount?: number },
  {
    acceptedTranslation,
    declinedCount,
  }: {
    acceptedTranslation: string | undefined;
    declinedCount: number;
  }
): AcceptedSuggestionUpdate {
  const remainingActive = Math.max(
    0,
    (translation.activeSuggestionCount ?? 0) - 1 - declinedCount
  );
  if (remainingActive === 0) {
    return {
      text: acceptedTranslation,
      suggestions: [],
      activeSuggestionCount: 0,
    };
  }
  return { text: acceptedTranslation, activeSuggestionCount: remainingActive };
}
