import React, { ComponentProps } from 'react';
import { Box, styled } from '@mui/material';

import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

type Props = {
  flag: string | null | undefined;
} & ComponentProps<typeof Box>;

const StyledIconWrapper = styled(Box)`
  overflow: hidden;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  position: relative;
  top: -1px;
  border-radius: 50%;
  filter: drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2));
  width: 1em;
  height: 1em;
  vertical-align: text-bottom;
`;

const StyledFlag = styled(FlagImage)`
  position: absolute;
  height: 1.4em;
`;

export const InlineLanguageIcon: React.FC<Props> = ({ flag, ...boxProps }) => {
  return (
    <StyledIconWrapper {...boxProps}>
      <StyledFlag flagEmoji={flag || ''} draggable="false" />
    </StyledIconWrapper>
  );
};
