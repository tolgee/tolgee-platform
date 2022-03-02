export type StateType =
  | 'UNTRANSLATED'
  | 'TRANSLATED'
  | 'REVIEWED'
  | 'NEEDS_REVIEW';

type StateStruct = Record<
  StateType,
  { translationKey: string; color: string; next: StateType[] }
>;

export const translationStates: StateStruct = {
  UNTRANSLATED: {
    translationKey: 'translation_state_untranslated',
    color: '#C4C4C4',
    next: [],
  },
  TRANSLATED: {
    translationKey: 'translation_state_translated',
    color: '#FFCE00',
    next: ['REVIEWED'],
  },
  REVIEWED: {
    translationKey: 'translation_state_reviewed',
    color: '#17AD18',
    next: ['NEEDS_REVIEW'],
  },
  NEEDS_REVIEW: {
    translationKey: 'translation_state_needs_review',
    color: '#E80000',
    next: ['REVIEWED'],
  },
};
