import { applyAcceptedSuggestion } from '../utils';

describe('applyAcceptedSuggestion', () => {
  it('keeps the preview untouched and decrements the count when siblings remain', () => {
    const result = applyAcceptedSuggestion(
      { activeSuggestionCount: 2 },
      { acceptedTranslation: 'accepted', declinedCount: 0 }
    );
    expect(result).toEqual({ text: 'accepted', activeSuggestionCount: 1 });
    expect(result).not.toHaveProperty('suggestions');
  });

  it('clears the preview and count when it was the only active suggestion', () => {
    const result = applyAcceptedSuggestion(
      { activeSuggestionCount: 1 },
      { acceptedTranslation: 'accepted', declinedCount: 0 }
    );
    expect(result).toEqual({
      text: 'accepted',
      suggestions: [],
      activeSuggestionCount: 0,
    });
  });

  it('clears the preview and count when the rest were declined', () => {
    const result = applyAcceptedSuggestion(
      { activeSuggestionCount: 2 },
      { acceptedTranslation: 'accepted', declinedCount: 1 }
    );
    expect(result).toEqual({
      text: 'accepted',
      suggestions: [],
      activeSuggestionCount: 0,
    });
  });

  it('clamps a stale-cache over-count to zero rather than going negative', () => {
    const result = applyAcceptedSuggestion(
      { activeSuggestionCount: 2 },
      { acceptedTranslation: 'accepted', declinedCount: 3 }
    );
    expect(result).toEqual({
      text: 'accepted',
      suggestions: [],
      activeSuggestionCount: 0,
    });
  });

  it('treats a missing activeSuggestionCount as zero', () => {
    const result = applyAcceptedSuggestion(
      {},
      { acceptedTranslation: 'accepted', declinedCount: 0 }
    );
    expect(result).toEqual({
      text: 'accepted',
      suggestions: [],
      activeSuggestionCount: 0,
    });
  });

  it('decrements correctly when siblings remain after declines (keep-branch arithmetic)', () => {
    const result = applyAcceptedSuggestion(
      { activeSuggestionCount: 5 },
      { acceptedTranslation: 'accepted', declinedCount: 2 }
    );
    expect(result).toEqual({ text: 'accepted', activeSuggestionCount: 2 });
    expect(result).not.toHaveProperty('suggestions');
  });
});
