import React from 'react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CellStateBar } from '../cell/CellStateBar';
import { LanguageHeading } from 'tg.component/languages/LanguageHeading';

type LanguageModel = components['schemas']['LanguageModel'];
export type CellLanguageModel = Pick<
  LanguageModel,
  'base' | 'flagEmoji' | 'name'
>;

const StyledContent = styled('div')`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  flex-shrink: 0;
`;

type Props = {
  language: CellLanguageModel;
  onResize?: () => void;
};

export const CellLanguage: React.FC<React.PropsWithChildren<Props>> = ({
  language,
  onResize,
}) => {
  return (
    <>
      <StyledContent>
        <LanguageHeading language={language} />
      </StyledContent>
      {onResize && <CellStateBar onResize={onResize} />}
    </>
  );
};
