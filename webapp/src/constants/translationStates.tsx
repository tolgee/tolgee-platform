import { T } from '@tolgee/react';

export type StateType = 'UNTRANSLATED' | 'TRANSLATED' | 'REVIEWED';

type StateStruct = Record<
  StateType,
  { translation: React.ReactElement; color: string; next: StateType | null }
>;

export const translationStates: StateStruct = {
  UNTRANSLATED: {
    translation: <T keyName="translation_state_untranslated" />,
    color: '#C4C4C4',
    next: null,
  },
  TRANSLATED: {
    translation: <T keyName="translation_state_translated" />,
    color: '#FFCE00',
    next: 'REVIEWED',
  },
  REVIEWED: {
    translation: <T keyName="translation_state_reviewed" />,
    color: '#17AD18',
    next: 'TRANSLATED',
  },
};
