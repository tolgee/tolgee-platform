export type StateType = 'UNTRANSLATED' | 'TRANSLATED' | 'REVIEWED';

type StateStruct = Record<
  StateType,
  { translationKey: string; color: string; next: StateType | null }
>;

export const translationStates: StateStruct = {
  UNTRANSLATED: {
    translationKey: 'translation_state_untranslated',
    color: '#C4C4C4',
    next: null,
  },
  TRANSLATED: {
    translationKey: 'translation_state_translated',
    color: '#FFCE00',
    next: 'REVIEWED',
  },
  REVIEWED: {
    translationKey: 'translation_state_reviewed',
    color: '#17AD18',
    next: 'TRANSLATED',
  },
};
