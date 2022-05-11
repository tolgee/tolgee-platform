import React from 'react';

import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { LanguageReferenceData } from '../types';

type Props = {
  data: LanguageReferenceData;
};

export const LanguageReference: React.FC<Props> = ({ data }) => {
  return (
    <span className="reference referenceComposed">
      <span className="referenceText">{data.language.name} </span>
      <CircledLanguageIcon size={14} flag={data.language.flagEmoji} />
    </span>
  );
};
