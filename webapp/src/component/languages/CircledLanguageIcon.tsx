import React, { ComponentProps } from 'react';
import { Box, styled } from '@mui/material';

import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const DEFAULT_SIZE = 18;

type CircledIconProps = {
  size?: number;
  wrapperProps?: ComponentProps<typeof Box>;
} & ComponentProps<typeof Box>;

type CircledLanguageIconProps = {
  flag: string | null | undefined;
} & CircledIconProps;

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

export const CircledLanguageIcon: React.FC<CircledLanguageIconProps> = ({
  flag,
  size = DEFAULT_SIZE,
  ...boxProps
}) => {
  return (
    <CircledIcon size={size} {...boxProps}>
      <StyledFlag
        height={Math.floor(size * 0.75) * 2}
        width={Math.floor(size * 0.75) * 2}
        flagEmoji={flag || ''}
        draggable="false"
      />
    </CircledIcon>
  );
};

export const CircledIcon: React.FC<CircledIconProps> = ({
  size = DEFAULT_SIZE,
  children,
  wrapperProps,
  ...boxProps
}) => {
  return (
    <StyledBox {...boxProps} className={boxProps.className}>
      <StyledIconWrapper {...wrapperProps} width={size} height={size}>
        {children}
      </StyledIconWrapper>
    </StyledBox>
  );
};

type CircledPillProps = {
  width?: number;
  height?: number;
  wrapperProps?: ComponentProps<typeof Box>;
} & ComponentProps<typeof Box>;

export const CircledPill: React.FC<CircledPillProps> = ({
  width = DEFAULT_SIZE,
  height = DEFAULT_SIZE,
  children,
  wrapperProps,
  ...boxProps
}) => {
  const borderRadius = Math.min(width, height);
  return (
    <StyledBox {...boxProps} className={boxProps.className}>
      <StyledIconWrapper
        {...wrapperProps}
        width={width}
        height={height}
        style={{ borderRadius }}
      >
        {children}
      </StyledIconWrapper>
    </StyledBox>
  );
};
