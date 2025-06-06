import React from 'react';
import { Box, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CellStateBar } from '../cell/CellStateBar';
import { FlagImage } from 'tg.component/languages/FlagImage';

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
  gap: 8px;
`;

type Props = {
  language: CellLanguageModel;
  onResize?: () => void;
};

export const CellLanguage: React.FC<Props> = ({ language, onResize }) => {
  return (
    <>
      <StyledContent>
        <FlagImage flagEmoji={language.flagEmoji!} height={20} />
        <Box sx={{ fontWeight: language.base ? 'bold' : 'normal' }}>
          {language.name}
        </Box>
      </StyledContent>
      {onResize && <CellStateBar onResize={onResize} />}
    </>
  );
};
