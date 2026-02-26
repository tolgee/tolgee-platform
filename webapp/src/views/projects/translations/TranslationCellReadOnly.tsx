import React from 'react';
import { styled } from '@mui/material';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { TranslationVisual } from './translationVisual/TranslationVisual';

const StyledTranslationCell = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  position: relative;
  outline: 0;
  overflow: hidden;
`;

const StyledStateBar = styled('div')`
  height: 100%;
  width: 4px;
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.7 : 1)}
  );
`;

const StyledTranslationContent = styled('div')`
  display: grid;
  grid-auto-rows: max-content;
  min-height: 23px;
  margin: 8px 12px 8px 12px;
  position: relative;
  align-content: start;
`;

type Props = {
  text?: string | null;
  state?: string;
  locale: string;
  isPlural: boolean;
};

export const TranslationCellReadOnly: React.FC<Props> = ({
  text,
  state,
  locale,
  isPlural,
}) => {
  const effectiveState = state || 'UNTRANSLATED';
  const stateColor =
    TRANSLATION_STATES[effectiveState]?.color ||
    TRANSLATION_STATES['UNTRANSLATED'].color;

  return (
    <StyledTranslationCell>
      <StyledStateBar style={{ borderLeft: `4px solid ${stateColor}` }} />
      <StyledTranslationContent>
        {text ? (
          <TranslationVisual
            text={text}
            locale={locale}
            isPlural={isPlural}
          />
        ) : null}
      </StyledTranslationContent>
    </StyledTranslationCell>
  );
};
