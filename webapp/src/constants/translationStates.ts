export type StateType =
  | 'UNTRANSLATED'
  | 'MACHINE_TRANSLATED'
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
  MACHINE_TRANSLATED: {
    translationKey: 'translation_state_machine_translated',
    color: '#39E1FA',
    next: ['REVIEWED', 'NEEDS_REVIEW'],
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
