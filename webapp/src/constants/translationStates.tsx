import { T } from '@tolgee/react';

export type StateType = 'UNTRANSLATED' | 'TRANSLATED' | 'REVIEWED' | 'DISABLED';
export type StateInType = Exclude<StateType, 'UNTRANSLATED' | 'DISABLED'>;

type StateStruct = Record<
  StateType,
  {
    translation: React.ReactElement;
    color: string;
    next: StateInType | null;
  }
>;

export const TRANSLATION_STATES = {
  DISABLED: {
    translation: <T keyName="translation_state_disabled" />,
    color: '#7e7e7e',
    next: null,
  },
  UNTRANSLATED: {
    translation: <T keyName="translation_state_untranslated" />,
    color: 'rgba(218,224,236,0.7)',
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
} as const satisfies StateStruct;

export const EXPORTABLE_STATES = Object.keys(TRANSLATION_STATES).filter(
  (val) => val !== 'DISABLED'
);
