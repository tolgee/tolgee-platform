import React, { FC } from 'react';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';

export const DirectionLocaleWrapper: FC<{ languageTag: string }> = (props) => (
  <span
    dir={
      props.languageTag ? getLanguageDirection(props.languageTag) : undefined
    }
  >
    {props.children}
  </span>
);
