import React from 'react';
import { styled, Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { CellStateBar } from '../cell/CellStateBar';

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

export const CellLanguage: React.FC<Props> = ({
  language,
  onResize,
  colIndex,
}) => {
  const handleResize = () => onResize(colIndex);
  return (
    <>
      <StyledContent>
        <CircledLanguageIcon flag={language.flagEmoji} />
        <Box sx={{ fontWeight: language.base ? 'bold' : 'normal' }}>
          {language.name}
        </Box>
      </StyledContent>
      <CellStateBar onResize={handleResize} />
    </>
  );
};
