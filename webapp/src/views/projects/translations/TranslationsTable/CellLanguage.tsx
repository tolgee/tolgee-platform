import React from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { CellStateBar } from '../cell/CellStateBar';
import { styled } from '@mui/material';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContent = styled('div')`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  flex-shrink: 0;
  & > * + * {
    margin-left: 5px;
  }
`;

type Props = {
  language: LanguageModel;
  colIndex: number;
  onResize: (colIndex: number) => void;
};

export const CellLanguage: React.FC<React.PropsWithChildren<Props>> = ({
  language,
  onResize,
  colIndex,
}) => {
  const handleResize = () => onResize(colIndex);
  return (
    <>
      <StyledContent>
        <CircledLanguageIcon flag={language.flagEmoji} />
        <div>{language.name}</div>
      </StyledContent>
      <CellStateBar onResize={handleResize} />
    </>
  );
};
