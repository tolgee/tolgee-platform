import React, { ComponentProps } from 'react';
import { Box, styled } from '@mui/material';

import { FlagImage } from 'tg.component/languages/FlagImage';

type Props = {
  flag: string | null | undefined;
  size?: number;
} & ComponentProps<typeof Box>;

const StyledBox = styled(Box)`
  padding: 2px;
`;

const StyledIconWrapper = styled(Box)`
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  position: relative;
  border-radius: 50%;
  filter: drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2));
`;

const StyledFlag = styled(FlagImage)`
  position: absolute;
`;

const StyledFlagBlur = styled(FlagImage)`
  z-index: 2;
  position: absolute;
  filter: blur(1px);
  opacity: 0.05;
`;

export const CircledLanguageIcon: React.FC<Props> = ({
  flag,
  size,
  ...boxProps
}) => {
  size = size || 18;
  return (
    <StyledBox {...boxProps} className={boxProps.className}>
      <StyledIconWrapper width={size} height={size}>
        <StyledFlag
          height={Math.floor(size * 0.75) * 2}
          width={Math.floor(size * 0.75) * 2}
          flagEmoji={flag || ''}
          draggable="false"
        />
        <StyledFlagBlur
          height={Math.floor(size * 0.75) * 2}
          width={Math.floor(size * 0.75) * 2}
          flagEmoji={flag || ''}
          draggable="false"
        />
      </StyledIconWrapper>
    </StyledBox>
  );
};
