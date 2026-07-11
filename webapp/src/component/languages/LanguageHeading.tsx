import React from 'react';
import { styled } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

type Props = {
  language: { name?: string; flagEmoji?: string; base?: boolean };
  flagHeight?: number;
};

const StyledRoot = styled('span')<{ $base?: boolean }>`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: ${({ $base }) => ($base ? 700 : 400)};
`;

export const LanguageHeading: React.VFC<Props> = ({
  language,
  flagHeight = 16,
}) => (
  <StyledRoot $base={language.base}>
    <FlagImage flagEmoji={language.flagEmoji ?? ''} height={flagHeight} />
    <span>{language.name}</span>
  </StyledRoot>
);
